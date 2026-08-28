package com.bibei.mapper;

import com.bibei.entity.PackingSectionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface SectionMapper {
    @Select("SELECT id, name, created_at AS createdAt, updated_at AS updatedAt FROM packing_section ORDER BY updated_at DESC, id DESC")
    List<PackingSectionEntity> findAll();

    @Select("SELECT id, name, created_at AS createdAt, updated_at AS updatedAt FROM packing_section WHERE id = #{id}")
    PackingSectionEntity findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM packing_section WHERE LOWER(TRIM(name)) = LOWER(TRIM(#{name})) " +
            "AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO packing_section(name, created_at, updated_at) VALUES(#{name}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PackingSectionEntity entity);

    @Update("UPDATE packing_section SET name = #{name}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(PackingSectionEntity entity);

    @Delete("DELETE FROM packing_section WHERE id = #{id}")
    int delete(@Param("id") long id);

    @Select("SELECT scene_id FROM scene_section WHERE section_id = #{sectionId} ORDER BY scene_id")
    List<Long> findSceneIds(@Param("sectionId") long sectionId);

    @Select("SELECT s.name FROM scene_section ss JOIN scene s ON s.id = ss.scene_id " +
            "WHERE ss.section_id = #{sectionId} ORDER BY s.name")
    List<String> findSceneNames(@Param("sectionId") long sectionId);

    @Select("SELECT COUNT(*) FROM section_item WHERE section_id = #{sectionId}")
    int countItems(@Param("sectionId") long sectionId);

    @Delete("DELETE FROM scene_section WHERE scene_id = #{sceneId} AND section_id = #{sectionId}")
    int deleteSceneBinding(@Param("sceneId") long sceneId, @Param("sectionId") long sectionId);

    @Select("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM scene_section WHERE scene_id = #{sceneId}")
    int nextSortOrder(@Param("sceneId") long sceneId);

    @Insert("INSERT INTO scene_section(scene_id, section_id, sort_order) VALUES(#{sceneId}, #{sectionId}, #{sortOrder})")
    int insertSceneBinding(
            @Param("sceneId") long sceneId,
            @Param("sectionId") long sectionId,
            @Param("sortOrder") int sortOrder
    );
}
