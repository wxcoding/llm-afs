package com.afs.module.rag.ingestion;

import com.afs.enums.KnowledgeStatus;
import com.afs.enums.VectorStatus;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.scamcase.entity.ScamCase;
import com.afs.module.scamcase.mapper.ScamCaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RAG 入库任务处理器。
 *
 * 由 RagIngestionDispatcher 提交到独立线程池异步执行。
 * 处理时回读数据库获取最新内容（避免"编辑后旧内容覆盖索引"），
 * 并驱动 knowledge.vector_status 状态流转：PROCESSING -> SUCCESS / FAILED。
 * 处理异常只记录日志与状态、不再抛出（Embedding 限流等异常重试无意义）。
 * @author guanwx
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RagIngestionProcessor {

    /** RAG 入库服务，执行文档切分与索引写入 */
    private final RagIngestionService ragIngestionService;
    /** 知识库记录 Mapper */
    private final KnowledgeMapper knowledgeMapper;
    /** 诈骗案例记录 Mapper */
    private final ScamCaseMapper scamCaseMapper;

    /**
     * 处理单条知识库记录的入库：回读最新内容，发布且正文非空时重建索引，否则清除索引。
     *
     * @param id 知识库记录主键
     */
    public void processKnowledge(Long id) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            log.warn("[RAG入库] 知识记录不存在，跳过: id={}", id);
            return;
        }
        updateVectorStatus(id, VectorStatus.PROCESSING);
        try {
            if (KnowledgeStatus.ACTIVE.getCode().equals(knowledge.getStatus())
                    && knowledge.getContent() != null && !knowledge.getContent().isEmpty()) {
                ragIngestionService.addKnowledgeDocument(
                        knowledge.getId(), knowledge.getTitle(), knowledge.getCategory(), knowledge.getContent());
            } else {
                // 非发布状态或正文为空：确保索引被清除（先删后建中的"删"）
                ragIngestionService.deleteKnowledgeDocument(id);
            }
            updateVectorStatus(id, VectorStatus.SUCCESS);
        } catch (Exception e) {
            updateVectorStatus(id, VectorStatus.FAILED);
            log.error("[RAG入库] 知识库文档向量化失败: id={}", id, e);
        }
    }

    /**
     * 处理单条诈骗案例的入库：回读最新内容，正文非空时重建索引，否则清除索引。
     *
     * @param id 案例记录主键
     */
    public void processScamCase(Long id) {
        ScamCase scamCase = scamCaseMapper.selectById(id);
        if (scamCase == null) {
            log.warn("[RAG入库] 案例记录不存在，跳过: id={}", id);
            return;
        }
        try {
            if (scamCase.getContent() != null && !scamCase.getContent().isEmpty()) {
                ragIngestionService.addScamCaseDocument(
                        scamCase.getId(), scamCase.getTitle(), scamCase.getType(),
                        scamCase.getContent(), scamCase.getTips());
            } else {
                ragIngestionService.deleteScamCaseDocument(id);
            }
        } catch (Exception e) {
            log.error("[RAG入库] 诈骗案例向量化失败: id={}", id, e);
        }
    }

    /**
     * 更新知识记录的向量化状态。
     *
     * @param id     知识库记录主键
     * @param status 目标向量化状态
     */
    private void updateVectorStatus(Long id, VectorStatus status) {
        Knowledge update = new Knowledge();
        update.setId(id);
        update.setVectorStatus(status.getCode());
        // MyBatis-Plus updateById 只更新非空字段，这里仅刷新 vector_status
        knowledgeMapper.updateById(update);
    }
}
