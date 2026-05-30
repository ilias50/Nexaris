CREATE TABLE IF NOT EXISTS agenda_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    source VARCHAR(50) NOT NULL,
    is_manual_locked BOOLEAN NOT NULL DEFAULT TRUE,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT ck_agenda_entries_time CHECK (start_at < end_at)
);

CREATE INDEX idx_agenda_entries_user_time ON agenda_entries (user_id, start_at, end_at);

CREATE TABLE IF NOT EXISTS user_planning_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    work_day_start TIME NOT NULL,
    work_day_end TIME NOT NULL,
    preferred_meeting_block_minutes INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_user_planning_preferences_user UNIQUE (user_id),
    CONSTRAINT ck_user_planning_preferences_time CHECK (work_day_start < work_day_end),
    CONSTRAINT ck_user_planning_preferences_block CHECK (preferred_meeting_block_minutes BETWEEN 15 AND 240)
);

CREATE TABLE IF NOT EXISTS planning_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    color VARCHAR(20) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_blocking BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id INT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_planning_tags_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS agenda_entry_tags (
    agenda_entry_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (agenda_entry_id, tag_id),
    CONSTRAINT fk_agenda_entry_tags_entry FOREIGN KEY (agenda_entry_id) REFERENCES agenda_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_agenda_entry_tags_tag FOREIGN KEY (tag_id) REFERENCES planning_tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_agenda_entry_tags_tag ON agenda_entry_tags (tag_id);

CREATE TABLE IF NOT EXISTS planning_user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    planning_role VARCHAR(60) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_planning_user_roles UNIQUE (user_id, planning_role)
);

CREATE INDEX idx_planning_user_roles_user ON planning_user_roles (user_id);

CREATE TABLE IF NOT EXISTS planning_role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    planning_role VARCHAR(60) NOT NULL,
    permission VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_planning_role_permissions UNIQUE (planning_role, permission)
);

CREATE INDEX idx_planning_role_permissions_role ON planning_role_permissions (planning_role);

CREATE TABLE IF NOT EXISTS planning_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(60) NOT NULL,
    description VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_planning_roles_name UNIQUE (role_name)
);

CREATE INDEX idx_planning_roles_active ON planning_roles (is_active, role_name);

INSERT INTO planning_role_permissions (planning_role, permission)
SELECT 'PLANNING_ADMIN', 'CREATE_ANY_ENTRY'
WHERE NOT EXISTS (
    SELECT 1 FROM planning_role_permissions
    WHERE planning_role = 'PLANNING_ADMIN' AND permission = 'CREATE_ANY_ENTRY'
);

INSERT INTO planning_role_permissions (planning_role, permission)
SELECT 'PLANNING_ADMIN', 'CREATE_MEETING'
WHERE NOT EXISTS (
    SELECT 1 FROM planning_role_permissions
    WHERE planning_role = 'PLANNING_ADMIN' AND permission = 'CREATE_MEETING'
);


INSERT INTO planning_role_permissions (planning_role, permission)
SELECT 'PLANNING_MEETING_ORGANIZER', 'CREATE_MEETING'
WHERE NOT EXISTS (
    SELECT 1 FROM planning_role_permissions
    WHERE planning_role = 'PLANNING_MEETING_ORGANIZER' AND permission = 'CREATE_MEETING'
);

INSERT INTO planning_role_permissions (planning_role, permission)
SELECT 'PLANNING_MEETING_ORGANIZER', 'SUGGEST_MEETING'
WHERE NOT EXISTS (
    SELECT 1 FROM planning_role_permissions
    WHERE planning_role = 'PLANNING_MEETING_ORGANIZER' AND permission = 'SUGGEST_MEETING'
);




INSERT INTO planning_roles (role_name, description, is_active, created_at)
SELECT 'PLANNING_ADMIN', 'Administration complete du module planning', TRUE, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM planning_roles WHERE role_name = 'PLANNING_ADMIN'
);

INSERT INTO planning_roles (role_name, description, is_active, created_at)
SELECT 'PLANNING_MEETING_ORGANIZER', 'Planification et creation de reunions', TRUE, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM planning_roles WHERE role_name = 'PLANNING_MEETING_ORGANIZER'
);

INSERT INTO planning_roles (role_name, description, is_active, created_at)
SELECT DISTINCT pur.planning_role, NULL, TRUE, NOW()
FROM planning_user_roles pur
WHERE pur.planning_role IS NOT NULL
  AND pur.planning_role <> ''
  AND NOT EXISTS (
    SELECT 1 FROM planning_roles pr WHERE pr.role_name = pur.planning_role
  );

INSERT INTO planning_roles (role_name, description, is_active, created_at)
SELECT DISTINCT prp.planning_role, NULL, TRUE, NOW()
FROM planning_role_permissions prp
WHERE prp.planning_role IS NOT NULL
  AND prp.planning_role <> ''
  AND NOT EXISTS (
    SELECT 1 FROM planning_roles pr WHERE pr.role_name = prp.planning_role
  );


INSERT INTO planning_role_permissions (planning_role, permission)
SELECT 'PLANNING_ADMIN', 'VIEW_ANY_CALENDAR'
WHERE NOT EXISTS (
    SELECT 1 FROM planning_role_permissions
    WHERE planning_role = 'PLANNING_ADMIN' AND permission = 'VIEW_ANY_CALENDAR'
);


