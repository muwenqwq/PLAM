from pydantic import BaseModel
from typing import Optional


class AgentTrace(BaseModel):
    agentName: str
    status: str  # success / failed
    inputSummary: Optional[str] = ""
    outputSummary: Optional[str] = ""
    modelName: Optional[str] = ""
    latencyMs: Optional[int] = 0
