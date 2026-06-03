from fastapi import APIRouter

router = APIRouter()


@router.get("/ai/health")
def health():
    return {"status": "UP", "chromaStatus": "UP", "llmStatus": "UP"}
