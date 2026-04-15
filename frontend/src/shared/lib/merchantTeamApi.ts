import { useAuthStore } from '@/shared/stores/authStore'
import type {
  MerchantTeamCreateRequest,
  MerchantTeamListResponse,
  MerchantTeamMemberAddRequest,
  MerchantTeamMemberListResponse,
  MerchantTeamUpdateRequest,
} from '@/shared/types/merchantTeam'

class MerchantTeamApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

interface EndpointPathInput {
  merchantId: string
  teamId?: string
  memberId?: string
}

// Backend endpoint naming is still in-progress for team management.
// Keep all path assumptions in one adapter map so endpoint changes stay isolated.
const merchantTeamEndpoints = {
  listTeams: ({ merchantId }: EndpointPathInput) => `/api/merchant/${merchantId}/teams`,
  createTeam: ({ merchantId }: EndpointPathInput) => `/api/merchant/${merchantId}/teams`,
  updateTeam: ({ merchantId, teamId }: EndpointPathInput) =>
    `/api/merchant/${merchantId}/teams/${teamId ?? ''}`,
  listMembers: ({ merchantId, teamId }: EndpointPathInput) =>
    `/api/merchant/${merchantId}/teams/${teamId ?? ''}/members`,
  addMember: ({ merchantId, teamId }: EndpointPathInput) =>
    `/api/merchant/${merchantId}/teams/${teamId ?? ''}/members`,
  removeMember: ({ merchantId, teamId, memberId }: EndpointPathInput) =>
    `/api/merchant/${merchantId}/teams/${teamId ?? ''}/members/${memberId ?? ''}`,
} as const

interface BackendTeamSummary {
  id: number
  merchantId: number
  name: string
  code: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string | null
}

interface BackendTeamMemberSummary {
  id: number
  merchantId: number
  teamId: number
  userId: number
  username: string
  role: string
  status: 'ACTIVE' | 'INACTIVE'
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const token = useAuthStore.getState().token
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    try {
      const payload = (await response.json()) as { message?: string; error?: string }
      message = payload.message ?? payload.error ?? message
    } catch {
      // Keep fallback message.
    }
    throw new MerchantTeamApiError(response.status, message)
  }

  if (response.status === 204) {
    return null as T
  }

  return (await response.json()) as T
}

export function isMerchantTeamApiError(error: unknown): error is MerchantTeamApiError {
  return error instanceof MerchantTeamApiError
}

export async function listMerchantTeams(merchantId: string): Promise<MerchantTeamListResponse> {
  const teams = await requestJson<BackendTeamSummary[]>(
    merchantTeamEndpoints.listTeams({ merchantId })
  )
  return {
    teams: teams.map((team) => ({
      id: String(team.id),
      merchantId: String(team.merchantId),
      name: team.name,
      code: team.code,
      status: team.status,
      memberCount: 0,
      active: team.status === 'ACTIVE',
      createdAt: team.createdAt ?? null,
    })),
  }
}

export async function createMerchantTeam(
  merchantId: string,
  payload: MerchantTeamCreateRequest
): Promise<void> {
  await requestJson<BackendTeamSummary>(merchantTeamEndpoints.createTeam({ merchantId }), {
    method: 'POST',
    body: JSON.stringify({
      name: payload.name.trim(),
      code: payload.code.trim(),
      status: payload.status ?? 'ACTIVE',
    }),
  })
}

export async function updateMerchantTeam(
  merchantId: string,
  teamId: string,
  payload: MerchantTeamUpdateRequest
): Promise<void> {
  await requestJson<BackendTeamSummary>(merchantTeamEndpoints.updateTeam({ merchantId, teamId }), {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function listMerchantTeamMembers(
  merchantId: string,
  teamId: string
): Promise<MerchantTeamMemberListResponse> {
  const members = await requestJson<BackendTeamMemberSummary[]>(
    merchantTeamEndpoints.listMembers({ merchantId, teamId })
  )
  return {
    members: members.map((member) => ({
      id: String(member.id),
      merchantId: String(member.merchantId),
      teamId: String(member.teamId),
      userId: String(member.userId),
      username: member.username,
      role: member.role,
      status: member.status,
      active: member.status === 'ACTIVE',
    })),
  }
}

export async function addMerchantTeamMember(
  merchantId: string,
  teamId: string,
  payload: MerchantTeamMemberAddRequest
): Promise<void> {
  const userId = Number(payload.userId)
  if (!Number.isFinite(userId) || userId <= 0) {
    throw new MerchantTeamApiError(400, 'Invalid user id')
  }
  await requestJson<BackendTeamMemberSummary>(merchantTeamEndpoints.addMember({ merchantId, teamId }), {
    method: 'POST',
    body: JSON.stringify({
      userId,
      role: payload.role.trim(),
      ...(payload.status ? { status: payload.status } : {}),
    }),
  })
}

export async function removeMerchantTeamMember(
  merchantId: string,
  teamId: string,
  memberId: string
): Promise<void> {
  await requestJson<void>(merchantTeamEndpoints.removeMember({ merchantId, teamId, memberId }), {
    method: 'DELETE',
  })
}

