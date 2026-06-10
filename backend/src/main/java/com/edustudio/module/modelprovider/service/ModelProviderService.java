package com.edustudio.module.modelprovider.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.edustudio.common.api.PageResult;
import com.edustudio.integration.ai.dto.AiModelConfigDTO;
import com.edustudio.module.modelprovider.dto.ModelProviderCreateRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderQueryRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderTestRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderUpdateRequest;
import com.edustudio.module.modelprovider.entity.AiModelProvider;
import com.edustudio.module.modelprovider.vo.ModelProviderTestVO;
import com.edustudio.module.modelprovider.vo.ModelProviderVO;

public interface ModelProviderService extends IService<AiModelProvider> {

    ModelProviderVO create(ModelProviderCreateRequest request);

    PageResult<ModelProviderVO> page(ModelProviderQueryRequest request);

    ModelProviderVO detail(Long id);

    ModelProviderVO update(Long id, ModelProviderUpdateRequest request);

    void delete(Long id);

    ModelProviderVO setDefault(Long id);

    ModelProviderVO getDefault();

    ModelProviderTestVO test(Long id, ModelProviderTestRequest request);

    AiModelConfigDTO resolveConfig(Long providerId);

    Long resolveProviderId(Long providerId);
}
