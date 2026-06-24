export function parseKnowledgePoints(input: string): string[] {
  return input
    .split(/[,，、;\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export function hasUsableTopic(subject?: string, points: string[] = []) {
  return Boolean(subject?.trim()) && points.length > 0
}
