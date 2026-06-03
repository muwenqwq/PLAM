from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class AnswerItem(BaseModel):
    questionId: Optional[int] = None
    answer: Optional[str] = None
    correct: Optional[bool] = None


class AssessmentAnalyzeRequest(BaseModel):
    taskId: str
    studentId: int
    courseId: int
    knowledgePointId: int
    answers: List[Dict[str, Any]] = []
    currentMastery: Dict[str, float] = {}


class AssessmentAnalyzeResponse(BaseModel):
    score: float = 0.0
    totalScore: float = 100.0
    masteryDelta: Dict[str, float] = {}
    details: List[Dict[str, Any]] = []
    weakPointsIdentified: List[str] = []
    agentTrace: List[Dict[str, Any]] = []
