-- ========================================
-- 为所有业务表与字段补充注释（COMMENT 语句幂等，可重复执行）
-- V18__add_table_and_column_comments.sql
-- ========================================

-- ----------------------------------------
-- 用户表
-- ----------------------------------------
COMMENT ON TABLE "user" IS '系统用户表';
COMMENT ON COLUMN "user"."id" IS '用户ID，主键自增';
COMMENT ON COLUMN "user"."username" IS '登录用户名，全局唯一';
COMMENT ON COLUMN "user"."password" IS '登录密码（BCrypt 加密存储）';
COMMENT ON COLUMN "user"."nickname" IS '用户昵称';
COMMENT ON COLUMN "user"."avatar" IS '用户头像地址';
COMMENT ON COLUMN "user"."phone" IS '手机号';
COMMENT ON COLUMN "user"."email" IS '邮箱地址';
COMMENT ON COLUMN "user"."role" IS '用户角色：admin-管理员，user-普通用户';
COMMENT ON COLUMN "user"."status" IS '用户状态：ACTIVE-正常，DISABLED-禁用，LOCKED-锁定';
COMMENT ON COLUMN "user"."last_login_time" IS '最后登录时间';
COMMENT ON COLUMN "user"."login_count" IS '累计登录次数';
COMMENT ON COLUMN "user"."created_by" IS '创建人';
COMMENT ON COLUMN "user"."updated_by" IS '更新人';
COMMENT ON COLUMN "user"."deleted" IS '逻辑删除标记：false-未删除，true-已删除';
COMMENT ON COLUMN "user"."create_time" IS '创建时间';
COMMENT ON COLUMN "user"."update_time" IS '最后更新时间';

-- ----------------------------------------
-- 对话会话表
-- ----------------------------------------
COMMENT ON TABLE "chat_session" IS '对话会话表，一个会话包含多条消息';
COMMENT ON COLUMN "chat_session"."id" IS '会话ID，主键自增';
COMMENT ON COLUMN "chat_session"."user_id" IS '所属用户ID，关联 user.id';
COMMENT ON COLUMN "chat_session"."title" IS '会话标题，默认取首条用户提问';
COMMENT ON COLUMN "chat_session"."create_time" IS '创建时间';

-- ----------------------------------------
-- 对话消息表
-- ----------------------------------------
COMMENT ON TABLE "chat_message" IS '对话消息表，保存用户提问与助手回复';
COMMENT ON COLUMN "chat_message"."id" IS '消息ID，主键自增';
COMMENT ON COLUMN "chat_message"."session_id" IS '所属会话ID，关联 chat_session.id';
COMMENT ON COLUMN "chat_message"."role" IS '消息角色：user-用户，assistant-助手';
COMMENT ON COLUMN "chat_message"."content" IS '消息正文';
COMMENT ON COLUMN "chat_message"."sources" IS 'RAG 参考来源，JSON 数组字符串，可为空';
COMMENT ON COLUMN "chat_message"."image" IS '用户上传的图片（Base64 或存储地址），可为空';
COMMENT ON COLUMN "chat_message"."degraded" IS '是否为降级回复（AI 调用失败时的兜底文案），降级回复不展示参考来源';
COMMENT ON COLUMN "chat_message"."create_time" IS '创建时间';

-- ----------------------------------------
-- 诈骗案例表
-- ----------------------------------------
COMMENT ON TABLE "scam_case" IS '诈骗案例表，作为案例库供 RAG 检索';
COMMENT ON COLUMN "scam_case"."id" IS '案例ID，主键自增';
COMMENT ON COLUMN "scam_case"."title" IS '案例标题';
COMMENT ON COLUMN "scam_case"."type" IS '诈骗类型，如刷单返利、冒充公检法等';
COMMENT ON COLUMN "scam_case"."content" IS '案例正文';
COMMENT ON COLUMN "scam_case"."tips" IS '防范提示';
COMMENT ON COLUMN "scam_case"."create_time" IS '创建时间';

-- ----------------------------------------
-- 知识库表
-- ----------------------------------------
COMMENT ON TABLE "knowledge" IS '知识库文档表，保存反诈知识条目及其文档元信息';
COMMENT ON COLUMN "knowledge"."id" IS '知识ID，主键自增';
COMMENT ON COLUMN "knowledge"."title" IS '知识标题';
COMMENT ON COLUMN "knowledge"."category" IS '知识分类，取值来自字典 knowledge_category';
COMMENT ON COLUMN "knowledge"."content" IS '知识正文';
COMMENT ON COLUMN "knowledge"."document_type" IS '文档类型: pdf, word, markdown, excel, text';
COMMENT ON COLUMN "knowledge"."source_file" IS '原始文件名';
COMMENT ON COLUMN "knowledge"."file_size" IS '文件大小(字节)';
COMMENT ON COLUMN "knowledge"."char_count" IS '文档字符数';
COMMENT ON COLUMN "knowledge"."status" IS '发布状态：DRAFT-草稿，PENDING_REVIEW-待审核，ACTIVE-已发布，REJECTED-已拒绝';
COMMENT ON COLUMN "knowledge"."vector_status" IS '向量化处理状态：PENDING/PROCESSING/SUCCESS/FAILED';
COMMENT ON COLUMN "knowledge"."create_time" IS '创建时间';
COMMENT ON COLUMN "knowledge"."update_time" IS '最后更新时间';

-- ----------------------------------------
-- 操作日志表
-- ----------------------------------------
COMMENT ON TABLE "operation_log" IS '操作日志表，记录后台关键操作及执行结果';
COMMENT ON COLUMN "operation_log"."id" IS '日志ID，主键自增';
COMMENT ON COLUMN "operation_log"."username" IS '操作人用户名';
COMMENT ON COLUMN "operation_log"."operation" IS '操作名称/业务描述';
COMMENT ON COLUMN "operation_log"."method" IS '被调用的接口方法标识（类名.方法名）';
COMMENT ON COLUMN "operation_log"."params" IS '请求参数（JSON）';
COMMENT ON COLUMN "operation_log"."result" IS '返回结果（JSON）';
COMMENT ON COLUMN "operation_log"."ip" IS '客户端IP地址';
COMMENT ON COLUMN "operation_log"."user_agent" IS '客户端 User-Agent';
COMMENT ON COLUMN "operation_log"."execution_time" IS '执行耗时（毫秒）';
COMMENT ON COLUMN "operation_log"."status" IS '执行状态：1-成功，0-失败';
COMMENT ON COLUMN "operation_log"."error_message" IS '失败时的异常信息';
COMMENT ON COLUMN "operation_log"."create_time" IS '创建时间';

-- ----------------------------------------
-- 消息反馈表
-- ----------------------------------------
COMMENT ON TABLE "message_feedback" IS '消息反馈表，记录用户对助手回答的评分与意见';
COMMENT ON COLUMN "message_feedback"."id" IS '反馈ID，主键自增';
COMMENT ON COLUMN "message_feedback"."message_id" IS '被反馈的消息ID，关联 chat_message.id';
COMMENT ON COLUMN "message_feedback"."user_id" IS '反馈用户ID，关联 user.id';
COMMENT ON COLUMN "message_feedback"."rating" IS '评分，1~5 分，可为空';
COMMENT ON COLUMN "message_feedback"."is_helpful" IS '是否有用：true-有用，false-没用';
COMMENT ON COLUMN "message_feedback"."feedback_type" IS '反馈类型（如内容错误、答非所问等）';
COMMENT ON COLUMN "message_feedback"."feedback_content" IS '反馈文字内容';
COMMENT ON COLUMN "message_feedback"."create_time" IS '创建时间';

-- ----------------------------------------
-- 知识标签表
-- ----------------------------------------
COMMENT ON TABLE "knowledge_tag" IS '知识标签表，用于知识条目的分类与检索';
COMMENT ON COLUMN "knowledge_tag"."id" IS '标签ID，主键自增';
COMMENT ON COLUMN "knowledge_tag"."name" IS '标签名称，全局唯一';
COMMENT ON COLUMN "knowledge_tag"."color" IS '标签展示颜色，如 #409EFF';
COMMENT ON COLUMN "knowledge_tag"."description" IS '标签说明';
COMMENT ON COLUMN "knowledge_tag"."knowledge_count" IS '关联的知识条目数量（冗余统计）';
COMMENT ON COLUMN "knowledge_tag"."create_time" IS '创建时间';

-- ----------------------------------------
-- 知识-标签关联表
-- ----------------------------------------
COMMENT ON TABLE "knowledge_tag_relation" IS '知识与标签的关联表，多对多关系';
COMMENT ON COLUMN "knowledge_tag_relation"."id" IS '关联ID，主键自增';
COMMENT ON COLUMN "knowledge_tag_relation"."knowledge_id" IS '知识ID，关联 knowledge.id';
COMMENT ON COLUMN "knowledge_tag_relation"."tag_id" IS '标签ID，关联 knowledge_tag.id';
COMMENT ON COLUMN "knowledge_tag_relation"."create_time" IS '创建时间';

-- ----------------------------------------
-- 知识版本历史表
-- ----------------------------------------
COMMENT ON TABLE "knowledge_version" IS '知识版本历史表，保存每次编辑前的快照';
COMMENT ON COLUMN "knowledge_version"."id" IS '版本ID，主键自增';
COMMENT ON COLUMN "knowledge_version"."knowledge_id" IS '所属知识ID，关联 knowledge.id';
COMMENT ON COLUMN "knowledge_version"."title" IS '该版本的知识标题';
COMMENT ON COLUMN "knowledge_version"."content" IS '该版本的知识正文';
COMMENT ON COLUMN "knowledge_version"."change_summary" IS '变更说明';
COMMENT ON COLUMN "knowledge_version"."operator_id" IS '操作人用户ID，关联 user.id';
COMMENT ON COLUMN "knowledge_version"."create_time" IS '创建时间（即该版本的生成时间）';

-- ----------------------------------------
-- 系统配置表
-- ----------------------------------------
COMMENT ON TABLE "system_config" IS '系统配置表，保存可在线调整的配置项';
COMMENT ON COLUMN "system_config"."id" IS '配置ID，主键自增';
COMMENT ON COLUMN "system_config"."config_key" IS '配置键，全局唯一';
COMMENT ON COLUMN "system_config"."config_value" IS '配置值';
COMMENT ON COLUMN "system_config"."config_type" IS '值类型（如 string、number、boolean、json）';
COMMENT ON COLUMN "system_config"."description" IS '配置说明';
COMMENT ON COLUMN "system_config"."is_editable" IS '是否允许在管理端修改';
COMMENT ON COLUMN "system_config"."create_time" IS '创建时间';
COMMENT ON COLUMN "system_config"."update_time" IS '最后更新时间';

-- ----------------------------------------
-- 知识审核表
-- ----------------------------------------
COMMENT ON TABLE "knowledge_audit" IS '知识审核记录表，保存知识条目的提交与审核状态';
COMMENT ON COLUMN "knowledge_audit"."id" IS '审核记录ID，主键自增';
COMMENT ON COLUMN "knowledge_audit"."knowledge_id" IS '被审核的知识ID，关联 knowledge.id';
COMMENT ON COLUMN "knowledge_audit"."title" IS '提交审核时的知识标题';
COMMENT ON COLUMN "knowledge_audit"."content" IS '提交审核时的知识正文';
COMMENT ON COLUMN "knowledge_audit"."category" IS '提交审核时的知识分类';
COMMENT ON COLUMN "knowledge_audit"."audit_status" IS '审核状态：pending-待审核，approved-已通过，rejected-已拒绝';
COMMENT ON COLUMN "knowledge_audit"."audit_comment" IS '审核意见';
COMMENT ON COLUMN "knowledge_audit"."submit_user_id" IS '提交人用户ID，关联 user.id';
COMMENT ON COLUMN "knowledge_audit"."audit_user_id" IS '审核人用户ID，关联 user.id';
COMMENT ON COLUMN "knowledge_audit"."submit_time" IS '提交时间';
COMMENT ON COLUMN "knowledge_audit"."audit_time" IS '审核完成时间';

-- ----------------------------------------
-- 字典类型表
-- ----------------------------------------
COMMENT ON TABLE "sys_dict_type" IS '字典类型表，定义一类可枚举值';
COMMENT ON COLUMN "sys_dict_type"."id" IS '字典类型ID，主键自增';
COMMENT ON COLUMN "sys_dict_type"."dict_code" IS '字典编码，全局唯一，如 knowledge_status';
COMMENT ON COLUMN "sys_dict_type"."dict_name" IS '字典名称';
COMMENT ON COLUMN "sys_dict_type"."description" IS '字典说明';
COMMENT ON COLUMN "sys_dict_type"."status" IS '启用状态：1-启用，0-停用';
COMMENT ON COLUMN "sys_dict_type"."create_time" IS '创建时间';
COMMENT ON COLUMN "sys_dict_type"."update_time" IS '最后更新时间';

-- ----------------------------------------
-- 字典项表
-- ----------------------------------------
COMMENT ON TABLE "sys_dict_item" IS '字典项表，保存字典类型下的具体枚举值';
COMMENT ON COLUMN "sys_dict_item"."id" IS '字典项ID，主键自增';
COMMENT ON COLUMN "sys_dict_item"."dict_code" IS '所属字典编码，关联 sys_dict_type.dict_code';
COMMENT ON COLUMN "sys_dict_item"."item_code" IS '枚举编码，与 dict_code 组合唯一';
COMMENT ON COLUMN "sys_dict_item"."item_name" IS '枚举显示名称';
COMMENT ON COLUMN "sys_dict_item"."item_value" IS '枚举附加值，可为空';
COMMENT ON COLUMN "sys_dict_item"."sort" IS '排序号，升序展示';
COMMENT ON COLUMN "sys_dict_item"."status" IS '启用状态：1-启用，0-停用';
COMMENT ON COLUMN "sys_dict_item"."remark" IS '备注';
COMMENT ON COLUMN "sys_dict_item"."create_time" IS '创建时间';
COMMENT ON COLUMN "sys_dict_item"."update_time" IS '最后更新时间';

-- ----------------------------------------
-- RAG 知识切片表
-- ----------------------------------------
COMMENT ON TABLE "rag_chunk" IS 'RAG 知识切片表，知识库/案例库切分后的检索单元';
COMMENT ON COLUMN "rag_chunk"."chunk_id" IS '切片ID（UUID），主键';
COMMENT ON COLUMN "rag_chunk"."source_type" IS '来源类型：knowledge-知识库，scam_case-案例库';
COMMENT ON COLUMN "rag_chunk"."source_id" IS '来源记录ID，指向 knowledge.id 或 scam_case.id';
COMMENT ON COLUMN "rag_chunk"."title" IS '切片所属文档标题（冗余，便于检索展示）';
COMMENT ON COLUMN "rag_chunk"."category" IS '切片所属分类（冗余）';
COMMENT ON COLUMN "rag_chunk"."content" IS '切片正文';
COMMENT ON COLUMN "rag_chunk"."search_text" IS '用于全文检索的分词文本';
COMMENT ON COLUMN "rag_chunk"."search_vector" IS '全文检索向量（由 search_text 自动生成的 tsvector）';
COMMENT ON COLUMN "rag_chunk"."created_at" IS '创建时间';

-- ----------------------------------------
-- 向量存储表（Spring AI PgVectorStore 内建表，initialize-schema=true 时自动创建）
-- 表不存在时整体跳过，避免关闭 initialize-schema 的环境迁移失败
-- ----------------------------------------
DO $$
BEGIN
    IF to_regclass('public.vector_store') IS NOT NULL THEN
        EXECUTE 'COMMENT ON TABLE vector_store IS ''向量存储表，保存 RAG 切片的向量与元数据（Spring AI PgVectorStore 管理）''';
        EXECUTE 'COMMENT ON COLUMN vector_store.id IS ''向量记录ID（UUID）''';
        EXECUTE 'COMMENT ON COLUMN vector_store.content IS ''切片正文''';
        EXECUTE 'COMMENT ON COLUMN vector_store.metadata IS ''切片元数据（JSON，含 source、title、type、dbId 等）''';
        EXECUTE 'COMMENT ON COLUMN vector_store.embedding IS ''切片向量（1024 维，COSINE_DISTANCE）''';
    END IF;
END $$;
