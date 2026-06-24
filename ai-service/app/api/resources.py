from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse

router = APIRouter()


@router.post("/generate", response_model=ResourceGenerateResponse)
def generate_resource(request: ResourceGenerateRequest) -> ResourceGenerateResponse:
    provider = create_provider(request.model)
    return provider.generate_resource(request)
