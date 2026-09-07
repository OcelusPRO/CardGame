import { describe, expect, it } from 'vitest'
import { gamePath, gameUrl, isOverlay, spectatePath, spectateUrl } from './gameLinks'

describe('game links', () => {
  it('builds the single address a game is played at', () => {
    expect(gamePath('ABCDE')).toBe('/game/ABCDE')
  })

  it('makes the invitation out of the page origin', () => {
    expect(gameUrl('ABCDE', 'https://jeu.example')).toBe('https://jeu.example/game/ABCDE')
  })

  it('carries no extra path a player would have to strip', () => {
    expect(gameUrl('ABCDE', 'http://localhost:8080').endsWith('/game/ABCDE')).toBe(true)
  })

  it('keeps the stream view at an address of its own', () => {
    expect(spectatePath('ABCDE')).toBe('/spec/ABCDE')
    expect(spectateUrl('ABCDE', false, 'https://jeu.example')).toBe('https://jeu.example/spec/ABCDE')
  })

  it('marks the OBS variant in the query, so the link alone carries the choice', () => {
    expect(spectateUrl('ABCDE', true, 'https://jeu.example')).toBe(
      'https://jeu.example/spec/ABCDE?overlay=1',
    )
  })

  it('reads the overlay flag back off a query string', () => {
    expect(isOverlay('?overlay=1')).toBe(true)
    expect(isOverlay('')).toBe(false)
    expect(isOverlay('?overlay=0')).toBe(false)
  })
})
