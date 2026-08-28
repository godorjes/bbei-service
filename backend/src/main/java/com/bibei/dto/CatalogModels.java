package com.bibei.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public final class CatalogModels {
    private CatalogModels() {
    }

    public static class SaveSceneRequest {
        @NotBlank(message = "请输入场景名称")
        @Size(max = 50, message = "场景名称不能超过50个字")
        public String name;
        public List<Long> sectionIds = new ArrayList<>();
    }

    public static class SaveSectionRequest {
        @NotBlank(message = "请输入分区名称")
        @Size(max = 30, message = "分区名称不能超过30个字")
        public String name;
        public List<Long> sceneIds = new ArrayList<>();
    }

    public static class SaveItemRequest {
        @NotBlank(message = "请输入物品名称")
        @Size(max = 50, message = "物品名称不能超过50个字")
        public String name;
        public List<Long> sectionIds = new ArrayList<>();
    }

    public static class CheckedRequest {
        public boolean checked;
    }

    public static class ItemView {
        public long id;
        public String name;
        public boolean checked;
        public List<Long> sectionIds = new ArrayList<>();
        public List<String> sectionNames = new ArrayList<>();
    }

    public static class SceneSectionView {
        public long id;
        public String name;
        public List<ItemView> items = new ArrayList<>();
    }

    public static class SceneSummary {
        public long id;
        public String name;
        public int totalCount;
        public int checkedCount;
        public List<Long> sectionIds = new ArrayList<>();
    }

    public static class SceneDetail extends SceneSummary {
        public List<SceneSectionView> sections = new ArrayList<>();
    }

    public static class SectionView {
        public long id;
        public String name;
        public int itemCount;
        public List<Long> sceneIds = new ArrayList<>();
        public List<String> sceneNames = new ArrayList<>();
    }
}
