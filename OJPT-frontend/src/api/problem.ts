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

