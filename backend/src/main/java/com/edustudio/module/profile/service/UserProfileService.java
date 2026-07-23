package com.edustudio.module.profile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.edustudio.module.profile.dto.UserProfileUpsertRequest;
import com.edustudio.module.profile.entity.UserProfile;
import com.edustudio.module.profile.vo.UserProfileVO;

import java.util.List;
import java.util.Map;

public interface UserProfileService extends IService<UserProfile> {

    UserProfileVO getMe();

    UserProfileVO upsertMe(UserProfileUpsertRequest request);

    UserProfileVO getBySpace(Long spaceId);

    UserProfileVO upsertBySpace(Long spaceId, UserProfileUpsertRequest request);

    UserProfileVO reanalyzeBySpace(Long spaceId);

    Map<String, Object> getAiProfile(Long spaceId);

    default void recordActivity(Long spaceId, String subject, List<String> knowledgePoints,
                                List<String> weakPoints, String source) {
        recordActivity(spaceId, subject, knowledgePoints, weakPoints, source, null);
    }

    void recordActivity(Long spaceId, String subject, List<String> knowledgePoints,
                        List<String> weakPoints, String source, String activitySummary);
}
