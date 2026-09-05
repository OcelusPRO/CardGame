import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { DailyActivityView } from '../../api/adminTypes'

interface Props {
  data: DailyActivityView[]
}

/*
 * Recharts draws into SVG attributes rather than classes, so the palette has to be
 * handed to it explicitly. `color-mix` against `--color-ink` is what the `text-ink/45`
 * utilities elsewhere compile to, which is why the grid and the ticks turn with the
 * theme instead of assuming a dark page the way they used to.
 */
const GRID = 'color-mix(in oklab, var(--color-ink) 10%, transparent)'
const TICK = 'color-mix(in oklab, var(--color-ink) 55%, transparent)'

/** Games opened and rounds played, day by day. */
export function ActivityChart({ data }: Props) {
  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -20 }}>
          <defs>
            <linearGradient id="gamesFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--color-punch)" stopOpacity={0.7} />
              <stop offset="100%" stopColor="var(--color-punch)" stopOpacity={0} />
            </linearGradient>
            <linearGradient id="roundsFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--color-mint)" stopOpacity={0.7} />
              <stop offset="100%" stopColor="var(--color-mint)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="day" tick={{ fill: TICK, fontSize: 11 }} tickLine={false} />
          <YAxis tick={{ fill: TICK, fontSize: 11 }} tickLine={false} axisLine={false} />
          <Tooltip
            contentStyle={{
              background: 'var(--color-paper)',
              border: `1px solid ${GRID}`,
              color: 'var(--color-ink)',
              borderRadius: 16,
            }}
          />
          <Area
            type="monotone"
            dataKey="gamesCreated"
            name="Parties créées"
            stroke="var(--color-punch)"
            fill="url(#gamesFill)"
            strokeWidth={2}
          />
          <Area
            type="monotone"
            dataKey="roundsPlayed"
            name="Manches jouées"
            stroke="var(--color-mint)"
            fill="url(#roundsFill)"
            strokeWidth={2}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
