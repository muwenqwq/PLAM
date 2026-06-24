package com.edustudio.module.profile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.edustudio.module.profile.dto.UserProfileUpsertRequest;
import com.edustudio.module.profile.entity.UserProfile;
import com.edustudio.module.profile.vo.UserProfileVO;

public interface UserProfileService extends IService<UserProfile> {

    UserProfileVO getMe();

    UserProfileVO upsertMe(UserProfileUpsertRequest request);

    UserProfileVO getBySpace(Long spaceId);

    UserProfileVO upsertBySpace(Long spaceId, UserProfileUpsertRequest request);
}
