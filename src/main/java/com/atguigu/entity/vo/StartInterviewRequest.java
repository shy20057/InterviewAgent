package com.atguigu.entity.vo;

import lombok.Data;

/**
 * 开始面试请求
 */
@Data
public class StartInterviewRequest {
    private String userId;
    private String position;       // java-backend / frontend / fullstack
    private String difficulty;     // easy / medium / hard / expert
    private String userSkills;     // 用户技能栈
    private Long resumeId;         // 简历ID（可选）
}
