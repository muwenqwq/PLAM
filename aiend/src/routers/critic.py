from fastapi import APIRouter
from pydantic import BaseModel
from src.agents import critic_agent

router = APIRouter(prefix="/ai/critic")


class ReviewRequest(BaseModel):
    taskId: str
    resourceType: str = ""
    content: str = ""
    knowledgePointName: str = ""


@router.post("/review")
def review_content(req: ReviewRequest):
    result = critic_agent.run(req.taskId, req.resourceType, req.content, req.knowledgePointName)
    return {
        "qualityScore": result.get("qualityScore", 4.0),
        "issues": result.get("issues", []),
        "suggestions": result.get("suggestions", []),
        "agentTrace": result.get("agentTrace", []),
    }
