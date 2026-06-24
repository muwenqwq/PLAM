package com.edustudio.module.agent.vo;

import com.edustudio.module.resource.vo.GeneratedResourceVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskResultVO {

    private AgentTaskVO task;

    private List<AgentStepVO> steps;

    private List<GeneratedResourceVO> resources;
}
