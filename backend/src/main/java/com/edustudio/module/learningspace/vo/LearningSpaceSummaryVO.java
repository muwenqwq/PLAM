package com.edustudio.module.learningspace.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSpaceSummaryVO {

    private Long id;

    private String spaceName;

    private String subject;

    private Integer resourceCount;

    private Integer taskCount;

    private Integer profileCount;

    private Integer generatedResourceCount;

    private Integer activeTaskCount;

    private Integer upcomingTaskCount;
}
