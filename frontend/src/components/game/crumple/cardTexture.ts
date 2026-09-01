/**
 * Repaints an answer card onto a canvas, so it can be used as the skin of the 3D sheet.
 *
 * Rasterising the real DOM node would be the faithful way to do this, but it costs a
 * screenshot library and a slow synchronous pass at the exact moment the animation has to
 * start. The card is a rectangle of paper with one sentence and a footer on it, so it is
 * cheaper — and smoother — to draw it again here. It only has to hold up for the frame
 * where the flat sheet replaces the card; a second later it is a ball.
 */

export interface CardFace {
  /** The answer itself, the only thing anybody reads on the card. */
  text: string
  /** Who wrote it, once the round is over. */
  author: string
  /** Bottom-right corner, when the votes are in. */
  votes?: number
}

const PAPER = '#fffcf5'
const INK = '#2b1e3f'
const DISPLAY = '"Bricolage Grotesque", "Trebuchet MS", system-ui, sans-serif'
const BODY = '"Outfit", ui-sans-serif, system-ui, sans-serif'

/** Mirrors `punchlineFontSize`: the longer the answer, the smaller it is set. */
function bodySize(text: string, width: number): number {
  const length = text.trim().length
  if (length <= 25) return Math.min(width * 0.1, 30)
  if (length <= 55) return Math.min(width * 0.08, 25)
  if (length <= 100) return Math.min(width * 0.066, 20)
  return Math.min(width * 0.056, 17)
}

/**
 * Draws the face at [scale] device pixels per CSS pixel and returns the canvas, ready to
 * be handed to a texture.
 */
export function drawCardFace(face: CardFace, width: number, height: number, scale: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(width * scale))
  canvas.height = Math.max(1, Math.round(height * scale))

  const context = canvas.getContext('2d')
  if (!context) return canvas
  context.scale(scale, scale)

  context.fillStyle = PAPER
  context.fillRect(0, 0, width, height)

  context.strokeStyle = INK
  context.lineWidth = 2
  context.strokeRect(1, 1, width - 2, height - 2)

  const padding = width * 0.09
  const size = bodySize(face.text, width)
  context.fillStyle = INK
  context.font = `600 ${size}px ${DISPLAY}`
  context.textBaseline = 'top'
  wrap(context, face.text, padding, padding, width - padding * 2, size * 1.25)

  const footerSize = Math.max(9, width * 0.042)
  context.font = `600 ${footerSize}px ${BODY}`
  context.fillStyle = 'rgba(43, 30, 63, 0.6)'
  const footerY = height - padding * 0.7 - footerSize
  context.fillText(face.author, padding, footerY)

  if (face.votes !== undefined) {
    const votes = `${face.votes} vote(s)`
    context.fillText(votes, width - padding - context.measureText(votes).width, footerY)
  }

  return canvas
}

/** Greedy word wrap, the same break-at-spaces rule the real card follows. */
function wrap(
  context: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  lineHeight: number,
): void {
  let line = ''
  let cursor = y

  for (const word of text.split(/\s+/)) {
    const candidate = line ? `${line} ${word}` : word
    if (line && context.measureText(candidate).width > maxWidth) {
      context.fillText(line, x, cursor)
      cursor += lineHeight
      line = word
    } else {
      line = candidate
    }
  }

  if (line) context.fillText(line, x, cursor)
}
