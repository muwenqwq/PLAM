from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class ChunkInfo(BaseModel):
    chunkId: Optional[str] = None
    content: str = ""
    sourceFile: str = ""


class ResourceGenerateRequest(BaseModel):
    taskId: str
    studentId: int
    courseId: int
    knowledgePointId: int
    knowledgePointName: str = ""
    profileJson: Dict[str, Any] = {}
    resourceTypes: List[str] = []
    courseChunks: List[ChunkInfo] = []


class ResourceItem(BaseModel):
    resourceType: str
    title: str
    content: str
    format: str = "markdown"
    sourcesJson: List[Dict[str, str]] = []


class ReviewDetail(BaseModel):
    resourceType: str
    qualityScore: float = 0.0
    issues: List[str] = []
    suggestions: List[str] = []


class ReviewResult(BaseModel):
    averageScore: float = 0.0
    details: List[ReviewDetail] = []


class ResourceGenerateResponse(BaseModel):
    resources: List[ResourceItem]
    reviewResult: Optional[ReviewResult] = None
    agentTrace: List[Dict[str, Any]] = []
