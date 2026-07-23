package com.edustudio.module.learningspace.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface LearningSpaceCleanupMapper {

    @Delete("DELETE cm FROM conversation_message cm JOIN conversation c ON c.id = cm.conversation_id WHERE c.user_id = #{userId} AND c.space_id = #{spaceId}")
    int deleteConversationMessages(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM conversation WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteConversations(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE s FROM agent_step s JOIN agent_task t ON t.id = s.task_id WHERE t.user_id = #{userId} AND t.space_id = #{spaceId}")
    int deleteAgentSteps(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM agent_task WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteAgentTasks(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE a FROM quiz_answer a JOIN quiz q ON q.id = a.quiz_id WHERE q.user_id = #{userId} AND q.space_id = #{spaceId}")
    int deleteQuizAnswers(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE qq FROM quiz_question qq JOIN quiz q ON q.id = qq.quiz_id WHERE q.user_id = #{userId} AND q.space_id = #{spaceId}")
    int deleteQuizQuestions(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM quiz WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteQuizzes(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM knowledge_chunk WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteKnowledgeChunks(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM knowledge_file WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteKnowledgeFiles(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM learning_path_item WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteLearningPathItems(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM learning_path WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteLearningPaths(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM generated_resource WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteGeneratedResources(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM mastery_record WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteMasteryRecords(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM learning_report WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteLearningReports(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Delete("DELETE FROM user_profile WHERE user_id = #{userId} AND space_id = #{spaceId}")
    int deleteUserProfiles(@Param("userId") Long userId, @Param("spaceId") Long spaceId);
}
