import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { DailyActivityView } from '../../api/adminTypes'

interface Props {
  data: DailyActivityView[]
}

/** Games opened and rounds played, day by day. */
export function ActivityChart({ data }: Props) {
  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -20 }}>
          <defs>
            <linearGradient id="gamesFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#ff2e88" stopOpacity={0.7} />
              <stop offset="100%" stopColor="#ff2e88" stopOpacity={0} />
            </linearGradient>
            <linearGradient id="roundsFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#2ee6a8" stopOpacity={0.7} />
              <stop offset="100%" stopColor="#2ee6a8" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
          <XAxis dataKey="day" tick={{ fill: 'rgba(255,255,255,0.45)', fontSize: 11 }} tickLine={false} />
          <YAxis tick={{ fill: 'rgba(255,255,255,0.45)', fontSize: 11 }} tickLine={false} axisLine={false} />
          <Tooltip
            contentStyle={{
              background: '#24103c',
              border: '1px solid rgba(255,255,255,0.15)',
              borderRadius: 16,
            }}
          />
          <Area
            type="monotone"
            dataKey="gamesCreated"
            name="Parties créées"
            stroke="#ff2e88"
            fill="url(#gamesFill)"
            strokeWidth={2}
          />
          <Area
            type="monotone"
            dataKey="roundsPlayed"
            name="Manches jouées"
            stroke="#2ee6a8"
            fill="url(#roundsFill)"
            strokeWidth={2}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
