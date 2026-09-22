import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DeckTransfer } from './DeckTransfer'

describe('DeckTransfer import', () => {
  it('fills the name, description and modes from the pasted deck text', async () => {
    const user = userEvent.setup()
    render(<DeckTransfer packs={[]} situations={[]} punchlines={[]} onImport={vi.fn()} />)

    await user.type(
      screen.getByLabelText('Deck à importer'),
      '# Nom: Ambiance Salée\n# Description: Humour noir\n# Modes: cartes\n\n## Situations\nUne situation : ____.',
    )

    expect(screen.getByPlaceholderText('Nom du pack')).toHaveValue('Ambiance Salée')
    expect(screen.getByPlaceholderText('Description')).toHaveValue('Humour noir')
    expect(screen.getByRole('checkbox', { name: /Cartes distribuées/ })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: /Sans limites/ })).not.toBeChecked()
  })

  it('still lets the host override the parsed fields by hand afterwards', async () => {
    const user = userEvent.setup()
    render(<DeckTransfer packs={[]} situations={[]} punchlines={[]} onImport={vi.fn()} />)

    await user.type(screen.getByLabelText('Deck à importer'), '# Nom: Ambiance Salée\n')
    const nameInput = screen.getByPlaceholderText('Nom du pack')
    await user.clear(nameInput)
    await user.type(nameInput, 'Autre nom')

    expect(nameInput).toHaveValue('Autre nom')
  })
})
