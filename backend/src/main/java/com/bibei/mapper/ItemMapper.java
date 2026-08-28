package com.bibei.mapper;

import com.bibei.entity.PackingItemEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ItemMapper {
    @Select("<script>SELECT id, name, created_at AS createdAt, updated_at AS updatedAt FROM packing_item " +
            "<where><if test='query != null and query != &quot;&quot;'>LOWER(name) LIKE LOWER(CONCAT('%', #{query}, '%'))</if></where> " +
            "ORDER BY updated_at DESC, id DESC</script>")
    List<PackingItemEntity> findAll(@Param("query") String query);

    @Select("SELECT id, name, created_at AS createdAt, updated_at AS updatedAt FROM packing_item WHERE id = #{id}")
    PackingItemEntity findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM packing_item WHERE LOWER(TRIM(name)) = LOWER(TRIM(#{name})) " +
            "AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO packing_item(name, created_at, updated_at) VALUES(#{name}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PackingItemEntity entity);

    @Update("UPDATE packing_item SET name = #{name}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(PackingItemEntity entity);

    @Delete("DELETE FROM packing_item WHERE id = #{id}")
    int delete(@Param("id") long id);

    @Select("SELECT section_id FROM section_item WHERE item_id = #{itemId} ORDER BY section_id")
    List<Long> findSectionIds(@Param("itemId") long itemId);

    @Select("SELECT ps.name FROM section_item si JOIN packing_section ps ON ps.id = si.section_id " +
            "WHERE si.item_id = #{itemId} ORDER BY ps.name")
    List<String> findSectionNames(@Param("itemId") long itemId);

    @Delete("DELETE FROM section_item WHERE item_id = #{itemId}")
    int deleteSectionBindings(@Param("itemId") long itemId);

    @Insert("INSERT INTO section_item(section_id, item_id) VALUES(#{sectionId}, #{itemId})")
    int insertSectionBinding(@Param("sectionId") long sectionId, @Param("itemId") long itemId);
}
