import { describe, expect, it } from 'vitest'
import { splitSituation } from './situationParts'

describe('splitSituation', () => {
  it('breaks the sentence around every hole', () => {
    const parts = splitSituation('Avant ____ et après ____.')

    expect(parts.map((part) => part.kind)).toEqual(['text', 'blank', 'text', 'blank', 'text'])
    expect(parts.filter((part) => part.kind === 'blank').map((part) => part.blankIndex)).toEqual([0, 1])
  })

  it('adds a trailing hole when the sentence has none', () => {
    const parts = splitSituation('Ma pire idée de vacances :')

    expect(parts).toHaveLength(2)
    expect(parts[1].kind).toBe('blank')
  })

  it('keeps the text on both sides of a hole', () => {
    const parts = splitSituation('Le pire, ____, voilà.')

    expect(parts[0].value).toBe('Le pire, ')
    expect(parts[2].value).toBe(', voilà.')
  })
})
