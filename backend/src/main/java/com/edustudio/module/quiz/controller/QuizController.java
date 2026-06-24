package com.edustudio.module.quiz.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.profile.vo.MasteryRecordVO;
import com.edustudio.module.quiz.dto.QuizGenerateRequest;
import com.edustudio.module.quiz.dto.QuizQueryRequest;
import com.edustudio.module.quiz.dto.QuizSubmitRequest;
import com.edustudio.module.quiz.service.QuizService;
import com.edustudio.module.quiz.vo.QuizResultVO;
import com.edustudio.module.quiz.vo.QuizVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "测验与掌握度")
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "生成测验")
    @PostMapping("/quizzes/generate")
    public Result<QuizVO> generate(@Valid @RequestBody QuizGenerateRequest request) {
        return Result.success(quizService.generate(request));
    }

    @Operation(summary = "分页查询测验")
    @GetMapping("/quizzes")
    public Result<PageResult<QuizVO>> page(@Valid QuizQueryRequest request) {
        return Result.success(quizService.page(request));
    }

    @Operation(summary = "查询测验详情")
    @GetMapping("/quizzes/{id}")
    public Result<QuizVO> detail(@PathVariable Long id) {
        return Result.success(quizService.detail(id));
    }

    @Operation(summary = "提交测验答案")
    @PostMapping("/quizzes/{id}/submit")
    public Result<QuizResultVO> submit(@PathVariable Long id, @Valid @RequestBody QuizSubmitRequest request) {
        return Result.success(quizService.submit(id, request));
    }

    @Operation(summary = "查询测验结果")
    @GetMapping("/quizzes/{id}/result")
    public Result<QuizResultVO> result(@PathVariable Long id) {
        return Result.success(quizService.result(id));
    }

    @Operation(summary = "查询测验分析")
    @GetMapping("/quizzes/{id}/analysis")
    public Result<QuizResultVO> analysis(@PathVariable Long id) {
        return Result.success(quizService.analysis(id));
    }

    @Operation(summary = "查询我的掌握度")
    @GetMapping("/mastery/me")
    public Result<List<MasteryRecordVO>> mastery() {
        return Result.success(quizService.mastery());
    }
}
