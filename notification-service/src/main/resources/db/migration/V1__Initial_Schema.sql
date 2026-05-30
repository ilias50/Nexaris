-- ============================================
-- V1: Schema initial complet
-- ============================================

-- Table notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table email_settings
CREATE TABLE IF NOT EXISTS email_settings (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    host         VARCHAR(255) NOT NULL,
    port         INT          NOT NULL DEFAULT 587,
    username     VARCHAR(255) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    from_address VARCHAR(255) NOT NULL DEFAULT 'no-reply@noreply.local',
    smtp_auth    BOOLEAN      NOT NULL DEFAULT TRUE,
    starttls     BOOLEAN      NOT NULL DEFAULT TRUE,
    ssl_trust    VARCHAR(255),
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Table in_app_notifications
CREATE TABLE IF NOT EXISTS in_app_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- Table notification_channel_preferences (avec support canaux dynamiques)
CREATE TABLE IF NOT EXISTS notification_channel_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    email_enabled BOOLEAN DEFAULT TRUE,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    channel_preferences TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    event_preferences TEXT,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    external_enabled BOOLEAN DEFAULT TRUE,
    INDEX idx_user_id (user_id)
);