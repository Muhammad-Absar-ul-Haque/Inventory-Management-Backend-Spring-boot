-- V2__add_notifications_table.sql
-- Adds the notifications table for Phase 5 Notification System

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
