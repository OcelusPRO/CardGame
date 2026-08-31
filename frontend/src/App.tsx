import { Suspense, lazy } from 'react'
import { Link, Route, Routes } from 'react-router-dom'
import { CreatePage } from './pages/CreatePage'
import { GamePage } from './pages/GamePage'
import { HomePage } from './pages/HomePage'
import { JoinPage } from './pages/JoinPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { DiscordButton } from './components/ui/DiscordButton'
import { useSession } from './session/useSession'

// The dashboard pulls in the charting library; players never download it.
const AdminPage = lazy(() => import('./pages/AdminPage').then((module) => ({ default: module.AdminPage })))

/** The shell: a thin header, then whichever screen the URL asks for. */
export function App() {
  const { me } = useSession()

  return (
    <div className="min-h-dvh">
      <header className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-4">
        <Link to="/" className="font-display text-xl font-black tracking-tight">
          Sans<span className="text-punch">Filtre</span>
        </Link>
        <DiscordButton me={me} />
      </header>

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/create" element={<CreatePage />} />
        <Route path="/join" element={<JoinPage />} />
        <Route path="/game/:code" element={<GamePage />} />
        <Route
          path="/admin"
          element={
            <Suspense fallback={<p className="p-10 text-center text-white/50">Chargement…</p>}>
              <AdminPage />
            </Suspense>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </div>
  )
}
