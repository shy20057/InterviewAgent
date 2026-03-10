package com.atguigu.entity.dto;

import lombok.Data;

/**
 * 提交答案请求
 */
@Data
public class SubmitAnswerRequestDTO {
    private String sessionId;
    private String answer;
}
