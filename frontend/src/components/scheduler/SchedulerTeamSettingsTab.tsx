import { useSchedulerTeamConfigs, useUpdateSchedulerTeamConfig } from '../../hooks/useSchedulerTeamConfigs'
import styles from './SchedulerTeamSettingsTab.module.css'

export default function SchedulerTeamSettingsTab() {
  const { data: configs, isLoading, isError } = useSchedulerTeamConfigs()
  const updateConfig = useUpdateSchedulerTeamConfig()

  const handleToggle = (teamId: number, currentEnabled: boolean) => {
    updateConfig.mutate({ teamId, enabled: !currentEnabled })
  }

  if (isLoading) {
    return <div className={styles.stateMsg}>불러오는 중...</div>
  }

  if (isError) {
    return <div className={styles.errorMsg}>팀 설정을 불러오지 못했습니다.</div>
  }

  if (!configs || configs.length === 0) {
    return <div className={styles.stateMsg}>등록된 팀이 없습니다.</div>
  }

  return (
    <div className={styles.tableWrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>팀명</th>
            <th>스케줄러 상태</th>
          </tr>
        </thead>
        <tbody>
          {configs.map((cfg) => (
            <tr key={cfg.teamId}>
              <td>{cfg.teamName}</td>
              <td>
                <button
                  role="switch"
                  aria-checked={cfg.enabled}
                  className={`${styles.toggle} ${cfg.enabled ? styles.toggleOn : styles.toggleOff}`}
                  onClick={() => handleToggle(cfg.teamId, cfg.enabled)}
                  disabled={updateConfig.isPending}
                >
                  <span className={styles.toggleThumb} />
                  <span className={styles.toggleLabel}>{cfg.enabled ? '활성' : '비활성'}</span>
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
