package com.edustudio.module.companion.support;

import com.edustudio.module.companion.entity.CompanionRole;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CompanionRolePayload {

    private CompanionRolePayload() {
    }

    public static Map<String, Object> from(CompanionRole role) {
        if (role == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("role_id", role.getId());
        values.put("role_name", value(role.getRoleName()));
        values.put("role_identity", value(role.getRoleIdentity()));
        values.put("background", value(role.getBackground()));
        values.put("personality", value(role.getPersonality()));
        values.put("expertise", value(role.getExpertise()));
        values.put("hobbies", value(role.getHobbies()));
        values.put("speaking_style", value(role.getSpeakingStyle()));
        values.put("scenario", value(role.getScenario()));
        values.put("companion_goal", value(role.getCompanionGoal()));
        values.put("boundaries", value(role.getBoundaries()));
        values.put("custom_prompt", value(role.getCustomPrompt()));
        values.put("tags", value(role.getTags()));
        return values;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}