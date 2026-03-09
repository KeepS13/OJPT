import { test, expect } from '@playwright/test'

test('题库分页应返回正确的 total/pages/size', async ({ request }) => {
  const res = await request.get('http://localhost:8080/api/problems?page=1&size=1')
  expect(res.status()).toBe(200)

  const body = await res.json()
  expect(body?.code).toBe(200)

  const data = body?.data
  expect(data?.size).toBe(1)
  expect(Array.isArray(data?.records)).toBeTruthy()

  const recordsLen = (data?.records?.length ?? 0) as number
  expect(data?.total).toBeGreaterThanOrEqual(recordsLen)

  // 数据库可能为空：为空时 pages/total 允许为 0；非空时 pages 应 >= 1
  if (recordsLen > 0) {
    expect(data?.total).toBeGreaterThan(0)
    expect(data?.pages).toBeGreaterThanOrEqual(1)
  }
})

