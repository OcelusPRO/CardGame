import { describe, expect, it } from 'vitest'
import { gamePath, gameUrl } from './gameLinks'

describe('game links', () => {
  it('builds the single address a game lives at', () => {
    expect(gamePath('ABCDE')).toBe('/game/ABCDE')
  })

  it('makes the invitation out of the page origin', () => {
    expect(gameUrl('ABCDE', 'https://jeu.example')).toBe('https://jeu.example/game/ABCDE')
  })

  it('carries no extra path a player would have to strip', () => {
    expect(gameUrl('ABCDE', 'http://localhost:8080').endsWith('/game/ABCDE')).toBe(true)
  })
})
