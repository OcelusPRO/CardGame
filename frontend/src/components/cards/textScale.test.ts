import { describe, expect, it } from 'vitest'
import { celebratedAnswerFontSize, punchlineFontSize, situationFontSize } from './textScale'

/** Reads the preferred, container relative term of a `clamp(min, preferred, max)`. */
function preferredCqi(value: string): number {
  return Number(value.split(',')[1].trim().replace('cqi', ''))
}

describe('punchlineFontSize', () => {
  it('scales with the width of the card, not with the breakpoint', () => {
    expect(punchlineFontSize('court')).toContain('cqi')
  })

  it('gives a short answer more room than a long one', () => {
    const short = preferredCqi(punchlineFontSize('un chat mouillé'))
    const long = preferredCqi(
      punchlineFontSize("l'irrépressible envie de tout brûler avant même le premier café du lundi"),
    )

    expect(short).toBeGreaterThan(long)
  })

  it('never lets the type grow past a readable maximum', () => {
    expect(punchlineFontSize('bref')).toContain('1.9rem)')
  })

  it('keeps a floor so a tiny card stays legible', () => {
    expect(punchlineFontSize('a'.repeat(200))).toContain('clamp(0.85rem')
  })

  it('ignores the surrounding spaces when measuring', () => {
    expect(punchlineFontSize('   court   ')).toBe(punchlineFontSize('court'))
  })
})

describe('situationFontSize', () => {
  it('gives a short situation the largest type', () => {
    expect(preferredCqi(situationFontSize('Le pire, ____.'))).toBe(9.5)
  })

  it('steps down for a wordy situation', () => {
    const long =
      'Ce qui a vraiment détruit la soirée de mariage de ma cousine germaine du côté de mon père, ' +
      'juste après le discours du témoin, ____.'

    expect(preferredCqi(situationFontSize(long))).toBeLessThan(7)
  })
})

describe('celebratedAnswerFontSize', () => {
  it('shouts the winning answer louder than a normal one', () => {
    const answer = 'un chat mouillé'

    expect(preferredCqi(celebratedAnswerFontSize(answer))).toBeGreaterThan(
      preferredCqi(punchlineFontSize(answer)),
    )
  })
})
