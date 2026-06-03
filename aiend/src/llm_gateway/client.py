"""LLM 调用客户端——支持 Anthropic（MiMo）和 OpenAI（GLM/DeepSeek 等）"""

import re
from anthropic import Anthropic
from openai import OpenAI
from src.config.settings import settings


def _chat_anthropic(system_prompt: str, user_message: str) -> str:
    """Anthropic 协议（MiMo）"""
    client = Anthropic(
        base_url=settings.LLM_BASE_URL,
        api_key=settings.LLM_API_KEY,
    )

    response = client.messages.create(
        model=settings.LLM_MODEL,
        max_tokens=4096,
        messages=[{"role": "user", "content": user_message}],
        system=system_prompt if system_prompt else None,
        timeout=120.0,
    )

    # MiMo 返回 ThinkingBlock + TextBlock，只取 text 类型
    parts = []
    for block in response.content:
        if block.type == "text" and hasattr(block, "text"):
            text = block.text
            try:
                text.encode('utf-8')
            except UnicodeEncodeError:
                text = text.encode('utf-8', errors='replace').decode('utf-8')
            parts.append(text)

    return "".join(parts) or ""


def _chat_openai(system_prompt: str, user_message: str) -> str:
    """OpenAI 兼容协议（GLM / DeepSeek / 通义千问 等）"""
    client = OpenAI(
        base_url=settings.LLM_BASE_URL,
        api_key=settings.LLM_API_KEY,
    )

    response = client.chat.completions.create(
        model=settings.LLM_MODEL,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_message},
        ],
        max_tokens=4096,
        timeout=120,
    )
    return response.choices[0].message.content or ""


def chat(system_prompt: str, user_message: str) -> str:
    """调用 LLM 对话，自动选择提供方"""
    if settings.LLM_PROVIDER == "openai":
        return _chat_openai(system_prompt, user_message)
    return _chat_anthropic(system_prompt, user_message)


def chat_json(system_prompt: str, user_message: str) -> str:
    """调用 LLM，要求返回 JSON。自动剥离 markdown 代码块"""
    full_prompt = (
        f"{system_prompt}\n\n"
        "请严格返回合法的 JSON 格式，不要包含任何 markdown 代码块标记，"
        "不要包含思考过程，只输出最终的 JSON 对象。"
    )
    result = chat(full_prompt, user_message)

    # 剥离 markdown ```json ... ``` 包裹（GLM 等模型常见）
    match = re.search(r'```(?:json)?\s*\n?(.*?)```', result, re.DOTALL)
    if match:
        return match.group(1).strip()

    return result.strip()
