import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DEFAULT_AVATAR } from './avatarCatalog'
import { AvatarBuilder } from './AvatarBuilder'

describe('AvatarBuilder', () => {
  it('changes only the half that was clicked', async () => {
    const onChange = vi.fn()
    render(<AvatarBuilder value={DEFAULT_AVATAR} onChange={onChange} />)

    await userEvent.click(screen.getByRole('button', { name: 'Punk' }))

    expect(onChange).toHaveBeenCalledWith({ ...DEFAULT_AVATAR, topStyleId: 'head-punk' })
  })

  it('changes only the colour that was clicked', async () => {
    const onChange = vi.fn()
    render(<AvatarBuilder value={DEFAULT_AVATAR} onChange={onChange} />)

    await userEvent.click(screen.getByRole('button', { name: 'Couleur de tête #2ee6a8' }))

    expect(onChange).toHaveBeenCalledWith({ ...DEFAULT_AVATAR, topColor: '#2ee6a8' })
  })

  it('marks the current selection for assistive technology', () => {
    render(<AvatarBuilder value={DEFAULT_AVATAR} onChange={vi.fn()} />)

    expect(screen.getByRole('button', { name: 'Rond' })).toHaveAttribute('aria-pressed', 'true')
  })
})
