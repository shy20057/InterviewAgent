package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.atguigu.mapper")
public class JavaAiLangchain4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaAiLangchain4jApplication.class, args);
    }

}
