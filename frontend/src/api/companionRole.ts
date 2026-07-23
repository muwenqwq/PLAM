import request, { PageResult } from './request'

export interface CompanionRole {
  id?: number
  roleName: string
  roleIdentity?: string
  avatarUrl?: string
  themeColor?: string
  background?: string
  personality?: string
  expertise?: string
  hobbies?: string
  speakingStyle?: string
  scenario?: string
  companionGoal?: string
  boundaries?: string
  customPrompt?: string
  tags?: string
  defaultRole?: boolean
  status?: string
}

export function listCompanionRoles(params: any = {}) {
  return request.get<any, PageResult<CompanionRole>>('/companion-roles', { params })
}

export function getActiveCompanionRole() {
  return request.get<any, CompanionRole | null>('/companion-roles/active')
}

export function createCompanionRole(data: CompanionRole) {
  return request.post<any, CompanionRole>('/companion-roles', data)
}

export function updateCompanionRole(id: number, data: Partial<CompanionRole>) {
  return request.put<any, CompanionRole>(`/companion-roles/${id}`, data)
}

export function deleteCompanionRole(id: number) {
  return request.delete<any, void>(`/companion-roles/${id}`)
}

export function setDefaultCompanionRole(id: number) {
  return request.post<any, CompanionRole>(`/companion-roles/${id}/default`)
}
