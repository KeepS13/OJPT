import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

describe('vite config', () => {
  it('builds to the project dist directory by default', () => {
    const configPath = resolve(process.cwd(), 'vite.config.ts')
    const source = readFileSync(configPath, 'utf8')

    expect(source).toContain("outDir: 'dist'")
    expect(source).not.toContain('C:\\\\Program Files')
  })
})
