"""应用配置，从 .env 文件加载"""

import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # LLM (Anthropic 兼容接口)
    LLM_BASE_URL: str = os.getenv("LLM_BASE_URL", "")
    LLM_API_KEY: str = os.getenv("LLM_API_KEY", "")
    LLM_MODEL: str = os.getenv("LLM_MODEL", "mimo-v2.5-pro")

    # Chroma
    CHROMA_PERSIST_DIR: str = os.getenv("CHROMA_PERSIST_DIR", "./chroma_data")


settings = Settings()
