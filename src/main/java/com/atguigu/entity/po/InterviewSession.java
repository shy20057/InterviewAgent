package com.atguigu.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_session")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String userId;
    private String position;
    private String difficulty;
    private String status;
    private Integer totalQuestions;
    private Integer answeredCount;
    private Double score;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
