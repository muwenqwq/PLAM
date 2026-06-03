from fastapi import APIRouter
from src.schemas.assessment import AssessmentAnalyzeRequest, AssessmentAnalyzeResponse
from src.agents import quiz_agent

router = APIRouter(prefix="/ai/assessment")


@router.post("/analyze", response_model=AssessmentAnalyzeResponse)
def analyze_assessment(req: AssessmentAnalyzeRequest):
    result = quiz_agent.run_analyze(req.taskId, req.answers, req.currentMastery)

    return AssessmentAnalyzeResponse(
        score=result["score"],
        totalScore=result["totalScore"],
        masteryDelta=result.get("masteryDelta", {}),
        details=result.get("details", []),
        weakPointsIdentified=result.get("weakPointsIdentified", []),
        agentTrace=result.get("agentTrace", []),
    )
