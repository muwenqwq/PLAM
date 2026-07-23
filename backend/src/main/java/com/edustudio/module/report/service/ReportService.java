package com.edustudio.module.report.service;

import com.edustudio.module.report.dto.ReportGenerateRequest;
import com.edustudio.module.report.vo.LearningReportVO;
import com.edustudio.module.report.vo.ReportOverviewVO;

import java.util.List;

public interface ReportService {

    ReportOverviewVO overview(Long spaceId);

    List<LearningReportVO> listBySpace(Long spaceId);

    LearningReportVO generate(ReportGenerateRequest request);

    LearningReportVO detail(Long id);

    void delete(Long id);

    String exportMarkdown(Long id);
}
