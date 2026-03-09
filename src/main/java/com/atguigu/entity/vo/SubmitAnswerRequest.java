package com.atguigu.entity.vo;

import lombok.Data;

/**
 * 提交答案请求
 */
@Data
public class SubmitAnswerRequest {
    private String sessionId;
    private String answer;
}
