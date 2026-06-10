from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.model import ModelTestRequest, ModelTestResponse

__test__ = False

router = APIRouter()


@router.post("/test", response_model=ModelTestResponse)
def test_model(request: ModelTestRequest) -> ModelTestResponse:
    provider = create_provider(request.model)
    return provider.test_connection(request)
