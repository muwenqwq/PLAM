package com.learnagent.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learnagent.common.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
