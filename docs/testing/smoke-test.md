# Smoke Test 说明

## 脚本位置

`scripts/smoke-test.ps1`

## 覆盖内容

1. Python AI 服务 `/ai/health`
2. Java 后端 `/api/health`
3. 登录接口 `/api/auth/login`
4. 默认模型配置与模型测试
5. 对话接口
6. Agent 任务接口
7. 资源生成接口
8. 学习路径生成接口
9. 测验生成接口
10. 报告 overview 接口
11. 前端 build

## 使用方式

先启动 MySQL、AI 服务、后端和前端依赖，再执行：

```powershell
.\scripts\smoke-test.ps1
```

如端口不同：

```powershell
.\scripts\smoke-test.ps1 -BackendBaseUrl http://127.0.0.1:8080 -AiBaseUrl http://127.0.0.1:8000
```

脚本会使用 `demo_student / 123456` 登录并自动携带 JWT 调用后续接口。
