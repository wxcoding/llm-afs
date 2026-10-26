package com.afs.enums;

/**
 * 知识库文档向量化处理状态（knowledge.vector_status）
 *
 * @author guanwx
 */
public enum VectorStatus {

    /** 待处理：记录已写入，入库任务已派发但未被消费 */
    PENDING,
    /** 处理中：消费者正在切分与向量化 */
    PROCESSING,
    /** 已完成：向量索引与全文索引均已就绪 */
    SUCCESS,
    /** 失败：消费过程异常（Embedding 限流、DB 异常等），可人工触发重建 */
    FAILED;

    /**
     * 获取状态编码（即枚举名）
     *
     * @return 状态编码
     */
    public String getCode() {
        return name();
    }
}
