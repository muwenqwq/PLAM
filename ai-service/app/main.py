from fastapi import FastAPI

from app.api import agents, chat, health, model_test, resources
from app.core.config import settings

app = FastAPI(title=settings.app_name, version=settings.version)

app.include_router(health.router, prefix="/ai", tags=["health"])
app.include_router(model_test.router, prefix="/ai/model", tags=["model"])
app.include_router(chat.router, prefix="/ai/chat", tags=["chat"])
app.include_router(agents.router, prefix="/ai/agents", tags=["agents"])
app.include_router(resources.router, prefix="/ai/resources", tags=["resources"])

