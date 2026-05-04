package com.afs.enums;

/**
 * 知识状态枚举
 */
public enum KnowledgeStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 待审核
     */
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),

    /**
     * 已发布
     */
    ACTIVE("ACTIVE", "已发布"),

    /**
     * 已拒绝
     */
    REJECTED("REJECTED", "已拒绝");

    /**
     * 状态编码（存数据库）
     */
    private final String code;

    /**
     * 状态描述（显示用）
     */
    private final String description;

    KnowledgeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据状态编码获取枚举
     * @param code 状态编码
     * @return 枚举，如果不存在返回默认 ACTIVE
     */
    public static KnowledgeStatus fromCode(String code) {
        for (KnowledgeStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return ACTIVE; // 默认返回已发布
    }

    /**
     * 判断是否是有效的状态编码
     * @param code 状态编码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        for (KnowledgeStatus status : values()) {
            if (status.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
