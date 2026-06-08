package com.learnagent;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.learnagent.common.mapper")
public class LearnAgentApplication {

    public static void main(String[] args) {
        // 启动前加载 .env 文件，将变量注入系统环境变量
        // 如果 .env 不存在则跳过（部署时用真实环境变量）
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")        // backend/ 目录下查找 .env
                    .ignoreIfMissing()      // .env 不存在时不报错
                    .load();
            dotenv.entries().forEach(entry -> {
                // 跳过空值，让 Spring 使用 application.yml 的默认值
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
        } catch (Exception e) {
            System.out.println("未找到 .env 文件，使用系统环境变量");
        }

        SpringApplication.run(LearnAgentApplication.class, args);
    }
}
