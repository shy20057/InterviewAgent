package com.atguigu.entity.po;

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
    private String skills;
    private String position;
    private String projectExperience;
    private String resumePath;
    private LocalDateTime uploadTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
