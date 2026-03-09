package com.atguigu.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试会话响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewSessionVO {
    private String sessionId;
    private String userId;
    private String position;
    private String difficulty;
    private String status;
    private String startTime;
    private String message;
}
