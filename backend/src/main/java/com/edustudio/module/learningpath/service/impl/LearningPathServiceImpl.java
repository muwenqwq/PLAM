package com.edustudio.module.learningpath.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiLearningPathAdjustRequest;
import com.edustudio.integration.ai.dto.AiLearningPathGenerateRequest;
import com.edustudio.integration.ai.dto.AiLearningPathGenerateResponse;
import com.edustudio.integration.ai.dto.AiLearningPathItemDTO;
import com.edustudio.module.learningpath.dto.LearningPathGenerateRequest;
import com.edustudio.module.learningpath.dto.LearningPathItemEditRequest;
import com.edustudio.module.learningpath.dto.LearningPathItemStatusRequest;
import com.edustudio.module.learningpath.dto.LearningPathQueryRequest;
import com.edustudio.module.learningpath.dto.LearningPathUpdateRequest;
import com.edustudio.module.learningpath.entity.LearningPath;
import com.edustudio.module.learningpath.entity.LearningPathItem;
import com.edustudio.module.learningpath.mapper.LearningPathItemMapper;
import com.edustudio.module.learningpath.mapper.LearningPathMapper;
import com.edustudio.module.learningpath.service.LearningPathService;
import com.edustudio.module.learningpath.vo.LearningPathItemVO;
import com.edustudio.module.learningpath.vo.LearningPathVO;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.service.UserProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathMapper learningPathMapper;
    private final LearningPathItemMapper learningPathItemMapper;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;
    private final UserProfileService userProfileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningPathVO generate(LearningPathGenerateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        Long providerId = modelProviderService.resolveProviderId(request.getModelProviderId());
        Map<String, Object> learnerProfile = userProfileService.getAiProfile(request.getSpaceId());
        AiLearningPathGenerateResponse response = aiServiceClient.generateLearningPath(AiLearningPathGenerateRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(providerId))
                .subject(request.getSubject())
                .goal(request.getGoal())
                .knowledgePoints(request.getKnowledgePoints())
                .days(request.getDays())
                .preference(request.getPreference())
                .profile(learnerProfile)
                .build());
        LearningPath path = new LearningPath();
        path.setUserId(userId);
        path.setSpaceId(request.getSpaceId());
        path.setTitle(response.getTitle());
        path.setGoal(request.getGoal());
        path.setSubject(request.getSubject());
        path.setPlanJson(response.getPlanJson() == null ? "{}" : JsonUtils.toJson(response.getPlanJson()));
        path.setProgressRate(BigDecimal.ZERO);
        path.setStartDate(LocalDate.now());
        path.setTargetDate(LocalDate.now().plusDays(request.getDays()));
        path.setStatus("active");
        learningPathMapper.insert(path);
        persistItems(path, response.getItems());
        userProfileService.recordActivity(
                request.getSpaceId(), request.getSubject(), request.getKnowledgePoints(), List.of(), "learning_path");
        return detail(path.getId());
    }

    @Override
    public PageResult<LearningPathVO> page(LearningPathQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<LearningPath> wrapper = new LambdaQueryWrapper<LearningPath>()
                .eq(LearningPath::getUserId, userId)
                .eq(LearningPath::getDeleted, 0)
                .eq(request.getSpaceId() != null, LearningPath::getSpaceId, request.getSpaceId())
                .eq(StringUtils.hasText(request.getStatus()), LearningPath::getStatus, request.getStatus())
                .like(StringUtils.hasText(request.getKeyword()), LearningPath::getTitle, request.getKeyword())
                .orderByDesc(LearningPath::getCreatedAt);
        Page<LearningPath> result = learningPathMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(path -> toVO(path, false)).toList(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public LearningPathVO detail(Long id) {
        return toVO(getOwnedPath(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningPathVO update(Long id, LearningPathUpdateRequest request) {
        LearningPath path = getOwnedPath(id);
        LocalDate startDate = request.getStartDate() == null ? path.getStartDate() : request.getStartDate();
        LocalDate targetDate = request.getTargetDate() == null ? path.getTargetDate() : request.getTargetDate();
        if (startDate != null && targetDate != null && targetDate.isBefore(startDate)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }

        path.setTitle(request.getTitle().trim());
        path.setGoal(request.getGoal().trim());
        path.setSubject(request.getSubject().trim());
        path.setStartDate(startDate);
        path.setTargetDate(targetDate);
        path.setPlanJson(updatedPlanJson(path.getPlanJson()));
        learningPathMapper.updateById(path);

        List<LearningPathItem> currentItems = items(path.getId());
        Map<Long, LearningPathItem> currentById = currentItems.stream()
                .collect(Collectors.toMap(LearningPathItem::getId, Function.identity()));
        Set<Long> retainedIds = new HashSet<>();
        int order = 1;
        for (LearningPathItemEditRequest itemRequest : request.getItems()) {
            LearningPathItem item;
            if (itemRequest.getId() == null) {
                item = new LearningPathItem();
                item.setPathId(path.getId());
                item.setUserId(path.getUserId());
                item.setSpaceId(path.getSpaceId());
            } else {
                item = currentById.get(itemRequest.getId());
                if (item == null || !retainedIds.add(itemRequest.getId())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "路径任务不存在、重复或不属于当前路径");
                }
            }
            applyItemEdit(path, item, itemRequest, order++);
            if (item.getId() == null) {
                learningPathItemMapper.insert(item);
            } else {
                learningPathItemMapper.updateById(item);
            }
        }
        for (LearningPathItem item : currentItems) {
            if (!retainedIds.contains(item.getId())) {
                learningPathItemMapper.deleteById(item.getId());
            }
        }
        refreshProgress(path.getId());
        return detail(path.getId());
    }

    @Override
    public LearningPathItemVO updateItemStatus(Long itemId, LearningPathItemStatusRequest request) {
        LearningPathItem item = getOwnedItem(itemId);
        item.setStatus(request.getStatus());
        item.setCompletedAt("done".equals(request.getStatus()) ? LocalDateTime.now() : null);
        learningPathItemMapper.updateById(item);
        refreshProgress(item.getPathId());
        return toItemVO(item);
    }

    @Override
    public List<LearningPathItemVO> today(Long spaceId) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        if (spaceId != null) {
            learningSpaceService.assertOwned(spaceId);
        }
        return learningPathItemMapper.selectList(new LambdaQueryWrapper<LearningPathItem>()
                        .eq(LearningPathItem::getUserId, userId)
                        .eq(LearningPathItem::getDeleted, 0)
                        .eq(spaceId != null, LearningPathItem::getSpaceId, spaceId)
                        .le(LearningPathItem::getDueDate, LocalDate.now())
                        .in(LearningPathItem::getStatus, List.of("todo", "doing"))
                        .orderByAsc(LearningPathItem::getDueDate)
                        .orderByAsc(LearningPathItem::getItemOrder))
                .stream().map(this::toItemVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningPathVO adjust(Long id) {
        LearningPath path = getOwnedPath(id);
        List<Map<String, Object>> itemMaps = items(path.getId()).stream()
                .map(item -> Map.<String, Object>of("title", item.getTitle(), "knowledge_points", item.getKnowledgePoints()))
                .toList();
        var response = aiServiceClient.adjustLearningPath(AiLearningPathAdjustRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(null))
                .pathTitle(path.getTitle())
                .currentProgress(path.getProgressRate())
                .reason("根据当前完成进度自动调整")
                .items(itemMaps)
                .build());
        int maxOrder = items(path.getId()).stream().mapToInt(LearningPathItem::getItemOrder).max().orElse(0);
        for (AiLearningPathItemDTO dto : nullSafe(response.getAdjustedItems())) {
            LearningPathItem item = toEntity(path, dto, ++maxOrder);
            learningPathItemMapper.insert(item);
        }
        path.setPlanJson(JsonUtils.toJson(Map.of("adjust_reason", "根据当前完成进度自动调整", "adjust_summary", response.getSummary(), "added_task_count", nullSafe(response.getAdjustedItems()).size())));
        learningPathMapper.updateById(path);
        return detail(id);
    }

    private void persistItems(LearningPath path, List<AiLearningPathItemDTO> items) {
        int order = 1;
        for (AiLearningPathItemDTO dto : nullSafe(items)) {
            learningPathItemMapper.insert(toEntity(path, dto, order++));
        }
    }

    private LearningPathItem toEntity(LearningPath path, AiLearningPathItemDTO dto, int order) {
        LearningPathItem item = new LearningPathItem();
        item.setPathId(path.getId());
        item.setUserId(path.getUserId());
        item.setSpaceId(path.getSpaceId());
        item.setItemOrder(order);
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setKnowledgePoints(dto.getKnowledgePoints() == null ? "[]" : JsonUtils.toJson(dto.getKnowledgePoints()));
        item.setEstimatedMinutes(dto.getEstimatedMinutes());
        item.setDueDate(path.getStartDate().plusDays(Math.max(0, (dto.getDueDay() == null ? order : dto.getDueDay()) - 1)));
        item.setStatus("todo");
        return item;
    }

    private void applyItemEdit(LearningPath path, LearningPathItem item,
                               LearningPathItemEditRequest request, int order) {
        String status = StringUtils.hasText(request.getStatus())
                ? request.getStatus()
                : (StringUtils.hasText(item.getStatus()) ? item.getStatus() : "todo");
        item.setItemOrder(order);
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription());
        item.setKnowledgePoints(request.getKnowledgePoints() == null
                ? (StringUtils.hasText(item.getKnowledgePoints()) ? item.getKnowledgePoints() : "[]")
                : JsonUtils.toJson(request.getKnowledgePoints()));
        item.setEstimatedMinutes(request.getEstimatedMinutes() == null ? 30 : request.getEstimatedMinutes());
        item.setDueDate(request.getDueDate() == null
                ? path.getStartDate().plusDays(Math.max(0, order - 1L))
                : request.getDueDate());
        item.setStatus(status);
        item.setCompletedAt("done".equals(status)
                ? (item.getCompletedAt() == null ? LocalDateTime.now() : item.getCompletedAt())
                : null);
    }

    @SuppressWarnings("unchecked")
    private String updatedPlanJson(String currentJson) {
        Map<String, Object> plan = new LinkedHashMap<>();
        if (StringUtils.hasText(currentJson)) {
            try {
                Map<String, Object> parsed = JsonUtils.fromJson(currentJson, Map.class);
                if (parsed != null) {
                    plan.putAll(parsed);
                }
            } catch (RuntimeException ignored) {
                // Keep the editable task list even if an older plan snapshot cannot be parsed.
            }
        }
        plan.put("edited_manually", true);
        plan.put("edited_at", LocalDateTime.now().toString());
        plan.remove("adjust_summary");
        plan.remove("adjustSummary");
        plan.remove("added_task_count");
        return JsonUtils.toJson(plan);
    }

    private void refreshProgress(Long pathId) {
        List<LearningPathItem> list = items(pathId);
        long done = list.stream().filter(item -> "done".equals(item.getStatus())).count();
        BigDecimal progress = list.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(done * 100.0 / list.size()).setScale(2, java.math.RoundingMode.HALF_UP);
        LearningPath path = learningPathMapper.selectById(pathId);
        path.setProgressRate(progress);
        path.setStatus(progress.compareTo(BigDecimal.valueOf(100)) >= 0 ? "completed" : "active");
        learningPathMapper.updateById(path);
    }

    private LearningPath getOwnedPath(Long id) {
        LearningPath path = learningPathMapper.selectOne(new LambdaQueryWrapper<LearningPath>()
                .eq(LearningPath::getId, id)
                .eq(LearningPath::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(LearningPath::getDeleted, 0));
        if (path == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学习路径不存在或无权访问");
        }
        return path;
    }

    private LearningPathItem getOwnedItem(Long itemId) {
        LearningPathItem item = learningPathItemMapper.selectOne(new LambdaQueryWrapper<LearningPathItem>()
                .eq(LearningPathItem::getId, itemId)
                .eq(LearningPathItem::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(LearningPathItem::getDeleted, 0));
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学习路径任务不存在或无权访问");
        }
        return item;
    }

    private List<LearningPathItem> items(Long pathId) {
        return learningPathItemMapper.selectList(new LambdaQueryWrapper<LearningPathItem>()
                .eq(LearningPathItem::getPathId, pathId)
                .eq(LearningPathItem::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(LearningPathItem::getDeleted, 0)
                .orderByAsc(LearningPathItem::getItemOrder));
    }

    private LearningPathVO toVO(LearningPath path, boolean includeItems) {
        return LearningPathVO.builder()
                .id(path.getId())
                .userId(path.getUserId())
                .spaceId(path.getSpaceId())
                .title(path.getTitle())
                .goal(path.getGoal())
                .subject(path.getSubject())
                .planJson(parseJson(path.getPlanJson()))
                .progressRate(path.getProgressRate())
                .startDate(path.getStartDate())
                .targetDate(path.getTargetDate())
                .status(path.getStatus())
                .createdAt(path.getCreatedAt())
                .items(includeItems ? items(path.getId()).stream().map(this::toItemVO).toList() : Collections.emptyList())
                .build();
    }

    private LearningPathItemVO toItemVO(LearningPathItem item) {
        return LearningPathItemVO.builder()
                .id(item.getId())
                .pathId(item.getPathId())
                .itemOrder(item.getItemOrder())
                .title(item.getTitle())
                .description(item.getDescription())
                .resourceId(item.getResourceId())
                .knowledgePoints(parseJson(item.getKnowledgePoints()))
                .estimatedMinutes(item.getEstimatedMinutes())
                .dueDate(item.getDueDate())
                .completedAt(item.getCompletedAt())
                .status(item.getStatus())
                .build();
    }

    private JsonNode parseJson(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, JsonNode.class) : null;
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
