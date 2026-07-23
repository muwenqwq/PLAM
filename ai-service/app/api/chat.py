import json

from fastapi import APIRouter
from fastapi.encoders import jsonable_encoder
from fastapi.responses import StreamingResponse

from app.core.provider_factory import create_provider
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse

router = APIRouter()


@router.post("", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    provider = create_provider(request.model)
    return provider.chat(request)


@router.post("/stream")
def stream_chat(request: ChatRequest) -> StreamingResponse:
    provider = create_provider(request.model)

    def events():
        try:
            for event in provider.stream_chat(request):
                payload = json.dumps(jsonable_encoder(event), ensure_ascii=False)
                yield f"data: {payload}\n\n"
        except Exception as exc:  # noqa: BLE001 - stream errors must reach the browser as an event.
            payload = json.dumps(
                {"type": "error", "message": f"AI 流式生成失败：{str(exc)[:300]}"},
                ensure_ascii=False,
            )
            yield f"data: {payload}\n\n"

    return StreamingResponse(
        events(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/intent", response_model=ChatIntentResponse)
def detect_intent(request: ChatIntentRequest) -> ChatIntentResponse:
    provider = create_provider(request.model)
    return provider.detect_intent(request)
