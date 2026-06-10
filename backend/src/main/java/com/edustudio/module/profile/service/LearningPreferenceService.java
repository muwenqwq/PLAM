package com.edustudio.module.profile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.edustudio.module.profile.dto.LearningPreferenceUpsertRequest;
import com.edustudio.module.profile.entity.LearningPreference;
import com.edustudio.module.profile.vo.LearningPreferenceVO;

public interface LearningPreferenceService extends IService<LearningPreference> {

    LearningPreferenceVO getMe();

    LearningPreferenceVO upsertMe(LearningPreferenceUpsertRequest request);
}
