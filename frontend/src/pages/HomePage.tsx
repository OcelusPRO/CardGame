import { motion } from 'motion/react'
import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { useSession } from '../session/useSession'

/** The front door: what the game is, and the two ways in. */
export function HomePage() {
  const { me } = useSession()

  return (
    <div className="mx-auto flex max-w-3xl flex-col items-center gap-10 px-4 py-14 text-center">
      <motion.h1
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="font-display text-5xl font-extrabold leading-[0.95] text-balance sm:text-7xl"
      >
        Le jeu de cartes
        <span className="block bg-linear-to-r from-punch via-zap to-mint bg-clip-text text-transparent">
          qui dérape
        </span>
      </motion.h1>

      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.15 }}
        className="max-w-xl text-lg text-white/70"
      >
        Une situation, des réponses, et le mauvais goût de vos amis. Créez une partie, partagez le code,
        et laissez le QR code faire le reste.
      </motion.p>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.25 }}
        className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row"
      >
        <Link to="/create" className="sm:w-auto">
          <Button full>Créer une partie</Button>
        </Link>
        <Link to="/join" className="sm:w-auto">
          <Button variant="ghost" full>
            Rejoindre avec un code
          </Button>
        </Link>
      </motion.div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Feature emoji="🎭" title="Mode sans limites" text="Pas de cartes réponses : chacun écrit la sienne." />
        <Feature emoji="✏️" title="Cartes maison" text="Composez votre paquet et gardez-le dans le navigateur." />
        <Feature emoji="🗳️" title="Vote ou maître du jeu" text="Chaque vote rapporte, la majorité rapporte plus." />
      </div>

      {me?.isAdmin && (
        <Link to="/admin" className="text-sm font-semibold text-white/50 underline">
          Espace administration
        </Link>
      )}
    </div>
  )
}

function Feature({ emoji, title, text }: { emoji: string; title: string; text: string }) {
  return (
    <div className="rounded-3xl bg-white/5 p-5 text-left ring-1 ring-white/10">
      <span className="text-2xl">{emoji}</span>
      <h2 className="mt-2 font-display text-lg font-bold">{title}</h2>
      <p className="mt-1 text-sm text-white/60">{text}</p>
    </div>
  )
}
