-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100),
    operation VARCHAR(100),
    method VARCHAR(200),
    params TEXT,
    result TEXT,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    execution_time BIGINT,
    status SMALLINT DEFAULT 1,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 操作日志表索引
CREATE INDEX IF NOT EXISTS idx_operation_log_username ON operation_log(username);
CREATE INDEX IF NOT EXISTS idx_operation_log_operation ON operation_log(operation);
CREATE INDEX IF NOT EXISTS idx_operation_log_create_time ON operation_log(create_time);