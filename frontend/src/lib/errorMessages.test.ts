import { describe, expect, it } from 'vitest'
import { errorMessage } from './errorMessages'

describe('errorMessage', () => {
  it('translates a known code', () => {
    expect(errorMessage('NICKNAME_TAKEN')).toBe('Ce pseudo est déjà pris.')
  })

  it('falls back on a generic wording', () => {
    expect(errorMessage('SOMETHING_NEW')).toBe("Quelque chose s'est mal passé.")
  })

  it('says nothing when there is no error', () => {
    expect(errorMessage(null)).toBe('')
  })
})
