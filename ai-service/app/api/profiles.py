from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.profiles import ProfileAnalyzeRequest, ProfileAnalyzeResponse

router = APIRouter()


@router.post("/analyze", response_model=ProfileAnalyzeResponse)
def analyze(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    provider = create_provider(request.model)
    return provider.analyze_profile(request)
