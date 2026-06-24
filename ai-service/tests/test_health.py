from fastapi.testclient import TestClient

from app.main import app


def test_health_endpoint():
    client = TestClient(app)
    response = client.get("/ai/health")
    assert response.status_code == 200
    assert response.headers["content-type"] == "application/json; charset=utf-8"
    data = response.json()
    assert data["status"] == "UP"
    assert data["service_name"] == "eduagent-ai-service"
