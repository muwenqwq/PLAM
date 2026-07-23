package com.edustudio.module.companion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanionRoleQueryRequest {

    @Min(value = 1, message = "页码不能小于 1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long pageSize = 10L;

    @Size(max = 128, message = "关键词不能超过 128 个字符")
    private String keyword;

    private Boolean defaultRole;

    @Pattern(regexp = "active|disabled", message = "状态只能是 active 或 disabled")
    private String status;
}
