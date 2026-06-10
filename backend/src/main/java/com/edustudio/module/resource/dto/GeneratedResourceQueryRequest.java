package com.edustudio.module.resource.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GeneratedResourceQueryRequest {

    @Min(value = 1, message = "页码不能小于 1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long pageSize = 10L;

    private Long spaceId;

    private Long taskId;

    @Size(max = 64, message = "资源类型不能超过 64 个字符")
    private String resourceType;

    @Size(max = 128, message = "关键词不能超过 128 个字符")
    private String keyword;
}
