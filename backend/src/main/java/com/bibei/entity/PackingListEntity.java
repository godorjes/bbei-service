package com.bibei.entity;

import java.time.LocalDateTime;

public class PackingListEntity {
    public Long id;
    public String title;
    public Long sourceTemplateId;
    public String sourceTemplateName;
    public String sourceContentJson;
    public String listStatus;
    public String contentJson;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime completedAt;
}
