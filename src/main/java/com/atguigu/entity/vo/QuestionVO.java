package com.atguigu.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试问题响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionVO {
    private String sessionId;
    private Integer questionNumber;
    private String question;
    private String category;
    private String difficulty;
}
