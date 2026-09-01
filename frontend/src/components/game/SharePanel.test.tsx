import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { SharePanel } from './SharePanel'

/**
 * The reveal block is hidden with `visibility`, not unmounted, so the lobby keeps its
 * size. jsdom loads no stylesheet, so these checks read the signals the component sets
 * itself: the `aria-hidden` flag and the size classes that must never change.
 */
function revealSlot(container: HTMLElement): HTMLElement {
  const slot = container.querySelector<HTMLElement>('[aria-hidden]')
  if (!slot) throw new Error('reveal slot not found')
  return slot
}

describe('SharePanel', () => {
  it('keeps the code out of sight by default', () => {
    const { container } = render(<SharePanel code="ABCDE" />)

    expect(revealSlot(container)).toHaveAttribute('aria-hidden', 'true')
    expect(revealSlot(container).className).toContain('invisible')
  })

  it('reveals it on demand', async () => {
    const { container } = render(<SharePanel code="ABCDE" />)

    await userEvent.click(screen.getByRole('button', { name: /Afficher le code/i }))

    expect(revealSlot(container)).toHaveAttribute('aria-hidden', 'false')
    expect(revealSlot(container).className).not.toContain('invisible')
    expect(screen.getByText('ABCDE')).toBeInTheDocument()
  })

  it('hides it again', async () => {
    const { container } = render(<SharePanel code="ABCDE" />)

    await userEvent.click(screen.getByRole('button', { name: /Afficher le code/i }))
    await userEvent.click(screen.getByRole('button', { name: /Masquer le code/i }))

    expect(revealSlot(container)).toHaveAttribute('aria-hidden', 'true')
  })

  it('never changes its footprint, so the lobby does not jump', async () => {
    const { container } = render(<SharePanel code="ABCDE" />)

    const hidden = revealSlot(container).className
    await userEvent.click(screen.getByRole('button', { name: /Afficher le code/i }))
    const shown = revealSlot(container).className

    expect(hidden).toContain('h-40 w-80')
    expect(shown).toContain('h-40 w-80')
  })

  it('offers the link without revealing anything', () => {
    const { container } = render(<SharePanel code="ABCDE" />)

    expect(screen.getByRole('button', { name: /Copier le lien/i })).toBeInTheDocument()
    expect(revealSlot(container)).toHaveAttribute('aria-hidden', 'true')
  })
})
