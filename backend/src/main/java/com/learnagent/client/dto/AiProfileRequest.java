package com.learnagent.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiProfileRequest {
    private String taskId;
    private String studentMessage;
    private List<Object> courseContext;
}
