import json
from fastapi import APIRouter
from src.schemas.resource import ResourceGenerateRequest, ResourceGenerateResponse, ResourceItem
from src.agents import retriever_agent, planner_agent, resource_agent, quiz_agent, code_agent, critic_agent

router = APIRouter(prefix="/ai/resources")

RESOURCE_GENERATORS = {
    "explanation_doc": resource_agent,
    "mindmap": resource_agent,   # 也生成 text，格式标记为 mermaid
    "reading_material": resource_agent,
}

@router.post("/generate", response_model=ResourceGenerateResponse)
def generate_resources(req: ResourceGenerateRequest):
    task_id = req.taskId
    kp_name = req.knowledgePointName
    profile = req.profileJson
    trace_all = []
    resources = []

    # 1. 检索
    ret_result = retriever_agent.run(task_id, kp_name)
    chunks = ret_result["chunks"]
    trace_all.extend(ret_result["agentTrace"])

    # 2. 规划
    plan_result = planner_agent.run(task_id, req.resourceTypes)
    trace_all.extend(plan_result["agentTrace"])

    # 3. 生成每种资源
    for rtype in req.resourceTypes:
        if rtype == "quiz":
            result = quiz_agent.run_quiz(task_id, kp_name, profile)
            content = result["content"]
        elif rtype == "code_lab":
            result = code_agent.run(task_id, kp_name, profile, chunks)
            content = result["content"]
        else:
            gen = RESOURCE_GENERATORS.get(rtype, resource_agent)
            result = gen.run(task_id, rtype, kp_name, profile, chunks)
            content = result["content"]

        resources.append(ResourceItem(
            resourceType=rtype,
            title=f"{kp_name} - {rtype}",
            content=content,
            format="mermaid" if rtype == "mindmap" else "markdown",
        ))
        trace_all.extend(result.get("agentTrace", []))

    # 4. 审查
    review_details = []
    for r in resources:
        cr = critic_agent.run(task_id, r.resourceType, r.content, kp_name)
        review_details.append({
            "resourceType": r.resourceType,
            "qualityScore": cr.get("qualityScore", 4.0),
            "issues": cr.get("issues", []),
            "suggestions": cr.get("suggestions", []),
        })
        trace_all.extend(cr.get("agentTrace", []))

    avg_score = sum(d["qualityScore"] for d in review_details) / max(len(review_details), 1)

    return ResourceGenerateResponse(
        resources=resources,
        reviewResult={
            "averageScore": round(avg_score, 2),
            "details": review_details,
        },
        agentTrace=trace_all,
    )
