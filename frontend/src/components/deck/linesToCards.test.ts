import { describe, expect, it } from 'vitest'
import { linesToCards } from './linesToCards'

describe('linesToCards', () => {
  it('keeps one card per non empty line', () => {
    expect(linesToCards('  un  \n\n deux \n')).toEqual(['un', 'deux'])
  })

  it('returns nothing for an empty editor', () => {
    expect(linesToCards('   \n  ')).toEqual([])
  })
})
