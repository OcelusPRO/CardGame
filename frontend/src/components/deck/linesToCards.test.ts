import { describe, expect, it } from 'vitest'
import { linesToCards, removedLines, withLines, withoutLines } from './linesToCards'

describe('linesToCards', () => {
  it('keeps one card per non empty line', () => {
    expect(linesToCards('  un  \n\n deux \n')).toEqual(['un', 'deux'])
  })

  it('returns nothing for an empty editor', () => {
    expect(linesToCards('   \n  ')).toEqual([])
  })
})

describe('withLines', () => {
  it('adds the cards the box does not already hold', () => {
    expect(withLines('un\n', ['deux', 'trois'])).toBe('un\ndeux\ntrois')
  })

  it('never writes the same card twice, whoever proposed it first', () => {
    expect(withLines('un\ndeux', ['deux', 'deux', 'trois'])).toBe('un\ndeux\ntrois')
  })

  it('fills an empty box without leaving a blank first line', () => {
    expect(withLines('', ['un'])).toBe('un')
  })

  it('leaves the box untouched when there is nothing to add', () => {
    expect(withLines('un\n\n', ['un'])).toBe('un\n\n')
  })
})

describe('removedLines', () => {
  it('names the lines an edit took out', () => {
    expect(removedLines('un\ndeux\ntrois', 'un\ntrois')).toEqual(['deux'])
  })

  it('says nothing was removed when a line was only added', () => {
    expect(removedLines('un', 'un\ndeux')).toEqual([])
  })
})

describe('withoutLines', () => {
  it('takes back exactly what withLines put in', () => {
    const deck = ['deux', 'trois']
    expect(withoutLines(withLines('un', deck), deck)).toBe('un')
  })

  it('leaves the lines it was not asked about', () => {
    expect(withoutLines('un\ndeux', ['trois'])).toBe('un\ndeux')
  })
})
