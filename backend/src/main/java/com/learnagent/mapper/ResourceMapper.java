package com.learnagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learnagent.entity.Resource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {
}
