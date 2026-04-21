import { useState } from 'react'
import { useSpaceConfigs, useDeleteSpaceConfig } from './hooks/useConfluenceConfigs'
import DeleteConfirmModal from '../../../components/common/DeleteConfirmModal'
import type { SpaceConfig } from '../../../services/confluenceAdminApi'
import styles from './ConfluenceAdminPage.module.css'

export default function SpaceConfigTable() {
  const { data: configs, isLoading, isError } = useSpaceConfigs()
  const deleteConfig = useDeleteSpaceConfig()
  const [deletingItem, setDeletingItem] = useState<SpaceConfig | null>(null)

  const handleConfirmDelete = () => {
    if (!deletingItem) return
    deleteConfig.mutate(deletingItem.deptId, {
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
    return <div className={styles.stateMsg}>등록된 실별 설정이 없습니다.</div>
  }

  return (
    <>
      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>실</th>
              <th>이메일</th>
              <th>API 토큰</th>
              <th>Space Key</th>
              <th>Base URL</th>
              <th>수정자</th>
              <th>수정일</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {configs.map((cfg) => (
              <tr key={cfg.id}>
                <td>{cfg.deptName}</td>
                <td>{cfg.userEmail}</td>
                <td>
                  <span className={styles.maskedToken}>***</span>
                </td>
                <td>
                  <code className={styles.codeText}>{cfg.spaceKey}</code>
                </td>
                <td className={styles.urlCell}>
                  <a href={cfg.baseUrl} target="_blank" rel="noopener noreferrer" className={styles.urlLink}>
                    {cfg.baseUrl}
                  </a>
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
          title="실별 설정 삭제"
          message={`'${deletingItem.deptName}' 실의 Confluence 설정을 삭제하시겠습니까?`}
          isPending={deleteConfig.isPending}
          onConfirm={handleConfirmDelete}
          onCancel={() => setDeletingItem(null)}
        />
      )}
    </>
  )
}
