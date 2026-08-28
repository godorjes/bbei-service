package com.bibei.mapper;

import com.bibei.entity.SceneEntity;
import com.bibei.entity.SceneItemRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface SceneMapper {
    @Select("SELECT id, name, created_at AS createdAt, updated_at AS updatedAt FROM scene ORDER BY updated_at DESC, id DESC")
    List<SceneEntity> findAll();

    @Select("SELECT id, name, created_at AS createdAt, updated_at AS updatedAt FROM scene WHERE id = #{id}")
    SceneEntity findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM scene")
    int count();

    @Select("SELECT COUNT(*) FROM scene WHERE LOWER(TRIM(name)) = LOWER(TRIM(#{name})) " +
            "AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO scene(name, created_at, updated_at) VALUES(#{name}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SceneEntity entity);

    @Update("UPDATE scene SET name = #{name}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(SceneEntity entity);

    @Delete("DELETE FROM scene WHERE id = #{id}")
    int delete(@Param("id") long id);

    @Select("SELECT section_id FROM scene_section WHERE scene_id = #{sceneId} ORDER BY sort_order, section_id")
    List<Long> findSectionIds(@Param("sceneId") long sceneId);

    @Delete("DELETE FROM scene_section WHERE scene_id = #{sceneId}")
    int deleteSectionBindings(@Param("sceneId") long sceneId);

    @Insert("INSERT INTO scene_section(scene_id, section_id, sort_order) VALUES(#{sceneId}, #{sectionId}, #{sortOrder})")
    int insertSectionBinding(
            @Param("sceneId") long sceneId,
            @Param("sectionId") long sectionId,
            @Param("sortOrder") int sortOrder
    );

    @Select("SELECT ps.id AS sectionId, ps.name AS sectionName, ss.sort_order AS sortOrder, " +
            "pi.id AS itemId, pi.name AS itemName, " +
            "CASE WHEN sis.item_id IS NULL THEN FALSE ELSE sis.checked END AS checked " +
            "FROM scene_section ss " +
            "JOIN packing_section ps ON ps.id = ss.section_id " +
            "LEFT JOIN section_item si ON si.section_id = ps.id " +
            "LEFT JOIN packing_item pi ON pi.id = si.item_id " +
            "LEFT JOIN scene_item_state sis ON sis.scene_id = ss.scene_id AND sis.item_id = pi.id " +
            "WHERE ss.scene_id = #{sceneId} " +
            "ORDER BY ss.sort_order, ps.id, LOWER(pi.name), pi.id")
    List<SceneItemRow> findChecklistRows(@Param("sceneId") long sceneId);

    @Select("SELECT COUNT(DISTINCT si.item_id) FROM scene_section ss " +
            "JOIN section_item si ON si.section_id = ss.section_id " +
            "WHERE ss.scene_id = #{sceneId} AND si.item_id = #{itemId}")
    int countVisibleItem(@Param("sceneId") long sceneId, @Param("itemId") long itemId);

    @Delete("DELETE FROM scene_item_state WHERE scene_id = #{sceneId} AND item_id = #{itemId}")
    int deleteCheckedState(@Param("sceneId") long sceneId, @Param("itemId") long itemId);

    @Insert("INSERT INTO scene_item_state(scene_id, item_id, checked, updated_at) " +
            "VALUES(#{sceneId}, #{itemId}, TRUE, CURRENT_TIMESTAMP)")
    int insertCheckedState(@Param("sceneId") long sceneId, @Param("itemId") long itemId);

    @Delete("DELETE FROM scene_item_state WHERE scene_id = #{sceneId}")
    int resetCheckedState(@Param("sceneId") long sceneId);

    @Delete("DELETE FROM scene_item_state WHERE NOT EXISTS (" +
            "SELECT 1 FROM scene_section ss JOIN section_item si ON si.section_id = ss.section_id " +
            "WHERE ss.scene_id = scene_item_state.scene_id AND si.item_id = scene_item_state.item_id)")
    int deleteInvisibleStates();
}
