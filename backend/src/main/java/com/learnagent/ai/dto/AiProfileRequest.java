package com.learnagent.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiProfileRequest {
    private String taskId;
    private String studentMessage;
    private List<Object> courseContext;
}
