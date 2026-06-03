package com.learnagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learnagent.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
