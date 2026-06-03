from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class ProfileExtractRequest(BaseModel):
    taskId: str
    studentMessage: str
    courseContext: list = []


class ProfileData(BaseModel):
    major: str = ""
    grade: str = ""
    course: str = ""
    goal: str = ""
    foundation: str = ""
    weakness: List[str] = []
    preference: List[str] = []
    timeBudget: str = ""
    masteryMap: Dict[str, float] = {}


class ProfileExtractResponse(BaseModel):
    profileJson: ProfileData
    agentTrace: List[Dict[str, Any]] = []
