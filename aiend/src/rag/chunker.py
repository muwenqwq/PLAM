"""文档切片工具"""


def chunk_markdown(content: str, max_length: int = 1000, overlap: int = 100) -> list[dict]:
    """按段落切分 Markdown 文档"""
    paragraphs = content.split("\n\n")
    chunks = []
    current = ""
    seq = 0

    for para in paragraphs:
        if len(current) + len(para) > max_length and current:
            chunks.append({
                "index": seq,
                "content": current.strip(),
            })
            seq += 1
            current = para if len(para) > overlap else current[-overlap:] + "\n\n" + para
        else:
            current = current + "\n\n" + para if current else para

    if current.strip():
        chunks.append({
            "index": seq,
            "content": current.strip(),
        })

    return chunks
