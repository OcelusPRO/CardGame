import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ThemeToggle } from './ThemeToggle'

describe('ThemeToggle', () => {
  it('offers the light theme while the page is dark', () => {
    render(<ThemeToggle dark onToggle={vi.fn()} />)
    const button = screen.getByRole('button', { name: /Passer au thème clair/i })
    expect(button).toHaveAttribute('aria-pressed', 'true')
  })

  it('offers the dark theme while the page is light', () => {
    render(<ThemeToggle dark={false} onToggle={vi.fn()} />)
    const button = screen.getByRole('button', { name: /Passer au thème sombre/i })
    expect(button).toHaveAttribute('aria-pressed', 'false')
  })

  it('calls back on click', async () => {
    const onToggle = vi.fn()
    render(<ThemeToggle dark={false} onToggle={onToggle} />)
    await userEvent.click(screen.getByRole('button'))
    expect(onToggle).toHaveBeenCalledOnce()
  })
})
