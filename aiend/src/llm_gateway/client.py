"""LLM 调用客户端——Anthropic 兼容接口（支持 MiMo 等）"""

from anthropic import Anthropic
from src.config.settings import settings


def get_client() -> Anthropic:
    """获取 LLM 客户端"""
    return Anthropic(
        base_url=settings.LLM_BASE_URL,
        api_key=settings.LLM_API_KEY,
    )


def chat(system_prompt: str, user_message: str) -> str:
    """调用 LLM 对话，返回文本内容。自动跳过 ThinkingBlock"""
    client = get_client()

    response = client.messages.create(
        model=settings.LLM_MODEL,
        max_tokens=4096,
        messages=[{"role": "user", "content": user_message}],
        system=system_prompt if system_prompt else None,
    )

    # MiMo 返回 ThinkingBlock + TextBlock，只取 text 类型
    parts = []
    for block in response.content:
        if block.type == "text" and hasattr(block, "text"):
            parts.append(block.text)

    return "".join(parts) or ""


def chat_json(system_prompt: str, user_message: str) -> str:
    """调用 LLM，要求返回 JSON"""
    full_prompt = (
        f"{system_prompt}\n\n"
        "请严格返回合法的 JSON 格式，不要包含任何 markdown 代码块标记，"
        "不要包含思考过程，只输出最终的 JSON 对象。"
    )
    return chat(full_prompt, user_message)
