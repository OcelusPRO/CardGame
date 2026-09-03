import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AnimationToggle } from './AnimationToggle'

describe('AnimationToggle', () => {
  it('reads as pressed while animations are on', () => {
    render(<AnimationToggle enabled onToggle={vi.fn()} />)
    const button = screen.getByRole('button', { name: /Désactiver les animations/i })
    expect(button).toHaveAttribute('aria-pressed', 'true')
  })

  it('offers to switch them back on once off', () => {
    render(<AnimationToggle enabled={false} onToggle={vi.fn()} />)
    const button = screen.getByRole('button', { name: /Activer les animations/i })
    expect(button).toHaveAttribute('aria-pressed', 'false')
  })

  it('calls back on click', async () => {
    const onToggle = vi.fn()
    render(<AnimationToggle enabled onToggle={onToggle} />)
    await userEvent.click(screen.getByRole('button'))
    expect(onToggle).toHaveBeenCalledOnce()
  })
})
