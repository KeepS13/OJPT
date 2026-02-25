/**
 * 仅用于前端“读取 JWT 的 exp 并判断剩余有效期”，不做签名校验。
 * 注意：任何前端解析结果都不应作为安全决策依据，安全校验必须在后端完成。
 */

function decodeBase64Url(input: string): string {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  return atob(padded)
}

/**
 * 返回 JWT 的过期时间戳（毫秒）。
 * - 若 token 无效或无 exp，返回 null
 */
export function getTokenExpiration(token: string): number | null {
  if (!token) return null
  const parts = token.split('.')
  if (parts.length < 2) return null

  try {
    const payloadPart = parts[1]
    if (!payloadPart) return null
    const payloadJson = decodeBase64Url(payloadPart)
    const payload = JSON.parse(payloadJson) as { exp?: number }
    if (typeof payload.exp !== 'number') return null
    return payload.exp * 1000
  } catch {
    return null
  }
}

