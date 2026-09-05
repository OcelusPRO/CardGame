import { afterEach, describe, expect, it } from 'vitest'
import { forgetActiveGame, readActiveGame, rememberActiveGame } from './activeGame'

afterEach(() => forgetActiveGame())

describe('activeGame', () => {
  it('returns an empty string when nothing is remembered', () => {
    expect(readActiveGame()).toBe('')
  })

  it('round-trips the code a seated player is at', () => {
    rememberActiveGame('ABCDE')
    expect(readActiveGame()).toBe('ABCDE')
  })

  it('refuses to overwrite a real seat with an empty code', () => {
    rememberActiveGame('ABCDE')
    rememberActiveGame('')
    expect(readActiveGame()).toBe('ABCDE')
  })

  it('forgets the code when the player leaves the table', () => {
    rememberActiveGame('ABCDE')
    forgetActiveGame()
    expect(readActiveGame()).toBe('')
  })
})
