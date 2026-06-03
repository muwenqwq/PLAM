package com.learnagent.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ResourceGenerateRequest {
    @NotNull private Long studentId;
    @NotNull private Long courseId;
    @NotNull private Long knowledgePointId;
    @NotEmpty private List<String> resourceTypes;
}
