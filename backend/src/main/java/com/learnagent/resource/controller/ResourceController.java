package com.learnagent.resource.controller;

import com.learnagent.resource.dto.ResourceGenerateRequest;
import com.learnagent.resource.dto.ResourceResponse;
import com.learnagent.resource.service.ResourceService;
import com.learnagent.common.dto.ApiResult;
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
