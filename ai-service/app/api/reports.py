from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.reports import ReportGenerateRequest, ReportGenerateResponse

router = APIRouter()


@router.post("/generate", response_model=ReportGenerateResponse)
def generate(request: ReportGenerateRequest) -> ReportGenerateResponse:
    provider = create_provider(request.model)
    return provider.generate_report(request)
