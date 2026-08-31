import { describe, expect, it } from 'vitest'
import { gameSocketUrl } from './socketUrl'

describe('gameSocketUrl', () => {
  it('uses ws on a plain origin', () => {
    expect(gameSocketUrl('ABCDE', 'http://localhost:5173')).toBe('ws://localhost:5173/ws/game/ABCDE')
  })

  it('uses wss on a secure origin', () => {
    expect(gameSocketUrl('ABCDE', 'https://jeu.example')).toBe('wss://jeu.example/ws/game/ABCDE')
  })

  it('drops any query string of the page', () => {
    expect(gameSocketUrl('ABCDE', 'https://jeu.example')).not.toContain('?')
  })
})
