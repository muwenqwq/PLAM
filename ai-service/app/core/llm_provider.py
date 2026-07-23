from abc import ABC, abstractmethod
from collections.abc import Iterator
from typing import Any

from app.schemas.agents import AgentRunRequest, AgentRunResponse
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.model import ModelTestRequest, ModelTestResponse
from app.schemas.profiles import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse


class LLMProvider(ABC):
    @abstractmethod
    def test_connection(self, request: ModelTestRequest) -> ModelTestResponse:
        raise NotImplementedError

    @abstractmethod
    def chat(self, request: ChatRequest) -> ChatResponse:
        raise NotImplementedError

    def stream_chat(self, request: ChatRequest) -> Iterator[dict[str, Any]]:
        """Stream chat events while preserving the regular chat response contract."""
        response = self.chat(request)
        content = response.reply_markdown or ""
        chunk_size = max(12, min(48, max(1, len(content) // 12)))
        for index in range(0, len(content), chunk_size):
            yield {"type": "delta", "content": content[index : index + chunk_size]}
        yield {"type": "done", "response": response}

    @abstractmethod
    def detect_intent(self, request: ChatIntentRequest) -> ChatIntentResponse:
        raise NotImplementedError

    @abstractmethod
    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        raise NotImplementedError

    @abstractmethod
    def generate_resource(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        raise NotImplementedError

    @abstractmethod
    def analyze_profile(self, request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
        raise NotImplementedError
