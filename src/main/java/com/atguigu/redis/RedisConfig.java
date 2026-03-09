package com.atguigu.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:}")
    private String redisHost;

    @Value("${spring.data.redis.port:}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;



    @Bean("redisTemplate")    // 创建Redis连接工厂
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>(); // 创建RedisTemplate对象 redis模板对象 简化Redis操作 封装 序列化 连接管理
        template.setConnectionFactory(factory); // 设置Redis连接工厂 自动管理Redis连接的获取与释放
        // 设置key的序列化方式 将键序列化为字符串格式
        template.setKeySerializer(RedisSerializer.string());
        // 设置value的序列化方式 将值序列化为JSON格式
        template.setValueSerializer(RedisSerializer.json());
        // 设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置hash的value序列化方式
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }
}