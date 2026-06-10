import hashlib


def stable_hash(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def similarity_score(query: str, content: str, rank: int) -> float:
    query_terms = set(query.lower())
    content_terms = set(content.lower())
    overlap = len(query_terms & content_terms)
    base = 0.92 - rank * 0.07
    return round(max(0.35, min(0.98, base + overlap * 0.01)), 4)
