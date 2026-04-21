import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { teamApi } from '../../../services/teamApi'
import { useUpsertTeamConfig } from './hooks/useConfluenceConfigs'
import styles from './ConfluenceAdminPage.module.css'

export default function TeamConfigForm() {
  const [teamId, setTeamId] = useState<number | ''>('')
  const [parentPageId, setParentPageId] = useState('')

  const { data: teams } = useQuery({
    queryKey: ['teams'],
    queryFn: teamApi.getTeams,
  })

  const upsert = useUpsertTeamConfig()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!teamId) return
    upsert.mutate(
      { teamId: teamId as number, parentPageId },
      {
        onSuccess: () => {
          setTeamId('')
          setParentPageId('')
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
          <label className={styles.label}>Parent Page ID *</label>
          <input
            className={styles.input}
            type="text"
            value={parentPageId}
            onChange={(e) => setParentPageId(e.target.value)}
            placeholder="Confluence 상위 페이지 ID"
            required
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
