import request from './request'
import type { PageParams } from './base'

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

export interface ProblemCodeSubmissionResult {
  submissionId: string
  status: string
  message: string
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
  return request.post<ProblemCodeSubmissionResult>(`/problems/no/${problemNo}/submissions`, payload)
}

export function runProblemCode(payload: ProblemCodeRunPayload) {
  return request.post<ProblemCodeRunResult>('/problems/run', payload)
}
