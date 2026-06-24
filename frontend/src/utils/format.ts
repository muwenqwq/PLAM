export function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function percent(value?: number | string) {
  const n = Number(value || 0)
  return `${n.toFixed(1)}%`
}

export function jsonText(value: unknown) {
  if (!value) return ''
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}
