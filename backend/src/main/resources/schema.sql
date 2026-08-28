CREATE TABLE IF NOT EXISTS scene_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(32) NOT NULL DEFAULT 'suitcase',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    content_json CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS packing_list (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    source_template_id BIGINT,
    source_template_name VARCHAR(100),
    list_status VARCHAR(20) NOT NULL,
    content_json CLOB NOT NULL,
    source_content_json CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_packing_list_template
        FOREIGN KEY (source_template_id) REFERENCES scene_template(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_template_archived_updated
    ON scene_template(archived, updated_at);

ALTER TABLE packing_list ADD COLUMN IF NOT EXISTS source_content_json CLOB;

CREATE INDEX IF NOT EXISTS idx_packing_list_status_updated
    ON packing_list(list_status, updated_at);

CREATE TABLE IF NOT EXISTS scene (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS packing_section (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS packing_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS scene_section (
    scene_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (scene_id, section_id),
    CONSTRAINT fk_scene_section_scene
        FOREIGN KEY (scene_id) REFERENCES scene(id) ON DELETE CASCADE,
    CONSTRAINT fk_scene_section_section
        FOREIGN KEY (section_id) REFERENCES packing_section(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS section_item (
    section_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    PRIMARY KEY (section_id, item_id),
    CONSTRAINT fk_section_item_section
        FOREIGN KEY (section_id) REFERENCES packing_section(id) ON DELETE CASCADE,
    CONSTRAINT fk_section_item_item
        FOREIGN KEY (item_id) REFERENCES packing_item(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS scene_item_state (
    scene_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    checked BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (scene_id, item_id),
    CONSTRAINT fk_scene_item_state_scene
        FOREIGN KEY (scene_id) REFERENCES scene(id) ON DELETE CASCADE,
    CONSTRAINT fk_scene_item_state_item
        FOREIGN KEY (item_id) REFERENCES packing_item(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS app_migration (
    version VARCHAR(80) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scene_updated ON scene(updated_at);
CREATE INDEX IF NOT EXISTS idx_scene_section_order ON scene_section(scene_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_section_item_item ON section_item(item_id);
CREATE INDEX IF NOT EXISTS idx_scene_item_state_checked ON scene_item_state(scene_id, checked);
