from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.agents import AgentRunRequest, AgentRunResponse

router = APIRouter()


@router.post("/run", response_model=AgentRunResponse)
def run_agents(request: AgentRunRequest) -> AgentRunResponse:
    provider = create_provider(request.model)
    return provider.run_agents(request)
