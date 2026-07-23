package com.edustudio.module.learningpath.service;

import com.edustudio.common.api.PageResult;
import com.edustudio.module.learningpath.dto.LearningPathGenerateRequest;
import com.edustudio.module.learningpath.dto.LearningPathItemStatusRequest;
import com.edustudio.module.learningpath.dto.LearningPathQueryRequest;
import com.edustudio.module.learningpath.dto.LearningPathUpdateRequest;
import com.edustudio.module.learningpath.vo.LearningPathItemVO;
import com.edustudio.module.learningpath.vo.LearningPathVO;

import java.util.List;

public interface LearningPathService {

    LearningPathVO generate(LearningPathGenerateRequest request);

    PageResult<LearningPathVO> page(LearningPathQueryRequest request);

    LearningPathVO detail(Long id);

    LearningPathVO update(Long id, LearningPathUpdateRequest request);

    LearningPathItemVO updateItemStatus(Long itemId, LearningPathItemStatusRequest request);

    List<LearningPathItemVO> today(Long spaceId);

    LearningPathVO adjust(Long id);
}
