"""PlannerAgent——规划资源生成方案"""

import time


def run(task_id: str, resource_types: list[str]) -> dict:
    start = time.time()

    trace = {
        "agentName": "PlannerAgent",
        "status": "success",
        "inputSummary": f"规划资源类型: {resource_types}",
        "outputSummary": f"规划生成 {len(resource_types)} 类资源",
        "modelName": "RuleEngine",
        "latencyMs": int((time.time() - start) * 1000),
    }

    return {"agentTrace": [trace]}
