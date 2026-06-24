from abc import ABC, abstractmethod

from app.schemas.agents import AgentRunRequest, AgentRunResponse
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.model import ModelTestRequest, ModelTestResponse
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse


class LLMProvider(ABC):
    @abstractmethod
    def test_connection(self, request: ModelTestRequest) -> ModelTestResponse:
        raise NotImplementedError

    @abstractmethod
    def chat(self, request: ChatRequest) -> ChatResponse:
        raise NotImplementedError

    @abstractmethod
    def detect_intent(self, request: ChatIntentRequest) -> ChatIntentResponse:
        raise NotImplementedError

    @abstractmethod
    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        raise NotImplementedError

    @abstractmethod
    def generate_resource(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        raise NotImplementedError

