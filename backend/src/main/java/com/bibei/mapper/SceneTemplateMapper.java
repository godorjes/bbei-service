package com.bibei.mapper;

import com.bibei.entity.SceneTemplateEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface SceneTemplateMapper {
    @Select("SELECT id, name, icon, pinned, archived, content_json AS contentJson, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM scene_template WHERE archived = #{archived} " +
            "ORDER BY pinned DESC, updated_at DESC")
    List<SceneTemplateEntity> findAll(@Param("archived") boolean archived);

    @Select("SELECT id, name, icon, pinned, archived, content_json AS contentJson, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM scene_template WHERE id = #{id}")
    SceneTemplateEntity findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM scene_template")
    int count();

    @Insert("INSERT INTO scene_template(name, icon, pinned, archived, content_json, created_at, updated_at) " +
            "VALUES(#{name}, #{icon}, #{pinned}, #{archived}, #{contentJson}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SceneTemplateEntity entity);

    @Update("UPDATE scene_template SET name = #{name}, icon = #{icon}, pinned = #{pinned}, " +
            "archived = #{archived}, content_json = #{contentJson}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(SceneTemplateEntity entity);

    @Update("UPDATE scene_template SET archived = #{archived}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int setArchived(@Param("id") long id, @Param("archived") boolean archived);

    @Delete("DELETE FROM scene_template WHERE id = #{id}")
    int delete(@Param("id") long id);
}
