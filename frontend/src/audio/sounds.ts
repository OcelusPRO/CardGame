/**
 * The sound catalogue. Every effect is described, not recorded: a handful of oscillator
 * and noise "voices" the engine renders on the fly. That keeps the whole soundtrack at a
 * few hundred bytes, silences the licensing question, and lets a sound be retuned by
 * changing a number instead of hunting for another sample.
 *
 * A real recording can take over at any time — see [SAMPLES] at the bottom.
 */

/** A plain oscillator, or `noise` for a burst of white noise squeezed through a bandpass. */
export type Wave = OscillatorType | 'noise'

export interface Voice {
  wave: Wave
  /** Pitch in Hz — the centre frequency of the filter when the wave is `noise`. */
  freq: number
  /** Pitch reached at the end of the voice. Absent means a steady note. */
  slide?: number
  /** Delay before the voice starts, in seconds, counted from the trigger. */
  at?: number
  /** Length in seconds. */
  dur: number
  /** Peak level, 0 to 1, before the master volume. */
  gain: number
  /** Fade-in, in seconds. The 4 ms default is what stops a hard start from clicking. */
  attack?: number
  /** Bandpass sharpness, `noise` only. Higher is more whistle, lower is more breath. */
  q?: number
}

export interface Sound {
  voices: Voice[]
  /** Shortest gap between two plays, in ms. Guards against a sound machine-gunning. */
  throttleMs?: number
}

const CATALOGUE = {
  /** Any button in the app. Dry and short — it is heard hundreds of times a game. */
  click: {
    throttleMs: 40,
    voices: [
      { wave: 'noise', freq: 2100, q: 1.1, dur: 0.04, gain: 0.14, attack: 0.001 },
      { wave: 'triangle', freq: 640, slide: 470, dur: 0.07, gain: 0.09 },
    ],
  },

  /** Picking a card up: a pop that goes up, because something was gained. */
  cardSelect: {
    throttleMs: 60,
    voices: [
      { wave: 'sine', freq: 430, slide: 780, dur: 0.13, gain: 0.16 },
      { wave: 'noise', freq: 3000, q: 0.9, dur: 0.05, gain: 0.06, attack: 0.001 },
    ],
  },

  /** Putting it back down: the same pop, played the other way round. */
  cardDeselect: {
    throttleMs: 60,
    voices: [{ wave: 'sine', freq: 700, slide: 360, dur: 0.12, gain: 0.13 }],
  },

  /** Cards flying into the hand at the top of a round. */
  deal: {
    throttleMs: 70,
    voices: [{ wave: 'noise', freq: 1500, slide: 420, q: 0.7, dur: 0.2, gain: 0.1, attack: 0.02 }],
  },

  /** The moment answers hit the table and the judging opens. */
  voteOpen: {
    voices: [
      { wave: 'triangle', freq: 392, dur: 0.16, gain: 0.12 },
      { wave: 'triangle', freq: 587, at: 0.09, dur: 0.2, gain: 0.12 },
    ],
  },

  /** A vote leaving: two notes, the second landing like a stamp. */
  vote: {
    throttleMs: 120,
    voices: [
      { wave: 'sine', freq: 587, dur: 0.09, gain: 0.16 },
      { wave: 'sine', freq: 880, at: 0.075, dur: 0.16, gain: 0.16 },
      { wave: 'noise', freq: 2600, q: 1.4, dur: 0.04, gain: 0.05, at: 0.075, attack: 0.001 },
    ],
  },

  /** Votes are in, the reveal begins: a rising chord under a drum-roll of noise. */
  voteEnd: {
    voices: [
      { wave: 'noise', freq: 900, slide: 2400, q: 0.6, dur: 0.32, gain: 0.07, attack: 0.2 },
      { wave: 'triangle', freq: 523, at: 0.3, dur: 0.18, gain: 0.14 },
      { wave: 'triangle', freq: 659, at: 0.4, dur: 0.18, gain: 0.14 },
      { wave: 'triangle', freq: 784, at: 0.5, dur: 0.34, gain: 0.15 },
    ],
  },

  /** Your answer won the round. */
  win: {
    voices: [
      { wave: 'triangle', freq: 523, dur: 0.14, gain: 0.15 },
      { wave: 'triangle', freq: 784, at: 0.12, dur: 0.14, gain: 0.15 },
      { wave: 'triangle', freq: 1047, at: 0.24, dur: 0.45, gain: 0.16 },
      { wave: 'sine', freq: 1568, at: 0.24, dur: 0.45, gain: 0.05 },
    ],
  },

  /** The last screen of the game. */
  fanfare: {
    voices: [
      { wave: 'triangle', freq: 392, dur: 0.16, gain: 0.14 },
      { wave: 'triangle', freq: 523, at: 0.14, dur: 0.16, gain: 0.14 },
      { wave: 'triangle', freq: 659, at: 0.28, dur: 0.16, gain: 0.14 },
      { wave: 'triangle', freq: 784, at: 0.42, dur: 0.6, gain: 0.16 },
      { wave: 'sine', freq: 1047, at: 0.42, dur: 0.6, gain: 0.07 },
      { wave: 'noise', freq: 4000, q: 0.5, dur: 0.5, gain: 0.05, at: 0.42, attack: 0.02 },
    ],
  },

  /** Somebody sat down at the table. */
  join: {
    throttleMs: 200,
    voices: [
      { wave: 'sine', freq: 523, dur: 0.1, gain: 0.11 },
      { wave: 'sine', freq: 784, at: 0.08, dur: 0.16, gain: 0.11 },
    ],
  },

  /**
   * One second closer to the buzzer. Deliberately thin: it is played five times in a
   * row and the engine pitches it up as the deadline nears.
   */
  tick: {
    throttleMs: 200,
    voices: [{ wave: 'square', freq: 980, dur: 0.035, gain: 0.055, attack: 0.002 }],
  },

  /** Time is up. */
  timeUp: {
    voices: [
      { wave: 'sawtooth', freq: 240, slide: 150, dur: 0.34, gain: 0.11, attack: 0.008 },
      { wave: 'square', freq: 120, slide: 75, dur: 0.34, gain: 0.06, attack: 0.008 },
    ],
  },

  /** A refused action, played under the toast. */
  error: {
    throttleMs: 300,
    voices: [
      { wave: 'square', freq: 320, dur: 0.09, gain: 0.08 },
      { wave: 'square', freq: 220, at: 0.09, dur: 0.18, gain: 0.08 },
    ],
  },
} satisfies Record<string, Sound>

export type SoundName = keyof typeof CATALOGUE

/** The catalogue, widened: the literal above is only there to name every effect. */
export const SOUNDS: Record<SoundName, Sound> = CATALOGUE

/**
 * Recordings that replace the synthesised version, name to public URL. Drop a file in
 * `frontend/public/sounds/` — a CC0 sample from Freesound, Pixabay, Kenney or
 * OpenGameArt does the job — and list it here; the engine fetches it once and plays it
 * from then on. Anything left out keeps its synthesised voice, and a file that fails to
 * load falls back to it too, so a missing sample is never a silent app.
 */
export const SAMPLES: Partial<Record<SoundName, string>> = {
  // click: '/sounds/click.mp3',
  // deal: '/sounds/deal.mp3',
}
