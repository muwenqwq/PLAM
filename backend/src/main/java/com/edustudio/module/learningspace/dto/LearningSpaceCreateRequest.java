package com.edustudio.module.learningspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearningSpaceCreateRequest {

    @NotBlank(message = "学习空间名称不能为空")
    @Size(max = 128, message = "学习空间名称不能超过 128 个字符")
    private String spaceName;

    @NotBlank(message = "学科方向不能为空")
    @Size(max = 128, message = "学科方向不能超过 128 个字符")
    private String subject;

    @Size(max = 1000, message = "学习空间描述不能超过 1000 个字符")
    private String description;

    @Size(max = 512, message = "封面地址不能超过 512 个字符")
    private String coverUrl;

    @Pattern(regexp = "private|shared", message = "可见性只能是 private 或 shared")
    private String visibility = "private";
}
