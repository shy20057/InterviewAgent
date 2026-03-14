package com.atguigu.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单题答疑记录实体类
 * 对应表：interview_question
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_question")
public class InterviewQuestion {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID，关联 session 表
     */
    private String sessionId;


    /**
     * AI生成的原问题内容
     */
    private String agentQuestion;

    /**
     * 候选人作答内容
     */
    private String userAnswer;

    /**
     * 技术分类
     */
    private String category;


    /**
     * 技术正确性评分
     */
    private Double techAccuracy;

    /**
     * 知识深度评分
     */
    private Double techDepth;

    /**
     * 逻辑严谨性评分
     */
    private Double logicCohesion;

    /**
     * 岗位匹配度评分
     */
    private Double jobMatchScore;

    /**
     * 语速得分
     */
    private Double speechRateScore;

    /**
     * 发音清晰度
     */
    private Double speechClarity;

    /**
     * 自信度评分
     */
    private Double confidenceLevel;

    /**
     * 作答/提交时间
     */
    private LocalDateTime answerTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
