package com.learnagent.controller;

import com.learnagent.dto.request.ResourceGenerateRequest;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.dto.response.ResourceResponse;
import com.learnagent.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping("/generate")
    public ApiResult<Map<String, Object>> generate(@Valid @RequestBody ResourceGenerateRequest req) {
        return ApiResult.ok(resourceService.generateResources(req));
    }

    @GetMapping("/{studentId}")
    public ApiResult<List<ResourceResponse>> list(@PathVariable Long studentId) {
        return ApiResult.ok(resourceService.getResourceList(studentId));
    }

    @GetMapping("/detail/{resourceId}")
    public ApiResult<ResourceResponse> detail(@PathVariable Long resourceId) {
        return ApiResult.ok(resourceService.getResourceDetail(resourceId));
    }
}
