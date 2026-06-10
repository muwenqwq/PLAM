from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse

router = APIRouter()


@router.post("", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    provider = create_provider(request.model)
    return provider.chat(request)


@router.post("/intent", response_model=ChatIntentResponse)
def detect_intent(request: ChatIntentRequest) -> ChatIntentResponse:
    provider = create_provider(request.model)
    return provider.detect_intent(request)
