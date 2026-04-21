import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { departmentApi } from '../../../services/departmentApi'
import { useUpsertSpaceConfig, useTestSpaceConnectionDirect } from './hooks/useConfluenceConfigs'
import styles from './ConfluenceAdminPage.module.css'

export default function SpaceConfigForm() {
  const [deptId, setDeptId] = useState<number | ''>('')
  const [userEmail, setUserEmail] = useState('')
  const [apiToken, setApiToken] = useState('')
  const [spaceKey, setSpaceKey] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [testResult, setTestResult] = useState<{ ok: boolean; message: string } | null>(null)

  const { data: departments } = useQuery({
    queryKey: ['departments'],
    queryFn: departmentApi.getAll,
  })

  const upsert = useUpsertSpaceConfig()
  const testConnection = useTestSpaceConnectionDirect()

  // 실 변경 시 테스트 결과 초기화
  useEffect(() => {
    setTestResult(null)
  }, [deptId])

  const handleTest = async () => {
    if (!userEmail || !apiToken || !spaceKey || !baseUrl) return
    setTestResult(null)
    try {
      await testConnection.mutateAsync({ userEmail, apiToken, spaceKey, baseUrl })
      setTestResult({ ok: true, message: '연결 성공' })
    } catch (err) {
      const msg = err instanceof Error ? err.message : '연결 실패'
      setTestResult({ ok: false, message: msg })
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!deptId) return
    upsert.mutate(
      { deptId: deptId as number, userEmail, apiToken, spaceKey, baseUrl },
      {
        onSuccess: () => {
          setUserEmail('')
          setApiToken('')
          setSpaceKey('')
          setBaseUrl('')
          setDeptId('')
          setTestResult(null)
        },
      }
    )
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formGrid}>
        <div className={styles.formField}>
          <label className={styles.label}>실 선택 *</label>
          <select
            className={styles.select}
            value={deptId}
            onChange={(e) => setDeptId(e.target.value ? Number(e.target.value) : '')}
            required
          >
            <option value="">실을 선택하세요</option>
            {departments?.map((dept) => (
              <option key={dept.id} value={dept.id}>
                {dept.name}
              </option>
            ))}
          </select>
        </div>

        <div className={styles.formField}>
          <label className={styles.label}>Confluence 이메일 *</label>
          <input
            className={styles.input}
            type="email"
            value={userEmail}
            onChange={(e) => setUserEmail(e.target.value)}
            placeholder="user@example.com"
            required
          />
        </div>

        <div className={styles.formField}>
          <label className={styles.label}>API 토큰 *</label>
          <input
            className={styles.input}
            type="password"
            value={apiToken}
            onChange={(e) => setApiToken(e.target.value)}
            placeholder="Confluence API Token"
            required
            autoComplete="new-password"
          />
        </div>

        <div className={styles.formField}>
          <label className={styles.label}>Space Key *</label>
          <input
            className={styles.input}
            type="text"
            value={spaceKey}
            onChange={(e) => setSpaceKey(e.target.value)}
            placeholder="~SPACE"
            required
          />
        </div>

        <div className={`${styles.formField} ${styles.formFieldFull}`}>
          <label className={styles.label}>Base URL *</label>
          <input
            className={styles.input}
            type="url"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
            placeholder="https://yourcompany.atlassian.net/wiki"
            required
          />
        </div>
      </div>

      {testResult && (
        <div className={testResult.ok ? styles.testSuccess : styles.testFail}>
          {testResult.ok ? '✓ ' : '✗ '}
          {testResult.message}
        </div>
      )}

      <div className={styles.formActions}>
        <button
          type="button"
          className={styles.testBtn}
          onClick={() => void handleTest()}
          disabled={!userEmail || !apiToken || !spaceKey || !baseUrl || testConnection.isPending}
        >
          {testConnection.isPending ? '테스트 중...' : '연결 테스트'}
        </button>
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
