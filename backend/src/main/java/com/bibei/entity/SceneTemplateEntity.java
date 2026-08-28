package com.bibei.entity;

import java.time.LocalDateTime;

public class SceneTemplateEntity {
    public Long id;
    public String name;
    public String icon;
    public Boolean pinned;
    public Boolean archived;
    public String contentJson;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
