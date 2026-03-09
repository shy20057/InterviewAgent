package com.atguigu.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试结果响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewResultVO {
    private String sessionId;
    private Double score;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private String evaluation;
    private String endTime;
}
