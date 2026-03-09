package com.atguigu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_resume")
public class UserResume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String resumePath;
    private String skills;
    private Integer experienceYears;
    private String expectedPosition;
    private LocalDateTime uploadTime;
}
