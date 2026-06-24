package com.edustudio.module.learningspace.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSpaceVO {

    private Long id;

    private Long userId;

    private String spaceName;

    private String subject;

    private String description;

    private String coverUrl;

    private String visibility;

    @JsonProperty("isDefault")
    private Boolean defaultSpace;

    private Integer resourceCount;

    private Integer taskCount;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
