package com.edustudio.module.learningspace.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edustudio.module.knowledge.entity.KnowledgeFile;
import com.edustudio.module.knowledge.mapper.KnowledgeFileMapper;
import com.edustudio.module.learningspace.mapper.LearningSpaceCleanupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LearningSpaceDataCleaner {

    private final KnowledgeFileMapper knowledgeFileMapper;
    private final KnowledgeStorageCleaner storageCleaner;
    private final LearningSpaceCleanupMapper cleanupMapper;

    public void deleteAll(Long userId, Long spaceId) {
        List<KnowledgeFile> files = knowledgeFileMapper.selectList(new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getUserId, userId)
                .eq(KnowledgeFile::getSpaceId, spaceId)
                .eq(KnowledgeFile::getDeleted, 0));
        storageCleaner.deleteStoredFiles(files);

        cleanupMapper.deleteConversationMessages(userId, spaceId);
        cleanupMapper.deleteConversations(userId, spaceId);
        cleanupMapper.deleteAgentSteps(userId, spaceId);
        cleanupMapper.deleteAgentTasks(userId, spaceId);
        cleanupMapper.deleteQuizAnswers(userId, spaceId);
        cleanupMapper.deleteQuizQuestions(userId, spaceId);
        cleanupMapper.deleteQuizzes(userId, spaceId);
        cleanupMapper.deleteKnowledgeChunks(userId, spaceId);
        cleanupMapper.deleteKnowledgeFiles(userId, spaceId);
        cleanupMapper.deleteLearningPathItems(userId, spaceId);
        cleanupMapper.deleteLearningPaths(userId, spaceId);
        cleanupMapper.deleteGeneratedResources(userId, spaceId);
        cleanupMapper.deleteMasteryRecords(userId, spaceId);
        cleanupMapper.deleteLearningReports(userId, spaceId);
        cleanupMapper.deleteUserProfiles(userId, spaceId);
    }
}
