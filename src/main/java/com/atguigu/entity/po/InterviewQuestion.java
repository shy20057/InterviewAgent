package com.atguigu.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_question")
public class InterviewQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer questionOrder;
    private String questionText;
    private String category;
    private String difficulty;
    private String userAnswer;
    private String aiFeedback;
    private Integer score;
    private LocalDateTime answerTime;
}
