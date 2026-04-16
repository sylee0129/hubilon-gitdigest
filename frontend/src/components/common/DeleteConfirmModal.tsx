import { useEffect } from 'react'
import styles from './DeleteConfirmModal.module.css'

interface Props {
  title: string
  message: string
  onConfirm: () => void
  onCancel: () => void
  isPending?: boolean
}

export default function DeleteConfirmModal({ title, message, onConfirm, onCancel, isPending }: Props) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onCancel])

  return (
    <div className={styles.overlay} onClick={onCancel}>
      <div className={styles.dialog} onClick={(e) => e.stopPropagation()}>
        <div className={styles.title}>{title}</div>
        <div className={styles.message}>{message}</div>
        <div className={styles.actions}>
          <button className={styles.cancelBtn} onClick={onCancel}>
            취소
          </button>
          <button className={styles.deleteBtn} onClick={onConfirm} disabled={isPending}>
            삭제
          </button>
        </div>
      </div>
    </div>
  )
}
