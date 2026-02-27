import request from './request'
import type { PageParams } from './base'

export interface SubmissionCreatePayload {
  problemId: string | number
  language: string
  sourceCode: string
}

export interface SubmissionListQuery extends PageParams {
  problemId?: string | number
}

export interface SubmissionStatusUpdatePayload {
  status: string
  timeMs?: number
  memoryKb?: number
  compileMessage?: string
  judgeMessage?: string
}

export function createSubmission(payload: SubmissionCreatePayload) {
  return request.post('/submissions', payload)
}

export function getSubmission(submissionId: string | number) {
  return request.get(`/submissions/${submissionId}`)
}

export function getMySubmissions(params: SubmissionListQuery) {
  return request.get('/submissions', { params })
}

export function adminUpdateSubmissionStatus(
  submissionId: string | number,
  payload: SubmissionStatusUpdatePayload,
) {
  return request.post(`/admin/submissions/${submissionId}:setStatus`, payload)
}

