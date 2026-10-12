package com.afs.module.knowledge.vo;

import lombok.Data;

/**
 * 批量文档上传结果
 */
@Data
public class BatchUploadResultVO {

    private int successCount;
    private int failCount;
    private String failMessages;

    public BatchUploadResultVO(int successCount, int failCount, String failMessages) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.failMessages = failMessages;
    }
}
