import { useState } from 'react'
import { useTeamConfigs, useDeleteTeamConfig } from './hooks/useConfluenceConfigs'
import DeleteConfirmModal from '../../../components/common/DeleteConfirmModal'
import type { TeamConfig } from '../../../services/confluenceAdminApi'
import styles from './ConfluenceAdminPage.module.css'

export default function TeamConfigTable() {
  const { data: configs, isLoading, isError } = useTeamConfigs()
  const deleteConfig = useDeleteTeamConfig()
  const [deletingItem, setDeletingItem] = useState<TeamConfig | null>(null)

  const handleConfirmDelete = () => {
    if (!deletingItem) return
    deleteConfig.mutate(deletingItem.teamId, {
      onSuccess: () => setDeletingItem(null),
    })
  }

  if (isLoading) {
    return <div className={styles.stateMsg}>불러오는 중...</div>
  }

  if (isError) {
    return <div className={styles.errorMsg}>설정 목록을 불러오지 못했습니다.</div>
  }

  if (!configs || configs.length === 0) {
    return <div className={styles.stateMsg}>등록된 팀별 설정이 없습니다.</div>
  }

  return (
    <>
      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>팀</th>
              <th>Parent Page ID</th>
              <th>수정자</th>
              <th>수정일</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {configs.map((cfg) => (
              <tr key={cfg.id}>
                <td>{cfg.teamName}</td>
                <td>
                  <code className={styles.codeText}>{cfg.parentPageId}</code>
                </td>
                <td>{cfg.updatedBy}</td>
                <td className={styles.dateCell}>{cfg.updatedAt?.slice(0, 10)}</td>
                <td>
                  <button
                    className={styles.deleteRowBtn}
                    onClick={() => setDeletingItem(cfg)}
                    title="삭제"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {deletingItem && (
        <DeleteConfirmModal
          title="팀별 설정 삭제"
          message={`'${deletingItem.teamName}' 팀의 Confluence 설정을 삭제하시겠습니까?`}
          isPending={deleteConfig.isPending}
          onConfirm={handleConfirmDelete}
          onCancel={() => setDeletingItem(null)}
        />
      )}
    </>
  )
}
