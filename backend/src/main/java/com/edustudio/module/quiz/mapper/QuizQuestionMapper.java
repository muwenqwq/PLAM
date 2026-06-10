package com.edustudio.module.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edustudio.module.quiz.entity.QuizQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizQuestionMapper extends BaseMapper<QuizQuestion> {
}
