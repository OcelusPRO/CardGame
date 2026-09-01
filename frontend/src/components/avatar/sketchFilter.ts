/**
 * The wobble every avatar is drawn through.
 *
 * `feTurbulence` makes a noise field and `feDisplacementMap` pushes each pixel of the
 * shape along it, which bends the clean vector outlines the way a hand never draws a
 * perfect circle. The seed comes from the avatar itself so a player's face wobbles the
 * same way on every screen and every render — random here would shimmer on each repaint.
 */
export function sketchSeed(...parts: string[]): number {
  let hash = 0
  for (const value of parts.join('|')) {
    hash = (hash * 31 + value.charCodeAt(0)) % 997
  }
  return hash
}
