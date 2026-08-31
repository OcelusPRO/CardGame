import { useCallback, useEffect, useState } from 'react'
import { adminApi } from '../../api/admin'
import type { CardAdminView, PackAdminView, PackInput } from '../../api/adminTypes'

/** Loads and mutates the catalogue, reloading only the list that actually changed. */
export function useAdminCatalog() {
  const [packs, setPacks] = useState<PackAdminView[]>([])
  const [situations, setSituations] = useState<CardAdminView[]>([])
  const [punchlines, setPunchlines] = useState<CardAdminView[]>([])

  const reload = useCallback(async () => {
    const [nextPacks, nextSituations, nextPunchlines] = await Promise.all([
      adminApi.packs(),
      adminApi.situations(),
      adminApi.punchlines(),
    ])
    setPacks(nextPacks)
    setSituations(nextSituations)
    setPunchlines(nextPunchlines)
  }, [])

  useEffect(() => {
    reload().catch(() => undefined)
  }, [reload])

  return {
    packs,
    situations,
    punchlines,
    savePack: async (input: Omit<PackInput, 'enabled'> & { enabled?: boolean }) => {
      await adminApi.savePack({ enabled: true, ...input })
      await reload()
    },
    deletePack: async (id: string) => {
      await adminApi.deletePack(id)
      await reload()
    },
    saveSituation: async (packId: string, text: string, id?: string) => {
      await adminApi.saveSituation({ id, packId, text, enabled: true })
      await reload()
    },
    deleteSituation: async (id: string) => {
      await adminApi.deleteSituation(id)
      await reload()
    },
    savePunchline: async (packId: string, text: string, id?: string) => {
      await adminApi.savePunchline({ id, packId, text, enabled: true })
      await reload()
    },
    deletePunchline: async (id: string) => {
      await adminApi.deletePunchline(id)
      await reload()
    },
  }
}
