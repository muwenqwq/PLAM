package com.edustudio.module.profile.service.impl;

import com.edustudio.integration.ai.dto.AiProfileAnalyzeResponse;
import com.edustudio.module.profile.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserProfileServiceImplTest {

    @Test
    void shouldKeepExistingFoundationWhenAiOmitsFoundationLevel() {
        UserProfileServiceImpl service = new UserProfileServiceImpl(null, null, null);
        UserProfile profile = new UserProfile();
        profile.setFoundationLevel("beginner");

        AiProfileAnalyzeResponse analysis = new AiProfileAnalyzeResponse();
        analysis.setShouldUpdate(true);
        analysis.setFoundationLevel(null);
        analysis.setAdaptiveSummary("Prefer examples before abstractions.");

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "applyAnalysis", profile, analysis));
        assertEquals("beginner", profile.getFoundationLevel());
    }
}
