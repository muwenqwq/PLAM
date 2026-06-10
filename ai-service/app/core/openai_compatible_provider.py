from app.core.mock_provider import MockLLMProvider
from app.schemas.agents import AgentRunRequest, AgentRunResponse
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.model import ModelTestRequest, ModelTestResponse
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse


class OpenAICompatibleProvider(MockLLMProvider):
    """Current sprint keeps real calls disabled unless later wired with a safe HTTP client."""

    def test_connection(self, request: ModelTestRequest) -> ModelTestResponse:
        config = request.model
        if not config.api_key:
            return ModelTestResponse(
                success=False,
                provider_type=config.provider_type,
                model_name=config.model_name,
                latency_ms=0,
                message="OpenAI-compatible 模型缺少 API Key，已拒绝真实连接测试。",
                sample_output=None,
            )
        return ModelTestResponse(
            success=True,
            provider_type=config.provider_type,
            model_name=config.model_name,
            latency_ms=42,
            message="OpenAI-compatible Provider 封装已就绪；当前演示环境未发起外部真实调用。",
            sample_output="Provider 配置格式有效。",
        )

    def chat(self, request: ChatRequest) -> ChatResponse:
        return super().chat(request)

    def detect_intent(self, request: ChatIntentRequest) -> ChatIntentResponse:
        return super().detect_intent(request)

    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        return super().run_agents(request)

    def generate_resource(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        return super().generate_resource(request)
