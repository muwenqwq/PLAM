from datetime import datetime

from fastapi import APIRouter

from app.core.config import settings
from app.schemas.model import HealthResponse

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="UP",
        service_name=settings.app_name,
        version=settings.version,
        mock_mode=settings.mock_mode,
        current_time=datetime.now(),
    )

