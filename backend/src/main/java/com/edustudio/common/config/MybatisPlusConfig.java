package com.edustudio.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.edustudio.module.user.mapper",
        "com.edustudio.module.role.mapper",
        "com.edustudio.module.learningspace.mapper",
        "com.edustudio.module.profile.mapper",
        "com.edustudio.module.modelprovider.mapper",
        "com.edustudio.module.companion.mapper",
        "com.edustudio.module.chat.mapper",
        "com.edustudio.module.agent.mapper",
        "com.edustudio.module.resource.mapper",
        "com.edustudio.module.knowledge.mapper",
        "com.edustudio.module.learningpath.mapper",
        "com.edustudio.module.quiz.mapper",
        "com.edustudio.module.report.mapper"
})
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}