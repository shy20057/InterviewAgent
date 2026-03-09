-- ============================================================
-- 面试模拟助手 - 数据库建表 SQL
-- 数据库名: guiguxiaozhi
-- ============================================================

-- 面试会话表
CREATE TABLE IF NOT EXISTS `interview_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID (UUID)',
    `user_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    `position` VARCHAR(32) NOT NULL DEFAULT 'java-backend' COMMENT '面试岗位: java-backend/frontend/fullstack',
    `difficulty` VARCHAR(16) NOT NULL DEFAULT 'medium' COMMENT '难度: easy/medium/hard/expert',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ongoing' COMMENT '状态: ongoing/completed',
    `total_questions` INT NOT NULL DEFAULT 0 COMMENT '总题数',
    `answered_count` INT NOT NULL DEFAULT 0 COMMENT '已答题数',
    `score` DOUBLE DEFAULT 0.0 COMMENT '总分',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试会话表';

-- 面试题目表
CREATE TABLE IF NOT EXISTS `interview_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `question_order` INT NOT NULL COMMENT '题目序号',
    `question_text` TEXT DEFAULT NULL COMMENT '问题内容',
    `category` VARCHAR(32) DEFAULT NULL COMMENT '分类',
    `difficulty` VARCHAR(16) DEFAULT NULL COMMENT '难度',
    `user_answer` TEXT DEFAULT NULL COMMENT '用户答案',
    `ai_feedback` TEXT DEFAULT NULL COMMENT 'AI点评',
    `score` INT DEFAULT NULL COMMENT '本题得分 (0-100)',
    `answer_time` DATETIME DEFAULT NULL COMMENT '答题时间',
    KEY `idx_session_id` (`session_id`),
    KEY `idx_question_order` (`session_id`, `question_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目表';

-- 用户简历表
CREATE TABLE IF NOT EXISTS `user_resume` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `resume_path` VARCHAR(512) DEFAULT NULL COMMENT '简历文件路径',
    `skills` TEXT DEFAULT NULL COMMENT '技能列表 (JSON)',
    `experience_years` INT DEFAULT NULL COMMENT '工作年限',
    `expected_position` VARCHAR(32) DEFAULT NULL COMMENT '期望职位',
    `upload_time` DATETIME DEFAULT NULL COMMENT '上传时间',
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户简历表';
