package com.atguigu.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 简历信息响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeVO {
    private Long id;
    private String userId;
    private String resumePath;
    private String skills;
    private String projectExperience;
    private String position;

}
