# 测试计划

## 测试目标

验证 EduAgent Studio 在本地和 Docker 环境中具备启动、登录、核心业务演示和基础构建能力。

## 测试范围

1. Python AI 服务单元测试。
2. Java 后端单元测试与打包。
3. Vue 前端生产构建。
4. Docker Compose 配置校验。
5. 登录、模型配置、对话、智能体、资源、学习路径、测验、报告接口 Smoke Test。

## 测试命令

```powershell
cd ai-service
python -m pytest

cd ..\backend
mvn.cmd test
mvn.cmd clean package

cd ..\frontend
npm.cmd run build

cd ..
docker compose config
.\scripts\smoke-test.ps1
```

## 验收标准

- AI 服务测试无失败。
- 后端测试无失败，能够生成 Spring Boot jar。
- 前端构建成功。
- Docker Compose 配置可解析。
- Smoke Test 中所有检查项输出 `[PASS]`。

## 风险说明

如果本机没有 Docker 或 MySQL 未运行，Docker 和端到端 Smoke Test 可能无法执行。此时应先保证代码构建通过，并在具备服务环境后执行完整联调。
