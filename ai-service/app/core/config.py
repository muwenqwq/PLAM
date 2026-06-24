import os


class Settings:
    app_name: str = os.getenv("AI_SERVICE_NAME", "eduagent-ai-service")
    version: str = os.getenv("AI_SERVICE_VERSION", "0.1.0")
    mock_mode: bool = os.getenv("AI_SERVICE_MOCK_MODE", "true").lower() == "true"


settings = Settings()
