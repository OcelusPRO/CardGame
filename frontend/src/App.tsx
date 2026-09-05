import { Suspense, lazy, useEffect } from 'react'
import { MotionConfig } from 'motion/react'
import { Link, Route, Routes, useNavigate } from 'react-router-dom'
import { CreatePage } from './pages/CreatePage'
import { GamePage } from './pages/GamePage'
import { HomePage } from './pages/HomePage'
import { JoinPage } from './pages/JoinPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { AnimationToggle } from './components/ui/AnimationToggle'
import { ThemeToggle } from './components/ui/ThemeToggle'
import { SoundToggle } from './components/ui/SoundToggle'
import { AccountMenu } from './components/ui/AccountMenu'
import { useAnimationPref } from './session/useAnimationPref'
import { useThemePref } from './session/useThemePref'
import { useSoundPref } from './audio/useSoundPref'
import { useSession } from './session/useSession'
import { takeReturnPath } from './session/authReturn'
import logo from './assets/logo.png'

// The dashboard pulls in the charting library; players never download it.
const AdminPage = lazy(() => import('./pages/AdminPage').then((module) => ({ default: module.AdminPage })))

/** The shell: a thin header, then whichever screen the URL asks for. */
export function App() {
  const { me } = useSession()
  const navigate = useNavigate()
  const { enabled: animate, toggle: toggleAnimations } = useAnimationPref()
  const { enabled: sound, toggle: toggleSound } = useSoundPref()
  const { dark, toggle: toggleTheme } = useThemePref()

  // Both sign ins land back on the home page. Whoever started from a table is taken
  // straight back to it, and the `?discord=`/`?twitch=` marker is wiped either way.
  useEffect(() => {
    const query = new URLSearchParams(window.location.search)
    if (!query.has('discord') && !query.has('twitch')) return
    navigate(takeReturnPath() ?? window.location.pathname, { replace: true })
  }, [navigate])

  // Kill CSS-driven motion too (the hand-drawn borders re-tracing on hover, and the like)
  // when the switch is off, mirroring the `prefers-reduced-motion` rule in index.css.
  useEffect(() => {
    document.documentElement.dataset.motion = animate ? 'on' : 'off'
  }, [animate])

  // The whole palette hangs off this one attribute: index.css restates every colour
  // token under `html[data-theme='dark']`, so no component has to know which way the
  // page is running. The browser chrome — form controls, scrollbars — follows the
  // `color-scheme` declared alongside them.
  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light'
  }, [dark])

  return (
    <MotionConfig reducedMotion={animate ? 'never' : 'always'}>
      <div className="min-h-dvh">
        <header className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-4">
          <Link to="/" className="flex items-center gap-2 font-display text-xl font-black tracking-tight">
            <img src={logo} alt="" className="h-9 w-auto" />
            Sans<span className="text-punch">Filtres</span>
          </Link>
          <div className="flex items-center gap-2">
            <SoundToggle enabled={sound} onToggle={toggleSound} />
            <ThemeToggle dark={dark} onToggle={toggleTheme} />
            <AnimationToggle enabled={animate} onToggle={toggleAnimations} />
            <AccountMenu me={me} />
          </div>
        </header>

        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/create" element={<CreatePage />} />
          <Route path="/join" element={<JoinPage />} />
          <Route path="/game/:code" element={<GamePage />} />
          <Route path="/game" element={<GamePage />} />
          <Route
            path="/admin"
            element={
              <Suspense fallback={<p className="p-10 text-center text-ink/60">Chargement…</p>}>
                <AdminPage />
              </Suspense>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </div>
    </MotionConfig>
  )
}
