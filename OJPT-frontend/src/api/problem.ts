import request from './request'
import type { PageParams } from './base'

const JUDGE_REQUEST_TIMEOUT_MS = 60000

export interface ProblemListQuery extends PageParams {
  keyword?: string
  difficulty?: string
  tagId?: string | number
  status?: string
  orderBy?: string
}

export function getProblemList(params: ProblemListQuery) {
  return request.get('/problems', { params })
}

export function getProblemDetail(problemId: string | number) {
  return request.get(`/problems/${problemId}`)
}

export function getProblemDetailByNo(problemNo: string | number) {
  return request.get(`/problems/no/${problemNo}`)
}

export interface ProblemSampleTestCase {
  id?: string
  caseType: 'SAMPLE'
  sortOrder: number
  inputText: string
  expectedOutput: string
  explanation?: string | null
}

export function getProblemSampleTestCases(problemNo: string | number) {
  return request.get<ProblemSampleTestCase[]>(`/problems/no/${problemNo}/test-cases/sample`)
}

export interface ProblemCodeSubmissionPayload {
  language: string
  sourceCode: string
}

export interface ProblemCodeDraftPayload {
  language: string
  sourceCode: string
}

export interface ProblemCodeDraft {
  problemNo: number
  language: string
  sourceCode: string
  updatedAt?: string | null
}

export interface ProblemCodeSubmissionResult {
  submissionId: string
  status: string
  message: string
  timeMs?: number | null
  rank?: number | null
  totalCaseCount?: number | null
  rankStats?: ProblemSubmissionRankStats | null
  caseResults?: ProblemCodeRunCaseResult[]
}

export interface ProblemSubmissionDistributionBucket {
  label: string
  min?: number | null
  max?: number | null
  count: number
}

export interface ProblemSubmissionRankStats {
  acceptedCount: number
  timeBuckets: ProblemSubmissionDistributionBucket[]
}

export interface ProblemCodeRunCasePayload {
  inputText: string
  expectedOutput: string
}

export interface ProblemCodeRunPayload {
  language: string
  sourceCode: string
  timeLimitMs?: number | null
  memoryLimitKb?: number | null
  cases: ProblemCodeRunCasePayload[]
}

export interface ProblemCodeRunCaseResult {
  caseIndex: number
  caseType?: 'SAMPLE' | 'HIDDEN' | 'CUSTOM' | string
  status: string
  inputText: string
  expectedOutput: string
  actualOutput?: string | null
  errorOutput?: string | null
  timeMs?: number | null
  message?: string | null
}

export interface ProblemCodeRunResult {
  status: string
  caseResults: ProblemCodeRunCaseResult[]
}

export function submitProblemCode(
  problemNo: string | number,
  payload: ProblemCodeSubmissionPayload,
) {
  return request.post<ProblemCodeSubmissionResult>(`/problems/no/${problemNo}/submissions`, payload, {
    timeout: JUDGE_REQUEST_TIMEOUT_MS,
  })
}

export function runProblemCode(payload: ProblemCodeRunPayload) {
  return request.post<ProblemCodeRunResult>('/problems/run', payload, {
    timeout: JUDGE_REQUEST_TIMEOUT_MS,
  })
}

export function getProblemCodeDraft(problemNo: string | number, language: string) {
  return request.get<ProblemCodeDraft | null>(`/problems/no/${problemNo}/draft`, {
    params: { language },
  })
}

export function saveProblemCodeDraft(
  problemNo: string | number,
  payload: ProblemCodeDraftPayload,
) {
  return request.put<ProblemCodeDraft>(`/problems/no/${problemNo}/draft`, payload)
}
