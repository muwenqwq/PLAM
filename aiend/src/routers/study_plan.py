from fastapi import APIRouter
from src.schemas.study_plan import StudyPlanGenerateRequest, StudyPlanGenerateResponse, NodeItem
from src.agents import path_agent

router = APIRouter(prefix="/ai/study-plan")


@router.post("/generate", response_model=StudyPlanGenerateResponse)
def generate_study_plan(req: StudyPlanGenerateRequest):
    result = path_agent.run(
        req.taskId, req.profileJson,
        [kp.model_dump() for kp in req.knowledgePoints],
        req.dependencies,
        req.resources,
    )

    nodes = [NodeItem(**n) for n in result["nodes"]]
    return StudyPlanGenerateResponse(
        nodes=nodes,
        agentTrace=result["agentTrace"],
    )
