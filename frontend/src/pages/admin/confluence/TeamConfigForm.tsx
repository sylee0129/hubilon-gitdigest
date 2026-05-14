import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { teamApi } from '../../../services/teamApi'
import { useTeamConfigs, useUpsertTeamConfig } from './hooks/useConfluenceConfigs'
import styles from './ConfluenceAdminPage.module.css'

export default function TeamConfigForm() {
  const [teamId, setTeamId] = useState<number | ''>('')
  const [parentPageId, setParentPageId] = useState('')
  const [pageName, setPageName] = useState('')

  const { data: teams } = useQuery({
    queryKey: ['teams'],
    queryFn: teamApi.getTeams,
  })

  const { data: teamConfigs } = useTeamConfigs()
  const upsert = useUpsertTeamConfig()

  useEffect(() => {
    if (!teamId) {
      setParentPageId('')
      setPageName('')
      return
    }
    const existing = teamConfigs?.find((c) => c.teamId === teamId)
    if (existing) {
      setParentPageId(existing.parentPageId)
      setPageName(existing.pageName ?? '')
    } else {
      setParentPageId('')
      setPageName('')
    }
  }, [teamId, teamConfigs])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!teamId) return
    upsert.mutate(
      { teamId: teamId as number, parentPageId, pageName: pageName || undefined },
      {
        onSuccess: () => {
          setTeamId('')
          setParentPageId('')
          setPageName('')
        },
      }
    )
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formGrid}>
        <div className={styles.formField}>
          <label className={styles.label}>팀 선택 *</label>
          <select
            className={styles.select}
            value={teamId}
            onChange={(e) => setTeamId(e.target.value ? Number(e.target.value) : '')}
            required
          >
            <option value="">팀을 선택하세요</option>
            {teams?.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </div>

        <div className={styles.formField}>
          <label className={styles.label}>주간보고 폴더 ID *</label>
          <input
            className={styles.input}
            type="text"
            value={parentPageId}
            onChange={(e) => setParentPageId(e.target.value)}
            placeholder="Confluence 상위 페이지 ID"
            required
          />
        </div>

        <div className={styles.formField}>
          <label className={styles.label}>하위 페이지 prefix (선택)</label>
          <input
            className={styles.input}
            type="text"
            value={pageName}
            onChange={(e) => setPageName(e.target.value)}
            placeholder="Confluence 페이지명 prefix (예: 플랫폼개발팀)"
          />
        </div>
      </div>

      <div className={styles.formActions}>
        <button
          type="submit"
          className={styles.submitBtn}
          disabled={upsert.isPending}
        >
          {upsert.isPending ? '저장 중...' : '저장'}
        </button>
      </div>
    </form>
  )
}