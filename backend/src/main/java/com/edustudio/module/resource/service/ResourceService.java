package com.edustudio.module.resource.service;

import com.edustudio.common.api.PageResult;
import com.edustudio.module.resource.dto.ResourceGenerateRequest;
import com.edustudio.module.resource.dto.ResourceQueryRequest;
import com.edustudio.module.resource.dto.ResourceUpdateRequest;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import com.edustudio.module.resource.vo.ResourceGenerateResultVO;

public interface ResourceService {

    ResourceGenerateResultVO generate(ResourceGenerateRequest request);

    PageResult<GeneratedResourceVO> page(ResourceQueryRequest request);

    GeneratedResourceVO detail(Long id);

    GeneratedResourceVO update(Long id, ResourceUpdateRequest request);

    void delete(Long id);

    String exportMarkdown(Long id);

    GeneratedResourceVO generateGraph(Long id);
}
