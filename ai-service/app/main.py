from fastapi import FastAPI

from app.api import agents, chat, health, learning_paths, model_test, quizzes, rag, reports, resources
from app.core.config import settings

app = FastAPI(title=settings.app_name, version=settings.version)


@app.middleware("http")
async def ensure_utf8_json_response(request, call_next):
    response = await call_next(request)
    content_type = response.headers.get("content-type", "")
    if content_type.startswith("application/json") and "charset=" not in content_type.lower():
        response.headers["content-type"] = "application/json; charset=utf-8"
    return response

app.include_router(health.router, prefix="/ai", tags=["health"])
app.include_router(model_test.router, prefix="/ai/model", tags=["model"])
app.include_router(chat.router, prefix="/ai/chat", tags=["chat"])
app.include_router(agents.router, prefix="/ai/agents", tags=["agents"])
app.include_router(resources.router, prefix="/ai/resources", tags=["resources"])
app.include_router(rag.router, prefix="/ai/rag", tags=["rag"])
app.include_router(learning_paths.router, prefix="/ai/learning-paths", tags=["learning-paths"])
app.include_router(quizzes.router, prefix="/ai/quizzes", tags=["quizzes"])
app.include_router(reports.router, prefix="/ai/reports", tags=["reports"])
