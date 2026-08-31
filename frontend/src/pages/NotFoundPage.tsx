import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'

/** Nothing here, but at least it stays in character. */
export function NotFoundPage() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-5 px-4 py-24 text-center">
      <p className="font-display text-6xl font-black text-punch">404</p>
      <p className="text-lg text-white/70">Cette page a été jouée puis défaussée.</p>
      <Link to="/">
        <Button variant="ghost">Retour à l'accueil</Button>
      </Link>
    </div>
  )
}
