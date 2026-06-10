package com.edustudio.module.agent.service;

import com.edustudio.common.api.PageResult;
import com.edustudio.module.agent.dto.AgentTaskCreateRequest;
import com.edustudio.module.agent.dto.AgentTaskQueryRequest;
import com.edustudio.module.agent.vo.AgentStepVO;
import com.edustudio.module.agent.vo.AgentTaskResultVO;
import com.edustudio.module.agent.vo.AgentTaskVO;
import com.edustudio.module.resource.vo.GeneratedResourceVO;

import java.util.List;

public interface AgentTaskService {

    AgentTaskResultVO createAndRun(AgentTaskCreateRequest request);

    PageResult<AgentTaskVO> page(AgentTaskQueryRequest request);

    AgentTaskVO detail(Long id);

    List<AgentStepVO> steps(Long id);

    AgentTaskResultVO rerun(Long id);

    GeneratedResourceVO saveResource(Long id);
}
