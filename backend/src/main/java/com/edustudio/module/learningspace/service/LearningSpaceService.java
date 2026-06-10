package com.edustudio.module.learningspace.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.edustudio.common.api.PageResult;
import com.edustudio.module.learningspace.dto.LearningSpaceCreateRequest;
import com.edustudio.module.learningspace.dto.LearningSpaceQueryRequest;
import com.edustudio.module.learningspace.dto.LearningSpaceUpdateRequest;
import com.edustudio.module.learningspace.entity.LearningSpace;
import com.edustudio.module.learningspace.vo.LearningSpaceSummaryVO;
import com.edustudio.module.learningspace.vo.LearningSpaceVO;

public interface LearningSpaceService extends IService<LearningSpace> {

    LearningSpaceVO create(LearningSpaceCreateRequest request);

    PageResult<LearningSpaceVO> page(LearningSpaceQueryRequest request);

    LearningSpaceVO detail(Long id);

    LearningSpaceVO update(Long id, LearningSpaceUpdateRequest request);

    void delete(Long id);

    LearningSpaceVO setDefault(Long id);

    LearningSpaceVO getDefault();

    LearningSpaceSummaryVO summary(Long id);

    void assertOwned(Long id);
}
