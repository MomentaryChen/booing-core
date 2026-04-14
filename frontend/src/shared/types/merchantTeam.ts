export type MerchantTeamRole = 'OWNER' | 'STAFF'
export type MerchantTeamStatus = 'ACTIVE' | 'INACTIVE'
export type MerchantTeamMemberStatus = 'ACTIVE' | 'INACTIVE'

export interface MerchantTeamSummary {
  id: string
  merchantId: string
  name: string
  code: string
  status: MerchantTeamStatus
  memberCount: number
  active: boolean
  createdAt?: string | null
}

export interface MerchantTeamMemberSummary {
  id: string
  merchantId: string
  teamId: string
  userId: string
  username: string
  role: string
  status: MerchantTeamMemberStatus
  active: boolean
}

export interface MerchantTeamListResponse {
  teams: MerchantTeamSummary[]
}

export interface MerchantTeamCreateRequest {
  name: string
  code: string
  status?: MerchantTeamStatus
}

export interface MerchantTeamUpdateRequest {
  name: string
  status?: MerchantTeamStatus
}

export interface MerchantTeamMemberListResponse {
  members: MerchantTeamMemberSummary[]
}

export interface MerchantTeamMemberAddRequest {
  userId: string
  role: string
  status?: MerchantTeamMemberStatus
}
