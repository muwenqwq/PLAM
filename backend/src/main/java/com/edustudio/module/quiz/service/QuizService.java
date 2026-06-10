package com.edustudio.module.quiz.service;

import com.edustudio.common.api.PageResult;
import com.edustudio.module.profile.vo.MasteryRecordVO;
import com.edustudio.module.quiz.dto.QuizGenerateRequest;
import com.edustudio.module.quiz.dto.QuizQueryRequest;
import com.edustudio.module.quiz.dto.QuizSubmitRequest;
import com.edustudio.module.quiz.vo.QuizResultVO;
import com.edustudio.module.quiz.vo.QuizVO;

import java.util.List;

public interface QuizService {

    QuizVO generate(QuizGenerateRequest request);

    PageResult<QuizVO> page(QuizQueryRequest request);

    QuizVO detail(Long id);

    QuizResultVO submit(Long id, QuizSubmitRequest request);

    QuizResultVO result(Long id);

    QuizResultVO analysis(Long id);

    List<MasteryRecordVO> mastery();
}
