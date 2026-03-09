package com.atguigu.entity.po;

import lombok.Data;

@Data
public class ChatForm {

    private String memoryId; // 对话id
    private String message; // 用户输入的问题
}
