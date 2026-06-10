def parse_text(file_name: str, source_text: str | None) -> str:
    if source_text and source_text.strip():
        return source_text.strip()
    topic = file_name.rsplit(".", 1)[0]
    return (
        f"{topic} 是当前学习空间中的资料。资料包含核心概念、典型例题、易错点和复习建议。"
        f"学习时应先理解 {topic} 的基础定义，再通过练习建立知识点之间的联系。"
    )
