# E-R 关系图

本项目采用“逻辑外键为主、数据库强外键为辅”的策略。下图使用 Mermaid 表示主要业务实体关系，关系字段会在 Java 后端 Service 层进行校验。

```mermaid
erDiagram
    SYS_USER ||--o{ SYS_USER_ROLE : has
    SYS_ROLE ||--o{ SYS_USER_ROLE : grants
    SYS_USER ||--o{ LEARNING_SPACE : owns
    SYS_USER ||--|| USER_PROFILE : has
    SYS_USER ||--|| LEARNING_PREFERENCE : has
    SYS_USER ||--o{ AI_MODEL_PROVIDER : configures

    LEARNING_SPACE ||--o{ CONVERSATION : contains
    CONVERSATION ||--o{ CONVERSATION_MESSAGE : records

    LEARNING_SPACE ||--o{ KNOWLEDGE_FILE : contains
    KNOWLEDGE_FILE ||--o{ KNOWLEDGE_CHUNK : splits

    LEARNING_SPACE ||--o{ AGENT_TASK : starts
    AI_MODEL_PROVIDER ||--o{ AGENT_TASK : powers
    AGENT_TASK ||--o{ AGENT_STEP : includes
    AGENT_TASK ||--o{ GENERATED_RESOURCE : produces
    LEARNING_SPACE ||--o{ GENERATED_RESOURCE : stores

    LEARNING_SPACE ||--o{ LEARNING_PATH : plans
    LEARNING_PATH ||--o{ LEARNING_PATH_ITEM : contains
    GENERATED_RESOURCE ||--o{ LEARNING_PATH_ITEM : supports

    LEARNING_SPACE ||--o{ QUIZ : contains
    QUIZ ||--o{ QUIZ_QUESTION : includes
    QUIZ_QUESTION ||--o{ QUIZ_ANSWER : answered_by
    SYS_USER ||--o{ QUIZ_ANSWER : submits

    LEARNING_SPACE ||--o{ MASTERY_RECORD : tracks
    QUIZ ||--o{ MASTERY_RECORD : updates
    LEARNING_SPACE ||--o{ LEARNING_REPORT : generates
    SYS_USER ||--o{ OPERATION_LOG : writes

    SYS_USER {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar user_type
        varchar status
        datetime created_at
        datetime updated_at
        tinyint deleted
    }

    SYS_ROLE {
        bigint id PK
        varchar role_code UK
        varchar role_name
        varchar status
    }

    LEARNING_SPACE {
        bigint id PK
        bigint user_id
        varchar space_name
        varchar subject
        int resource_count
        int task_count
        varchar status
    }

    AI_MODEL_PROVIDER {
        bigint id PK
        bigint user_id
        varchar provider_name
        varchar provider_type
        varchar base_url
        varchar api_key_encrypted
        varchar api_key_masked
        varchar model_name
        varchar embedding_model
        decimal temperature
        int max_tokens
        tinyint stream_enabled
        tinyint is_default
        varchar status
    }

    AGENT_TASK {
        bigint id PK
        bigint user_id
        bigint space_id
        bigint provider_id
        varchar task_type
        json input_params
        varchar execution_status
        text output_summary
        json result_json
        text error_message
    }

    AGENT_STEP {
        bigint id PK
        bigint task_id
        bigint user_id
        varchar agent_name
        int step_order
        varchar execution_status
        json input_json
        json result_json
    }

    GENERATED_RESOURCE {
        bigint id PK
        bigint user_id
        bigint space_id
        bigint task_id
        varchar resource_type
        varchar title
        longtext content_markdown
        json content_json
        decimal quality_score
    }

    QUIZ {
        bigint id PK
        bigint user_id
        bigint space_id
        varchar title
        varchar subject
        decimal total_score
        varchar status
    }

    QUIZ_QUESTION {
        bigint id PK
        bigint quiz_id
        bigint user_id
        int question_order
        varchar question_type
        text stem
        json options_json
        text answer_text
    }

    QUIZ_ANSWER {
        bigint id PK
        bigint quiz_id
        bigint question_id
        bigint user_id
        text answer_text
        decimal score
        tinyint is_correct
    }
```

## 关系说明

1. 用户是所有个人学习数据的归属主体，核心表都保留 `user_id`。
2. 学习空间是学习活动的上下文，知识库、对话、任务、资源、路径、测验和报告都可以按 `space_id` 归档。
3. 智能体任务通过 `agent_task` 和 `agent_step` 分离总体任务与分步骤执行记录，便于前端展示执行过程。
4. 生成资源可来自智能体任务，也可在后续阶段支持人工创建或重新生成。
5. 测验结果会更新掌握度记录，并用于学习报告。
