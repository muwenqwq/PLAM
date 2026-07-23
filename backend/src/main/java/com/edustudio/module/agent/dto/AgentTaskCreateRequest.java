package com.edustudio.module.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentTaskCreateRequest {

    @NotNull(message = "学习空间不能为空")
    private Long spaceId;

    private Long providerId;

    @NotBlank(message = "任务类型不能为空")
    @Pattern(regexp = "resource_generation|path_planning|quiz_generation|report_generation", message = "任务类型不合法")
    private String taskType = "resource_generation";

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题不能超过 200 个字符")
    private String title;

    @Size(max = 128, message = "学科不能超过 128 个字符")
    private String subject;

    @Size(max = 64, message = "资源类型不能超过 64 个字符")
    private String resourceType = "plan";

    private JsonNode inputParams;
}
