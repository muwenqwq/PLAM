"""LearnAgent-A3 Python AI 服务入口"""

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from src.routers import (
    health,
    profile,
    resource,
    study_plan,
    tutor,
    assessment,
    course,
    critic,
)

app = FastAPI(
    title="LearnAgent-A3 AI Service",
    description="Python FastAPI AI 服务——大模型调用、RAG 检索、多智能体编排",
    version="1.0.0",
)


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """全局异常处理——LLM/Chroma 错误返回 JSON 而非 500"""
    return JSONResponse(
        status_code=200,  # Java 通过 JSON 中的 error 字段判断失败
        content={
            "error": str(exc),
            "agentTrace": [{
                "agentName": "System",
                "status": "failed",
                "outputSummary": str(exc)[:200],
            }],
        },
    )


# 注册路由
app.include_router(health.router)
app.include_router(profile.router)
app.include_router(resource.router)
app.include_router(study_plan.router)
app.include_router(tutor.router)
app.include_router(assessment.router)
app.include_router(course.router)
app.include_router(critic.router)
