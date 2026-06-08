package com.learnagent.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learnagent.common.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
