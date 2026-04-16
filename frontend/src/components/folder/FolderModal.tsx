import { useState, useRef, useEffect, type FormEvent } from 'react'
import { useCreateFolder, useUpdateFolder } from '../../hooks/useFolders'
import { useUsers } from '../../hooks/useUsers'
import type { Folder, FolderMember, User } from '../../types/folder'
import { CATEGORY_LABELS, STATUS_LABELS } from '../../types/folder'
import styles from './FolderModal.module.css'

interface FolderModalProps {
  onClose: () => void
  folder?: Folder
}

export default function FolderModal({ onClose, folder }: FolderModalProps) {
  const isEdit = folder !== undefined

  const [name, setName] = useState(folder?.name ?? '')
  const [category, setCategory] = useState<Folder['category']>(folder?.category ?? 'DEVELOPMENT')
  const [status, setStatus] = useState<Folder['status']>(folder?.status ?? 'IN_PROGRESS')
  const [members, setMembers] = useState<FolderMember[]>(folder?.members ?? [])
  const [searchQuery, setSearchQuery] = useState('')
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  const searchRef = useRef<HTMLInputElement>(null)
  const dropdownRef = useRef<HTMLDivElement>(null)

  const debouncedQuery = useDebounce(searchQuery, 300)
  const enableSearch = debouncedQuery.length >= 2
  const { data: searchResults } = useUsers(enableSearch ? debouncedQuery : undefined)

  const createFolder = useCreateFolder()
  const updateFolder = useUpdateFolder()

  const isPending = createFolder.isPending || updateFolder.isPending

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node) &&
        searchRef.current &&
        !searchRef.current.contains(e.target as Node)
      ) {
        setDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value)
    setDropdownOpen(true)
    setHighlightedIndex(-1)
  }

  const handleSelectUser = (user: User) => {
    if (members.some((m) => m.id === user.id)) return
    setMembers((prev) => [...prev, { id: user.id, name: user.name, teamName: user.teamName }])
    setSearchQuery('')
    setDropdownOpen(false)
    setHighlightedIndex(-1)
  }

  const filteredResults = searchResults?.filter((u) => !members.some((m) => m.id === u.id)) ?? []

  const handleSearchKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      if (highlightedIndex >= 0 && highlightedIndex < filteredResults.length) {
        handleSelectUser(filteredResults[highlightedIndex])
      }
      return
    }
    if (!dropdownOpen || filteredResults.length === 0) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlightedIndex((prev) => Math.min(prev + 1, filteredResults.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlightedIndex((prev) => Math.max(prev - 1, 0))
    } else if (e.key === 'Escape') {
      setDropdownOpen(false)
      setHighlightedIndex(-1)
    }
  }

  const handleRemoveMember = (id: number) => {
    setMembers((prev) => prev.filter((m) => m.id !== id))
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    const payload = {
      name,
      category,
      status,
      memberIds: members.map((m) => m.id),
    }

    if (isEdit) {
      updateFolder.mutate(
        { id: folder.id, payload },
        { onSuccess: onClose, onError: (err) => console.error(err) }
      )
    } else {
      createFolder.mutate(payload, {
        onSuccess: onClose,
        onError: (err) => console.error(err),
      })
    }
  }

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h2 className={styles.title}>{isEdit ? '폴더 수정' : '폴더 추가'}</h2>
          <button className={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {/* 구분 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="category">구분</label>
            <select
              id="category"
              className={styles.select}
              value={category}
              onChange={(e) => setCategory(e.target.value as Folder['category'])}
            >
              {(Object.keys(CATEGORY_LABELS) as Folder['category'][]).map((key) => (
                <option key={key} value={key}>{CATEGORY_LABELS[key]}</option>
              ))}
            </select>
          </div>

          {/* 프로젝트명 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="folderName">프로젝트명</label>
            <input
              id="folderName"
              type="text"
              className={styles.input}
              placeholder="프로젝트명을 입력하세요"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          {/* 담당자 */}
          <div className={styles.field}>
            <span className={styles.label}>담당자</span>
            <div className={styles.chipContainer}>
              {members.map((m) => (
                <span key={m.id} className={styles.chip}>
                  {m.name}
                  <button
                    type="button"
                    className={styles.chipRemove}
                    onClick={() => handleRemoveMember(m.id)}
                  >
                    ×
                  </button>
                </span>
              ))}
              <div className={styles.searchWrapper}>
                <input
                  ref={searchRef}
                  type="text"
                  className={styles.searchInput}
                  placeholder="담당자 검색 (2글자 이상)"
                  value={searchQuery}
                  onChange={handleSearchChange}
                  onFocus={() => searchQuery.length >= 2 && setDropdownOpen(true)}
                  onKeyDown={handleSearchKeyDown}
                />
                {dropdownOpen && enableSearch && (
                  <div ref={dropdownRef} className={styles.dropdown}>
                    {filteredResults.length === 0 ? (
                      <div className={styles.dropdownEmpty}>검색 결과가 없습니다.</div>
                    ) : (
                      filteredResults.map((user, idx) => (
                        <button
                          key={user.id}
                          type="button"
                          className={`${styles.dropdownItem}${idx === highlightedIndex ? ` ${styles.dropdownItemHighlighted}` : ''}`}
                          onClick={() => handleSelectUser(user)}
                        >
                          <span className={styles.dropdownName}>{user.name}</span>
                          {user.teamName && (
                            <span className={styles.dropdownDept}>{user.teamName}</span>
                          )}
                        </button>
                      ))
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* 상태 */}
          <div className={styles.field}>
            <span className={styles.label}>상태</span>
            <div className={styles.radioGroup}>
              {(Object.keys(STATUS_LABELS) as Folder['status'][]).map((key) => (
                <label key={key} className={styles.radioLabel}>
                  <input
                    type="radio"
                    name="status"
                    value={key}
                    checked={status === key}
                    onChange={() => setStatus(key)}
                  />
                  {STATUS_LABELS[key]}
                </label>
              ))}
            </div>
          </div>

          {/* 에러 */}
          {(createFolder.isError || updateFolder.isError) && (
            <div className={styles.errorMsg}>
              {((createFolder.error ?? updateFolder.error) instanceof Error
                ? (createFolder.error ?? updateFolder.error) as Error
                : null)?.message ?? '저장에 실패했습니다.'}
            </div>
          )}

          {/* 버튼 */}
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

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}
