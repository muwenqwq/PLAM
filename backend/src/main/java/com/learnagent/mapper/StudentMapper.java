package com.learnagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learnagent.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
