package com.atguigu.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_messages") // 映射到MongoDB的集合名称  mysql里面叫表名  mongodb里面叫文档/集合
                           // 必须要加 @Document 注解才能让这个实体类成为 MongoDB 可识别和操作的实体。
public class ChatMessages {

    // 唯一标识 映射到MongoDB文档的 _id字段
    @Id
    private ObjectId messageId;
    private String memoryId;

    private String content; // 存储当前聊天记录列表的json字符串
}
