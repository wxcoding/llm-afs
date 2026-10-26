package com.afs.module.knowledge.vo;

import lombok.Data;

/**
 * 批量文档上传结果
 *
 * @author guanwx
 */
@Data
public class BatchUploadResultVO {

    /** 上传成功数量 */
    private int successCount;
    /** 上传失败数量 */
    private int failCount;
    /** 失败原因汇总 */
    private String failMessages;

    /**
     * 构造批量上传结果
     *
     * @param successCount 成功数量
     * @param failCount 失败数量
     * @param failMessages 失败原因汇总
     */
    public BatchUploadResultVO(int successCount, int failCount, String failMessages) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.failMessages = failMessages;
    }
}
