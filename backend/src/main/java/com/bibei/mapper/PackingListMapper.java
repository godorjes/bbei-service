package com.bibei.mapper;

import com.bibei.entity.PackingListEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PackingListMapper {
    String COLUMNS = "id, title, source_template_id AS sourceTemplateId, " +
            "source_template_name AS sourceTemplateName, source_content_json AS sourceContentJson, " +
            "list_status AS listStatus, content_json AS contentJson, created_at AS createdAt, " +
            "updated_at AS updatedAt, completed_at AS completedAt";

    @Select("SELECT " + COLUMNS + " FROM packing_list WHERE list_status = #{status} ORDER BY updated_at DESC")
    List<PackingListEntity> findByStatus(@Param("status") String status);

    @Select("SELECT " + COLUMNS + " FROM packing_list WHERE id = #{id}")
    PackingListEntity findById(@Param("id") long id);

    @Select("SELECT " + COLUMNS + " FROM packing_list ORDER BY updated_at DESC")
    List<PackingListEntity> findAll();

    @Insert("INSERT INTO packing_list(title, source_template_id, source_template_name, source_content_json, list_status, " +
            "content_json, created_at, updated_at, completed_at) VALUES(#{title}, #{sourceTemplateId}, " +
            "#{sourceTemplateName}, #{sourceContentJson}, #{listStatus}, #{contentJson}, #{createdAt}, #{updatedAt}, #{completedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PackingListEntity entity);

    @Update("UPDATE packing_list SET title = #{title}, source_template_id = #{sourceTemplateId}, " +
            "source_template_name = #{sourceTemplateName}, source_content_json = #{sourceContentJson}, " +
            "list_status = #{listStatus}, content_json = #{contentJson}, " +
            "updated_at = #{updatedAt}, completed_at = #{completedAt} " +
            "WHERE id = #{id}")
    int update(PackingListEntity entity);
    @Delete("DELETE FROM packing_list WHERE id = #{id}")
    int delete(@Param("id") long id);
}
