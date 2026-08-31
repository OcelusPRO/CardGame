import { describe, expect, it } from 'vitest'
import { parseServerMessage } from './serverMessages'

describe('parseServerMessage', () => {
  it('reads a state frame', () => {
    const message = parseServerMessage('{"type":"state","game":{"code":"ABCDE"}}')

    expect(message?.type).toBe('state')
  })

  it('reads an error frame', () => {
    expect(parseServerMessage('{"type":"error","code":"NOT_THE_HOST"}')).toEqual({
      type: 'error',
      code: 'NOT_THE_HOST',
    })
  })

  it('returns null on garbage rather than throwing', () => {
    expect(parseServerMessage('nope')).toBeNull()
    expect(parseServerMessage('{"nope":1}')).toBeNull()
  })
})
