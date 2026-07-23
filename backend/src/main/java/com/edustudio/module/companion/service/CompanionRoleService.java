package com.edustudio.module.companion.service;

import com.edustudio.common.api.PageResult;
import com.edustudio.module.companion.dto.CompanionRoleCreateRequest;
import com.edustudio.module.companion.dto.CompanionRoleQueryRequest;
import com.edustudio.module.companion.dto.CompanionRoleUpdateRequest;
import com.edustudio.module.companion.entity.CompanionRole;
import com.edustudio.module.companion.vo.CompanionRoleVO;

public interface CompanionRoleService {

    CompanionRoleVO create(CompanionRoleCreateRequest request);

    PageResult<CompanionRoleVO> page(CompanionRoleQueryRequest request);

    CompanionRoleVO detail(Long id);

    CompanionRoleVO active();

    CompanionRoleVO update(Long id, CompanionRoleUpdateRequest request);

    void delete(Long id);

    CompanionRoleVO setDefault(Long id);

    CompanionRole getOwnedActiveRole(Long id);
}
