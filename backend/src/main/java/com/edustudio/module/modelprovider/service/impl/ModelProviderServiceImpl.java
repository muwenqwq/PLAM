package com.edustudio.module.modelprovider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiModelConfigDTO;
import com.edustudio.integration.ai.dto.AiModelTestRequest;
import com.edustudio.integration.ai.dto.AiModelTestResponse;
import com.edustudio.module.modelprovider.dto.ModelProviderCreateRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderQueryRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderTestRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderUpdateRequest;
import com.edustudio.module.modelprovider.entity.AiModelProvider;
import com.edustudio.module.modelprovider.mapper.ModelProviderMapper;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.modelprovider.vo.ModelProviderTestVO;
import com.edustudio.module.modelprovider.vo.ModelProviderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModelProviderServiceImpl extends ServiceImpl<ModelProviderMapper, AiModelProvider>
        implements ModelProviderService {

    private static final String ACTIVE_STATUS = "active";
    private static final String MOCK_TYPE = "mock";
    private static final String DEFAULT_MOCK_MODEL = "mock-chat-v1";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AiServiceClient aiServiceClient;

    @Value("${eduagent.jwt.secret:eduagent-studio-dev-secret-change-in-production}")
    private String cryptoSecret;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderVO create(ModelProviderCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        boolean firstProvider = count(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getUserId, userId)
                .eq(AiModelProvider::getDeleted, 0)) == 0;
        boolean defaultProvider = Boolean.TRUE.equals(request.getDefaultProvider()) || firstProvider;
        if (defaultProvider) {
            clearDefault(userId);
        }

        AiModelProvider entity = new AiModelProvider();
        entity.setUserId(userId);
        entity.setProviderName(request.getProviderName());
        entity.setProviderType(request.getProviderType());
        entity.setBaseUrl(request.getBaseUrl());
        applyApiKey(entity, request.getApiKey());
        entity.setModelName(request.getModelName());
        entity.setEmbeddingModel(request.getEmbeddingModel());
        entity.setTemperature(defaultValue(request.getTemperature(), new BigDecimal("0.70")));
        entity.setMaxTokens(request.getMaxTokens() == null ? 2048 : request.getMaxTokens());
        entity.setStreamEnabled(request.getStreamEnabled() == null || request.getStreamEnabled());
        entity.setDefaultProvider(defaultProvider);
        entity.setStatus(ACTIVE_STATUS);
        entity.setRemark(request.getRemark());
        save(entity);
        return toVO(entity);
    }

    @Override
    public PageResult<ModelProviderVO> page(ModelProviderQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<AiModelProvider> wrapper = new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getUserId, userId)
                .eq(AiModelProvider::getDeleted, 0)
                .eq(StringUtils.hasText(request.getProviderType()), AiModelProvider::getProviderType, request.getProviderType())
                .eq(StringUtils.hasText(request.getStatus()), AiModelProvider::getStatus, request.getStatus())
                .and(StringUtils.hasText(request.getKeyword()), query -> query
                        .like(AiModelProvider::getProviderName, request.getKeyword())
                        .or()
                        .like(AiModelProvider::getModelName, request.getKeyword()))
                .orderByDesc(AiModelProvider::getDefaultProvider)
                .orderByDesc(AiModelProvider::getUpdatedAt);
        Page<AiModelProvider> result = page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        List<ModelProviderVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public ModelProviderVO detail(Long id) {
        return toVO(getOwnedEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderVO update(Long id, ModelProviderUpdateRequest request) {
        AiModelProvider entity = getOwnedEntity(id);
        if (StringUtils.hasText(request.getProviderName())) {
            entity.setProviderName(request.getProviderName());
        }
        if (StringUtils.hasText(request.getProviderType())) {
            entity.setProviderType(request.getProviderType());
        }
        if (request.getBaseUrl() != null) {
            entity.setBaseUrl(request.getBaseUrl());
        }
        if (request.getApiKey() != null) {
            applyApiKey(entity, request.getApiKey());
        }
        if (StringUtils.hasText(request.getModelName())) {
            entity.setModelName(request.getModelName());
        }
        if (request.getEmbeddingModel() != null) {
            entity.setEmbeddingModel(request.getEmbeddingModel());
        }
        if (request.getTemperature() != null) {
            entity.setTemperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            entity.setMaxTokens(request.getMaxTokens());
        }
        if (request.getStreamEnabled() != null) {
            entity.setStreamEnabled(request.getStreamEnabled());
        }
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
        if (request.getRemark() != null) {
            entity.setRemark(request.getRemark());
        }
        updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiModelProvider entity = getOwnedEntity(id);
        Long userId = entity.getUserId();
        boolean wasDefault = Boolean.TRUE.equals(entity.getDefaultProvider());
        removeById(entity.getId());
        if (wasDefault) {
            promoteNewestAsDefault(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderVO setDefault(Long id) {
        AiModelProvider entity = getOwnedEntity(id);
        clearDefault(entity.getUserId());
        entity.setDefaultProvider(true);
        updateById(entity);
        return toVO(entity);
    }

    @Override
    public ModelProviderVO getDefault() {
        AiModelProvider entity = getDefaultEntity(LoginUserHolder.requireCurrentUserId());
        return entity == null ? toVO(mockEntity(LoginUserHolder.requireCurrentUserId())) : toVO(entity);
    }

    @Override
    public ModelProviderTestVO test(Long id, ModelProviderTestRequest request) {
        AiModelProvider entity = getOwnedEntity(id);
        AiModelTestResponse response = aiServiceClient.testModel(AiModelTestRequest.builder()
                .modelConfig(toConfig(entity))
                .prompt(request.getPrompt())
                .build());
        return ModelProviderTestVO.builder()
                .providerId(entity.getId())
                .success(Boolean.TRUE.equals(response.getSuccess()))
                .providerType(response.getProviderType())
                .modelName(response.getModelName())
                .latencyMs(response.getLatencyMs())
                .message(response.getMessage())
                .sampleOutput(response.getSampleOutput())
                .build();
    }

    @Override
    public AiModelConfigDTO resolveConfig(Long providerId) {
        if (providerId != null) {
            return toConfig(getOwnedEntity(providerId));
        }
        AiModelProvider defaultEntity = getDefaultEntity(LoginUserHolder.requireCurrentUserId());
        return defaultEntity == null ? toConfig(mockEntity(LoginUserHolder.requireCurrentUserId())) : toConfig(defaultEntity);
    }

    @Override
    public Long resolveProviderId(Long providerId) {
        if (providerId != null) {
            return getOwnedEntity(providerId).getId();
        }
        AiModelProvider defaultEntity = getDefaultEntity(LoginUserHolder.requireCurrentUserId());
        return defaultEntity == null ? null : defaultEntity.getId();
    }

    private AiModelProvider getOwnedEntity(Long id) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        AiModelProvider entity = getOne(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getId, id)
                .eq(AiModelProvider::getUserId, userId)
                .eq(AiModelProvider::getDeleted, 0), false);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "模型配置不存在或无权访问");
        }
        return entity;
    }

    private AiModelProvider getDefaultEntity(Long userId) {
        return getOne(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getUserId, userId)
                .eq(AiModelProvider::getDefaultProvider, true)
                .eq(AiModelProvider::getDeleted, 0)
                .last("LIMIT 1"), false);
    }

    private void clearDefault(Long userId) {
        lambdaUpdate()
                .eq(AiModelProvider::getUserId, userId)
                .eq(AiModelProvider::getDeleted, 0)
                .set(AiModelProvider::getDefaultProvider, false)
                .update();
    }

    private void promoteNewestAsDefault(Long userId) {
        AiModelProvider next = getOne(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getUserId, userId)
                .eq(AiModelProvider::getDeleted, 0)
                .orderByDesc(AiModelProvider::getUpdatedAt)
                .last("LIMIT 1"), false);
        if (next != null) {
            next.setDefaultProvider(true);
            updateById(next);
        }
    }

    private AiModelProvider mockEntity(Long userId) {
        AiModelProvider entity = new AiModelProvider();
        entity.setUserId(userId);
        entity.setProviderName("系统 Mock 模型");
        entity.setProviderType(MOCK_TYPE);
        entity.setBaseUrl("mock://local/llm");
        entity.setApiKeyMasked("MOCK-ONLY");
        entity.setModelName(DEFAULT_MOCK_MODEL);
        entity.setEmbeddingModel("mock-embedding-v1");
        entity.setTemperature(new BigDecimal("0.70"));
        entity.setMaxTokens(2048);
        entity.setStreamEnabled(false);
        entity.setDefaultProvider(true);
        entity.setStatus(ACTIVE_STATUS);
        entity.setRemark("用户未配置模型时的后端自动 Mock 兜底。");
        return entity;
    }

    private AiModelConfigDTO toConfig(AiModelProvider entity) {
        return AiModelConfigDTO.builder()
                .providerName(entity.getProviderName())
                .providerType(entity.getProviderType())
                .baseUrl(entity.getBaseUrl())
                .apiKey(decrypt(entity.getApiKeyEncrypted()))
                .modelName(entity.getModelName())
                .embeddingModel(entity.getEmbeddingModel())
                .temperature(entity.getTemperature())
                .maxTokens(entity.getMaxTokens())
                .streamEnabled(Boolean.TRUE.equals(entity.getStreamEnabled()))
                .extra(Map.of("mock", MOCK_TYPE.equalsIgnoreCase(entity.getProviderType())))
                .build();
    }

    private ModelProviderVO toVO(AiModelProvider entity) {
        return ModelProviderVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .providerName(entity.getProviderName())
                .providerType(entity.getProviderType())
                .baseUrl(entity.getBaseUrl())
                .apiKeyMasked(entity.getApiKeyMasked())
                .modelName(entity.getModelName())
                .embeddingModel(entity.getEmbeddingModel())
                .temperature(entity.getTemperature())
                .maxTokens(entity.getMaxTokens())
                .streamEnabled(entity.getStreamEnabled())
                .defaultProvider(Boolean.TRUE.equals(entity.getDefaultProvider()))
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void applyApiKey(AiModelProvider entity, String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            if (MOCK_TYPE.equalsIgnoreCase(entity.getProviderType())) {
                entity.setApiKeyEncrypted(null);
                entity.setApiKeyMasked("MOCK-ONLY");
            }
            return;
        }
        entity.setApiKeyEncrypted(encrypt(apiKey));
        entity.setApiKeyMasked(mask(apiKey));
    }

    private String mask(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        int length = apiKey.length();
        if (length <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(length - 4);
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[12];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "API Key 加密失败");
        }
    }

    private String decrypt(String encryptedText) {
        if (!StringUtils.hasText(encryptedText)) {
            return null;
        }
        try {
            String[] parts = encryptedText.split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "API Key 解密失败");
        }
    }

    private SecretKeySpec keySpec() throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256").digest(cryptoSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    private BigDecimal defaultValue(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }
}
