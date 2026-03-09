package com.atguigu;

import com.atguigu.entity.po.ChatMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@SpringBootTest
public class MongoCrudTest {

    @Autowired
    private MongoTemplate mongoTemplate; // MongoDB操作模板 提供操作MongoDB的API

    /*增*/
    @Test
    public void testInsert() {
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.setContent("聊天记录列表");
        mongoTemplate.insert(chatMessages);
    }

    /*查*/
    @Test
    public void testFind() {
        ChatMessages chatMessages = mongoTemplate.findById("69aceb3ddfb2cd503020943b", ChatMessages.class);
        System.out.println(chatMessages);
    }

    /*改*/
    @Test   
    public void testUpdate() {
        /*
        *  Criteria - 查询条件构造器 类似于 SQL: WHERE id = '696ba16bc806671af8048269'
        *  Query - 查询封装对象   类似于 SQL 的完整 SELECT 语句结构
        *  Update - 更新操作定义 类似于 SQL: SET content = '更新后的聊天记录列表
        *   -set(): 设置字段值
        *   -inc(): 数值递增
        *   -push(): 数组追加
        *   -unset(): 删除字段
        * */
        Criteria criteria = Criteria.where("_id").is("69aceb3ddfb2cd503020943b");
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", "更新后的聊天记录列表");
        
        mongoTemplate.upsert(query, update, ChatMessages.class);

    }

    /*删*/
    @Test
    public void testDelete() {
        Criteria criteria = Criteria.where("_id").is("69aced5502a656c58ae71109");
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);

    }
}