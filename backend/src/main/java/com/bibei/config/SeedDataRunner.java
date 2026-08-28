package com.bibei.config;

import com.bibei.dto.ApiModels.SaveTemplateRequest;
import com.bibei.mapper.SceneTemplateMapper;
import com.bibei.model.ChecklistItem;
import com.bibei.model.ChecklistSection;
import com.bibei.service.TemplateService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(1)
public class SeedDataRunner implements ApplicationRunner {
    private final SceneTemplateMapper mapper;
    private final TemplateService templateService;
    private final JdbcTemplate jdbcTemplate;

    public SeedDataRunner(SceneTemplateMapper mapper, TemplateService templateService, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.templateService = templateService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer migrated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_migration WHERE version = 'option-b-catalog-v1'",
                Integer.class
        );
        if (mapper.count() > 0 || (migrated != null && migrated > 0)) {
            return;
        }
        templateService.create(template("周末旅行", "backpack", true,
                section("证件", item("身份证")),
                section("数码", item("手机充电器"), item("充电宝"), item("耳机")),
                section("衣物", item("换洗衣物", 2), item("睡衣")),
                section("洗漱", item("牙刷"), item("洗面奶"))));

        templateService.create(template("商务出差", "briefcase", true,
                section("证件", item("身份证")),
                section("数码", item("笔记本电脑"), item("电脑充电器", 1, "放入电脑包"), item("手机充电器")),
                section("衣物", item("衬衫", 2), item("换洗衣物", 2)),
                section("工作", item("名片"), item("会议资料"))));

        templateService.create(template("一周旅行", "suitcase", true,
                section("证件", item("身份证"), item("银行卡")),
                section("数码", item("手机充电器"), item("充电宝"), item("耳机")),
                section("衣物", item("上衣", 5), item("裤子", 3), item("换洗衣物", 7)),
                section("洗漱", item("牙刷"), item("洗面奶"), item("毛巾")),
                section("健康", item("常用药"), item("创可贴"))));
    }

    private SaveTemplateRequest template(
            String name,
            String icon,
            boolean pinned,
            ChecklistSection... sections
    ) {
        SaveTemplateRequest request = new SaveTemplateRequest();
        request.name = name;
        request.icon = icon;
        request.pinned = pinned;
        request.sections = Arrays.asList(sections);
        return request;
    }

    private ChecklistSection section(String title, ChecklistItem... items) {
        ChecklistSection section = new ChecklistSection(title);
        section.items = Arrays.asList(items);
        return section;
    }

    private ChecklistItem item(String name) {
        return item(name, 1, null);
    }

    private ChecklistItem item(String name, int quantity) {
        return item(name, quantity, null);
    }

    private ChecklistItem item(String name, int quantity, String note) {
        ChecklistItem item = new ChecklistItem(name);
        item.quantity = quantity;
        item.note = note;
        return item;
    }
}
