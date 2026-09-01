import type * as THREE_NS from 'three'

/**
 * One WebGL surface, shared by every card being crumpled.
 *
 * A renderer per card would be simpler to write, but browsers cap the number of live
 * WebGL contexts at around sixteen and a full table can lose more cards than that in a
 * single round. So there is one fixed, transparent canvas over the page, one scene, and
 * one render loop; each card adds a sheet to it and takes it away when it has fallen.
 * Nothing is created until the first crumple, and everything is disposed of once the last
 * sheet is gone — an idle table pays for none of this.
 *
 * The camera is set up so that one world unit is one CSS pixel at z = 0: a sheet placed
 * at a card's own screen rectangle covers it exactly, which is what lets the flat sheet
 * take over from the real DOM card without anything appearing to move.
 */

export interface CrumpleRequest {
  /** Where the card is on screen, in viewport coordinates. */
  rect: { left: number; top: number; width: number; height: number }
  /** The card face, already painted. */
  face: HTMLCanvasElement
}

const FOV = 45
const CRUMPLE_MS = 950
/** Segments across the sheet. Few enough that the folds stay big and angular. */
const SEGMENTS_X = 12
const SEGMENTS_Y = 16
/** Pixels per second, in the world's own units — the sheet is thrown, then falls. */
const LAUNCH_SPEED = 420
const GRAVITY = 2900
const DRIFT_SPEED = 110

interface Sheet {
  mesh: THREE_NS.Mesh
  geometry: THREE_NS.BufferGeometry
  material: THREE_NS.MeshStandardMaterial
  texture: THREE_NS.CanvasTexture
  flat: Float32Array
  crumpled: Float32Array
  origin: { x: number; y: number }
  bornAt: number
  folded: boolean
  resolve: () => void
}

interface Stage {
  three: typeof THREE_NS
  renderer: THREE_NS.WebGLRenderer
  scene: THREE_NS.Scene
  camera: THREE_NS.PerspectiveCamera
  canvas: HTMLCanvasElement
  sheets: Sheet[]
  frame: number
  onResize: () => void
}

let stage: Stage | null = null
let starting: Promise<Stage | null> | null = null

/**
 * Crumples one card and throws it away. Resolves once the ball has left the screen, so
 * the caller knows when it may drop the node it was standing in for.
 *
 * Resolves immediately when there is no WebGL to draw on — a card that cannot be animated
 * simply goes, rather than staying on the table forever.
 */
export async function crumpleCard(request: CrumpleRequest): Promise<void> {
  const active = await ensureStage()
  if (!active) return

  return new Promise<void>((resolve) => addSheet(active, request, resolve))
}

async function ensureStage(): Promise<Stage | null> {
  if (stage) return stage
  if (!starting) {
    starting = createStage().finally(() => {
      starting = null
    })
  }
  return starting
}

async function createStage(): Promise<Stage | null> {
  if (typeof window === 'undefined') return null

  try {
    // Three is a heavy import for something a player only meets at the end of a round,
    // so it is fetched the first time a card is actually thrown away.
    const three = await import('three')

    const canvas = document.createElement('canvas')
    canvas.setAttribute('aria-hidden', 'true')
    canvas.style.cssText =
      'position:fixed;inset:0;width:100%;height:100%;pointer-events:none;z-index:45'

    const renderer = new three.WebGLRenderer({ canvas, antialias: true, alpha: true })
    renderer.setClearAlpha(0)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))

    const scene = new three.Scene()
    scene.add(new three.AmbientLight(0xffffff, 1.55))

    const key = new three.DirectionalLight(0xffffff, 2.1)
    key.position.set(220, 340, 480)
    scene.add(key)

    const fill = new three.DirectionalLight(0xc7b8ff, 0.8)
    fill.position.set(-260, -180, 220)
    scene.add(fill)

    const camera = new three.PerspectiveCamera(FOV, 1, 1, 6000)

    document.body.appendChild(canvas)

    const created: Stage = {
      three,
      renderer,
      scene,
      camera,
      canvas,
      sheets: [],
      frame: 0,
      onResize: () => resize(created),
    }
    resize(created)
    // Removed on teardown: a table plays many rounds, and a listener left behind on every
    // one of them would pile up for the whole session.
    window.addEventListener('resize', created.onResize)

    stage = created
    return created
  } catch {
    // No WebGL, or the module failed to load: the caller falls back to no animation.
    return null
  }
}

/** One world unit per CSS pixel at z = 0, whatever the window size. */
function resize(active: Stage): void {
  const width = window.innerWidth
  const height = window.innerHeight

  active.renderer.setSize(width, height, false)
  active.camera.aspect = width / height
  active.camera.position.z = height / 2 / Math.tan((FOV / 2) * (Math.PI / 180))
  active.camera.updateProjectionMatrix()
}

function addSheet(active: Stage, request: CrumpleRequest, resolve: () => void): void {
  const { three } = active
  const { rect, face } = request

  // Non-indexed, so every triangle keeps its own normals and the flat shading gives each
  // facet a distinct tone instead of a smooth gradient.
  const geometry = new three.PlaneGeometry(rect.width, rect.height, SEGMENTS_X, SEGMENTS_Y).toNonIndexed()
  const position = geometry.attributes.position
  const count = position.count

  const flat = new Float32Array(count * 3)
  const crumpled = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) {
    flat[i * 3] = position.getX(i)
    flat[i * 3 + 1] = position.getY(i)
    flat[i * 3 + 2] = position.getZ(i)
  }
  fillCrumpledTarget(flat, crumpled, count, rect.width, rect.height)

  const texture = new three.CanvasTexture(face)
  texture.colorSpace = three.SRGBColorSpace
  texture.anisotropy = 4

  const material = new three.MeshStandardMaterial({
    map: texture,
    side: three.DoubleSide,
    roughness: 0.92,
    metalness: 0.02,
    flatShading: true,
  })

  const mesh = new three.Mesh(geometry, material)
  const origin = {
    x: rect.left + rect.width / 2 - window.innerWidth / 2,
    y: window.innerHeight / 2 - (rect.top + rect.height / 2),
  }
  mesh.position.set(origin.x, origin.y, 0)
  active.scene.add(mesh)

  active.sheets.push({
    mesh,
    geometry,
    material,
    texture,
    flat,
    crumpled,
    origin,
    bornAt: performance.now(),
    folded: false,
    resolve,
  })

  if (!active.frame) active.frame = requestAnimationFrame(() => tick(active))
}

/**
 * Where every vertex of the sheet ends up once it is a ball.
 *
 * The sheet is wrapped onto a sphere, then that sphere is spoilt: one ridge term folds it
 * along regular creases, a second, hashed one pushes whole patches in or out. Without the
 * second the ball comes out as a tidy globe, which reads as a fruit rather than as paper.
 */
function fillCrumpledTarget(
  flat: Float32Array,
  crumpled: Float32Array,
  count: number,
  width: number,
  height: number,
): void {
  const radius = Math.min(width, height) * 0.21

  for (let i = 0; i < count; i++) {
    const u = flat[i * 3] / (width / 2)
    const v = flat[i * 3 + 1] / (height / 2)

    const phi = (u + 1) * Math.PI + Math.sin(v * 4) * 0.4
    const theta = ((v + 1) / 2) * Math.PI + Math.cos(u * 4) * 0.4

    const crease = Math.abs(Math.sin(u * 5 + v * 3)) * radius * 0.42
    const patch = (hash(Math.round(u * 4), Math.round(v * 4)) - 0.5) * radius * 0.58
    const r = radius + crease + patch

    crumpled[i * 3] = r * Math.sin(theta) * Math.cos(phi)
    crumpled[i * 3 + 1] = r * Math.cos(theta)
    crumpled[i * 3 + 2] = r * Math.sin(theta) * Math.sin(phi)
  }
}

/** A deterministic value in [0, 1): the same sheet crumples the same way every time. */
function hash(x: number, y: number): number {
  const value = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453
  return value - Math.floor(value)
}

function tick(active: Stage): void {
  const now = performance.now()
  const bottom = -window.innerHeight / 2 - 400
  const survivors: Sheet[] = []

  for (const sheet of active.sheets) {
    const age = now - sheet.bornAt

    if (age < CRUMPLE_MS) {
      const progress = age / CRUMPLE_MS
      foldSheet(sheet, easeInOutBack(progress))
      sheet.mesh.rotation.set(progress * 1.6, progress * 2.8, progress * 0.9)
      survivors.push(sheet)
      continue
    }

    if (!sheet.folded) {
      foldSheet(sheet, 1)
      sheet.folded = true
    }

    // Thrown up a little, then let go of: the arc is plain projectile motion, which is
    // more convincing here than any easing curve.
    const t = (age - CRUMPLE_MS) / 1000
    sheet.mesh.position.x = sheet.origin.x + DRIFT_SPEED * t
    sheet.mesh.position.y = sheet.origin.y + LAUNCH_SPEED * t - 0.5 * GRAVITY * t * t
    sheet.mesh.rotation.set(1.6 + t * 3.4, 2.8 + t * 4.6, 0.9 + t * 2.2)

    if (sheet.mesh.position.y > bottom) {
      survivors.push(sheet)
    } else {
      dispose(active, sheet)
      sheet.resolve()
    }
  }

  active.sheets = survivors
  active.renderer.render(active.scene, active.camera)

  if (active.sheets.length > 0) {
    active.frame = requestAnimationFrame(() => tick(active))
  } else {
    active.frame = 0
    teardown(active)
  }
}

/** Moves the sheet [amount] of the way from flat to balled up. */
function foldSheet(sheet: Sheet, amount: number): void {
  const position = sheet.geometry.attributes.position
  const count = position.count
  // A shudder that peaks halfway through, so the paper looks fought with rather than
  // smoothly interpolated. It dies away as the ball closes.
  const shudder = Math.sin(Math.min(1, Math.max(0, amount)) * Math.PI)

  for (let i = 0; i < count; i++) {
    const index = i * 3
    const jitter = shudder * ((i % 7) - 3) * 1.4
    position.setXYZ(
      i,
      sheet.flat[index] + (sheet.crumpled[index] - sheet.flat[index]) * amount + jitter,
      sheet.flat[index + 1] + (sheet.crumpled[index + 1] - sheet.flat[index + 1]) * amount + jitter,
      sheet.flat[index + 2] + (sheet.crumpled[index + 2] - sheet.flat[index + 2]) * amount + jitter,
    )
  }

  position.needsUpdate = true
  sheet.geometry.computeVertexNormals()
}

/** Overshoots at both ends: the sheet resists, then gives all at once. */
function easeInOutBack(t: number): number {
  const c1 = 1.70158
  const c2 = c1 * 1.525
  return t < 0.5
    ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
    : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2
}

function dispose(active: Stage, sheet: Sheet): void {
  active.scene.remove(sheet.mesh)
  sheet.geometry.dispose()
  sheet.material.dispose()
  sheet.texture.dispose()
}

function teardown(active: Stage): void {
  if (active.sheets.length > 0) return
  window.removeEventListener('resize', active.onResize)
  active.renderer.dispose()
  active.canvas.remove()
  if (stage === active) stage = null
}
