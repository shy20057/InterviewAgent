package com.atguigu.entity.dto;

import lombok.Data;

/**
 * 开始面试请求
 */
@Data
public class StartInterviewDTO {
    private String userId;
    private String position;       // java-backend / frontend / fullstack
    private String difficulty;     // easy / medium / hard / expert
    private String userSkills;     // 用户技能栈 "userSkills": ["Redis", "Spring Boot", "MySQL"]


}
