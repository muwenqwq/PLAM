package com.edustudio.module.resource.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.edustudio.common.api.PageResult;
import com.edustudio.module.resource.dto.GeneratedResourceQueryRequest;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.vo.GeneratedResourceVO;

public interface GeneratedResourceService extends IService<GeneratedResource> {

    PageResult<GeneratedResourceVO> page(GeneratedResourceQueryRequest request);

    GeneratedResourceVO detail(Long id);

    GeneratedResourceVO toVO(GeneratedResource entity);
}
