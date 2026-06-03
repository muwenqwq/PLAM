from pydantic import BaseModel
from typing import List, Dict, Any


class TutorAskRequest(BaseModel):
    taskId: str
    studentId: int
    courseId: int
    question: str
    profileJson: Dict[str, Any] = {}


class TutorAskResponse(BaseModel):
    answer: str
    sources: List[Dict[str, str]] = []
    suggestedResourceIds: List[int] = []
    safetyStatus: str = "passed"
    agentTrace: List[Dict[str, Any]] = []
