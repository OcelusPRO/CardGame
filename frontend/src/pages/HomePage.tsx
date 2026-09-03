import { motion } from 'motion/react'
import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { useSession } from '../session/useSession'
import logo from '../assets/logo.png'

/** The front door: what the game is, and the two ways in. */
export function HomePage() {
  const { me } = useSession()

  return (
    <div className="mx-auto flex max-w-3xl flex-col items-center gap-10 px-4 py-14 text-center">
      <motion.img
        src={logo}
        alt=""
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="-mb-4 w-52 max-w-[68%] drop-shadow-[0_12px_24px_rgba(43,30,63,0.18)] sm:w-64"
      />

      <motion.h1
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="font-display text-5xl font-extrabold leading-[0.95] text-balance sm:text-7xl"
      >
        Le jeu de cartes
        <span className="block bg-linear-to-r from-punch via-grape to-mint bg-clip-text text-transparent">
          qui dérape
        </span>
      </motion.h1>

      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.15 }}
        className="max-w-xl text-lg text-ink/75"
      >
        Une situation, des réponses, et le mauvais goût de vos amis. Créez une partie, envoyez le lien,
        et vous jouez dans la minute.
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
        <Feature emoji="🎭" title="Mode sans limites" text="Pas de cartes à jouer : chacun écrit sa réponse." />
        <Feature emoji="✏️" title="Cartes maison" text="Écrivez vos propres cartes et rejouez-les quand vous voulez." />
        <Feature emoji="🗳️" title="Vote ou maître du jeu" text="Chaque vote rapporte, la majorité rapporte plus." />
      </div>

      {me?.isAdmin && (
        <Link to="/admin" className="text-sm font-semibold text-ink/60 underline">
          Espace administration
        </Link>
      )}
    </div>
  )
}

function Feature({ emoji, title, text }: { emoji: string; title: string; text: string }) {
  return (
    <motion.div
      whileHover={{ y: -4 }}
      whileTap={{ scale: 0.98 }}
      transition={{ type: 'spring', stiffness: 400, damping: 20 }}
      className="sketch sketch-hover bg-paper/70 p-5 text-left shadow-card transition hover:bg-paper/90 hover:shadow-glow"
    >
      <span className="text-2xl">{emoji}</span>
      <h2 className="mt-2 font-display text-lg font-bold">{title}</h2>
      <p className="mt-1 text-sm text-ink/70">{text}</p>
    </motion.div>
  )
}
