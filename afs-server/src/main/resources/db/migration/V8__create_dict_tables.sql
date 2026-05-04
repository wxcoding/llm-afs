-- ========================================
-- 字典系统
-- V8__create_dict_tables.sql
-- ========================================

-- 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGSERIAL PRIMARY KEY,
    dict_code VARCHAR(100) NOT NULL UNIQUE,
    dict_name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 字典项表
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGSERIAL PRIMARY KEY,
    dict_code VARCHAR(100) NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_value VARCHAR(200),
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (dict_code, item_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_dict_code ON sys_dict_item(dict_code);
CREATE INDEX IF NOT EXISTS idx_dict_item_status ON sys_dict_item(status);
CREATE INDEX IF NOT EXISTS idx_dict_type_status ON sys_dict_type(status);

-- ========================================
-- 初始化数据 - 知识状态字典
-- ========================================

-- 插入知识状态字典类型
INSERT INTO sys_dict_type (dict_code, dict_name, description, status)
VALUES ('knowledge_status', '知识状态', '知识库内容的发布状态', 1)
ON CONFLICT (dict_code) DO NOTHING;

-- 插入知识状态字典项
INSERT INTO sys_dict_item (dict_code, item_code, item_name, sort, status)
VALUES 
    ('knowledge_status', 'DRAFT', '草稿', 1, 1),
    ('knowledge_status', 'PENDING_REVIEW', '待审核', 2, 1),
    ('knowledge_status', 'ACTIVE', '已发布', 3, 1),
    ('knowledge_status', 'REJECTED', '已拒绝', 4, 1)
ON CONFLICT (dict_code, item_code) DO NOTHING;

-- ========================================
-- 初始化数据 - 其他业务字典
-- ========================================

-- 插入用户角色字典类型
INSERT INTO sys_dict_type (dict_code, dict_name, description, status)
VALUES ('user_role', '用户角色', '系统用户角色', 1)
ON CONFLICT (dict_code) DO NOTHING;

-- 插入用户角色字典项
INSERT INTO sys_dict_item (dict_code, item_code, item_name, sort, status)
VALUES 
    ('user_role', 'admin', '管理员', 1, 1),
    ('user_role', 'user', '普通用户', 2, 1)
ON CONFLICT (dict_code, item_code) DO NOTHING;

-- 插入审核状态字典类型
INSERT INTO sys_dict_type (dict_code, dict_name, description, status)
VALUES ('audit_status', '审核状态', '知识审核记录状态', 1)
ON CONFLICT (dict_code) DO NOTHING;

-- 插入审核状态字典项
INSERT INTO sys_dict_item (dict_code, item_code, item_name, sort, status)
VALUES 
    ('audit_status', 'pending', '待审核', 1, 1),
    ('audit_status', 'approved', '已通过', 2, 1),
    ('audit_status', 'rejected', '已拒绝', 3, 1)
ON CONFLICT (dict_code, item_code) DO NOTHING;

-- 插入知识分类字典类型
INSERT INTO sys_dict_type (dict_code, dict_name, description, status)
VALUES ('knowledge_category', '知识分类', '知识库内容分类', 1)
ON CONFLICT (dict_code) DO NOTHING;

-- 插入知识分类字典项
INSERT INTO sys_dict_item (dict_code, item_code, item_name, sort, status)
VALUES 
    ('knowledge_category', '防范技巧', '防范技巧', 1, 1),
    ('knowledge_category', '诈骗类型', '诈骗类型', 2, 1),
    ('knowledge_category', '应对方法', '应对方法', 3, 1),
    ('knowledge_category', '案例分析', '案例分析', 4, 1)
ON CONFLICT (dict_code, item_code) DO NOTHING;
