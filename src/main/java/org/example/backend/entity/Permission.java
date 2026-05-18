package org.example.backend.entity;

public enum Permission {

    // =========================
    // Project
    // =========================
    PROJECT_CREATE,
    PROJECT_VIEW,
    PROJECT_UPDATE,
    PROJECT_DELETE,
    PROJECT_ARCHIVE,

    // =========================
    // Task
    // =========================
    TASK_CREATE,
    TASK_VIEW,
    TASK_UPDATE,
    TASK_DELETE,
    TASK_ASSIGN,
    TASK_CHANGE_STATUS,

    // =========================
    // Board
    // =========================
    BOARD_VIEW,
    BOARD_UPDATE,

    // =========================
    // Member
    // =========================
    MEMBER_INVITE,
    MEMBER_REMOVE,
    MEMBER_UPDATE_ROLE,

    // =========================
    // Comment
    // =========================
    COMMENT_CREATE,
    COMMENT_UPDATE,
    COMMENT_DELETE,

    // =========================
    // Attachment
    // =========================
    ATTACHMENT_UPLOAD,
    ATTACHMENT_DELETE,

    // =========================
    // Role & Permission
    // =========================
    ROLE_MANAGE,
    PERMISSION_MANAGE
}
