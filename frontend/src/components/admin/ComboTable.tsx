import type { ComboView } from '../../api/adminTypes'

interface Props {
  combos: ComboView[]
}

/** Which answer really lands on which situation, ranked by votes per play. */
export function ComboTable({ combos }: Props) {
  if (combos.length === 0) {
    return <p className="text-sm text-ink/60">Pas encore assez de manches jouées pour dégager une tendance.</p>
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[40rem] border-collapse text-sm">
        <thead>
          <tr className="text-left text-xs uppercase tracking-wider text-ink/55">
            <th className="p-2">Situation</th>
            <th className="p-2">Réponse</th>
            <th className="p-2 text-right">Jouée</th>
            <th className="p-2 text-right">Votes</th>
            <th className="p-2 text-right">Ratio</th>
          </tr>
        </thead>
        <tbody>
          {combos.map((combo) => (
            <tr key={`${combo.situationId}-${combo.punchlineId}`} className="border-t border-ink/15">
              <td className="p-2 text-ink/75">{combo.situationText}</td>
              <td className="p-2 font-semibold">{combo.punchlineText}</td>
              <td className="p-2 text-right tabular-nums">{combo.plays}</td>
              <td className="p-2 text-right tabular-nums">{combo.votes}</td>
              <td className="p-2 text-right font-bold tabular-nums text-honey">{combo.voteRatio.toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
