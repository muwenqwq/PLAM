from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class KpInfo(BaseModel):
    id: int
    name: str
    difficulty: int = 1


class DepInfo(BaseModel):
    fromField: int   # JSON 字段名用 from
    to: int
    relation: str = "prerequisite"

    class Config:
        fields = {'fromField': 'from'}


class ResInfo(BaseModel):
    id: int
    type: str
    knowledgePointId: int


class StudyPlanGenerateRequest(BaseModel):
    taskId: str
    studentId: int
    courseId: int
    profileJson: Dict[str, Any] = {}
    knowledgePoints: List[KpInfo] = []
    dependencies: List[Dict[str, Any]] = []
    resources: List[Dict[str, Any]] = []


class NodeItem(BaseModel):
    order: int
    knowledgePointId: int
    knowledgePoint: str
    recommendedResourceIds: List[int] = []
    estimatedMinutes: int = 30
    reason: str = ""
    completionCriteria: str = ""


class StudyPlanGenerateResponse(BaseModel):
    nodes: List[NodeItem]
    agentTrace: List[Dict[str, Any]] = []
