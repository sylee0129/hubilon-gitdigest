import { useState, type FormEvent } from 'react'
import { useCreateCategory, useUpdateCategory } from '../../hooks/useCategories'
import type { Category } from '../../types/folder'
import styles from './CategoryModal.module.css'

interface CategoryModalProps {
  isOpen: boolean
  onClose: () => void
  initialCategory?: Category
}

export default function CategoryModal({ isOpen, onClose, initialCategory }: CategoryModalProps) {
  const isEdit = initialCategory !== undefined
  const [name, setName] = useState(initialCategory?.name ?? '')

  const createCategory = useCreateCategory()
  const updateCategory = useUpdateCategory()

  const isPending = createCategory.isPending || updateCategory.isPending

  if (!isOpen) return null

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return

    if (isEdit) {
      updateCategory.mutate(
        { id: initialCategory.id, name: name.trim() },
        { onSuccess: onClose, onError: (err) => console.error(err) }
      )
    } else {
      createCategory.mutate(name.trim(), {
        onSuccess: onClose,
        onError: (err) => console.error(err),
      })
    }
  }

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h2 className={styles.title}>{isEdit ? '카테고리 수정' : '카테고리 추가'}</h2>
          <button className={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="categoryName">카테고리명</label>
            <input
              id="categoryName"
              type="text"
              className={styles.input}
              placeholder="카테고리명을 입력하세요"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
            />
          </div>

          {(createCategory.isError || updateCategory.isError) && (
            <div className={styles.errorMsg}>
              {((createCategory.error ?? updateCategory.error) instanceof Error
                ? (createCategory.error ?? updateCategory.error) as Error
                : null)?.message ?? '저장에 실패했습니다.'}
            </div>
          )}

          <div className={styles.actions}>
            <button type="button" className={styles.cancelBtn} onClick={onClose}>
              취소
            </button>
            <button type="submit" className={styles.submitBtn} disabled={isPending}>
              {isPending ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
