from fastapi import APIRouter
from src.schemas.profile import ProfileExtractRequest, ProfileExtractResponse
from src.agents import profile_agent

router = APIRouter(prefix="/ai/profile")


@router.post("/extract", response_model=ProfileExtractResponse)
def extract_profile(req: ProfileExtractRequest):
    result = profile_agent.run(req.taskId, req.studentMessage)
    return ProfileExtractResponse(
        profileJson=result["profileJson"],
        agentTrace=result["agentTrace"],
    )
