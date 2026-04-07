import { useState, useEffect, useRef, type FormEvent } from 'react'
import { useCreateProject } from '../../hooks/useProjects'
import { oauthApi } from '../../services/oauthApi'
import styles from './AddProjectModal.module.css'

interface Props {
  onClose: () => void
}

interface OAuthMessage {
  type: 'gitlab-oauth' | 'gitlab-oauth-error'
  token?: string
  gitlabUrl?: string
  message?: string
}

export default function AddProjectModal({ onClose }: Props) {
  const [gitlabUrl, setGitlabUrl] = useState('')
  const [gitlabProjectId, setGitlabProjectId] = useState('')
  const [authType, setAuthType] = useState<'PAT' | 'OAUTH'>('PAT')
  const [accessToken, setAccessToken] = useState('')
  const [oauthStatus, setOauthStatus] = useState<'idle' | 'waiting' | 'done' | 'error'>('idle')
  const [oauthError, setOauthError] = useState('')

  const createProject = useCreateProject()
  const popupRef = useRef<Window | null>(null)

  // OAuth 팝업 메시지 수신
  useEffect(() => {
    const handleMessage = (event: MessageEvent<OAuthMessage>) => {
      if (event.origin !== window.location.origin.replace(':3000', ':8080') &&
          event.origin !== 'http://localhost:8080') return

      if (event.data?.type === 'gitlab-oauth' && event.data.token) {
        setOauthStatus('done')
        // 팝업에서 받은 token + gitlabUrl로 바로 프로젝트 등록
        createProject.mutate(
          {
            gitlabUrl: event.data.gitlabUrl ?? gitlabUrl,
            authType: 'OAUTH',
            accessToken: event.data.token,
          },
          { onSuccess: onClose }
        )
      }

      if (event.data?.type === 'gitlab-oauth-error') {
        setOauthStatus('error')
        setOauthError(event.data.message ?? 'OAuth 인증에 실패했습니다.')
        popupRef.current?.close()
      }
    }

    window.addEventListener('message', handleMessage)
    return () => window.removeEventListener('message', handleMessage)
  }, [gitlabUrl, createProject, onClose])

  // 팝업이 사용자에 의해 닫힌 경우 감지
  useEffect(() => {
    if (oauthStatus !== 'waiting') return
    const timer = setInterval(() => {
      if (popupRef.current?.closed) {
        setOauthStatus((prev) => (prev === 'waiting' ? 'idle' : prev))
        clearInterval(timer)
      }
    }, 500)
    return () => clearInterval(timer)
  }, [oauthStatus])

  const handleOAuthLogin = async () => {
    if (!gitlabUrl) return
    try {
      setOauthStatus('waiting')
      setOauthError('')
      const { authUrl } = await oauthApi.getGitLabAuthUrl(gitlabUrl)
      const popup = window.open(authUrl, 'gitlab-oauth', 'width=860,height=640,scrollbars=yes')
      if (!popup) {
        setOauthStatus('error')
        setOauthError('팝업이 차단되었습니다. 브라우저 팝업 차단을 해제해 주세요.')
        return
      }
      popupRef.current = popup
    } catch {
      setOauthStatus('error')
      setOauthError('OAuth URL을 가져오는 데 실패했습니다.')
    }
  }

  const handlePatSubmit = (e: FormEvent) => {
    e.preventDefault()
    createProject.mutate(
      {
        gitlabUrl,
        authType: 'PAT',
        accessToken: accessToken || undefined,
        gitlabProjectId: gitlabProjectId ? Number(gitlabProjectId) : undefined,
      },
      { onSuccess: onClose }
    )
  }

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h2 className={styles.title}>GitLab 프로젝트 추가</h2>
          <button className={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        <form onSubmit={authType === 'PAT' ? handlePatSubmit : (e) => e.preventDefault()} className={styles.form}>
          {/* GitLab URL */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="gitlabUrl">GitLab URL</label>
            <input
              id="gitlabUrl"
              type="url"
              className={styles.input}
              placeholder="https://gitlab.com/your-group/your-repo"
              value={gitlabUrl}
              onChange={(e) => setGitlabUrl(e.target.value)}
              required
            />
          </div>

          {/* GitLab Project ID */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="gitlabProjectId">
              GitLab Project ID
              <span className={styles.optional}> (선택)</span>
            </label>
            <input
              id="gitlabProjectId"
              type="number"
              className={styles.input}
              placeholder="예: 12345678"
              value={gitlabProjectId}
              onChange={(e) => setGitlabProjectId(e.target.value)}
              min={1}
            />
            <p className={styles.fieldHint}>
              GitLab 프로젝트 홈 → 프로젝트명 아래 숫자 ID.
              <br />
              <strong>read_api 스코프 없이 read_repository 토큰으로 연결</strong>할 때 필요합니다.
            </p>
          </div>

          {/* 인증 방식 */}
          <div className={styles.field}>
            <span className={styles.label}>인증 방식</span>
            <div className={styles.radioGroup}>
              <label className={styles.radioLabel}>
                <input
                  type="radio"
                  name="authType"
                  value="PAT"
                  checked={authType === 'PAT'}
                  onChange={() => { setAuthType('PAT'); setOauthStatus('idle') }}
                />
                Personal Access Token (PAT)
              </label>
              <label className={styles.radioLabel}>
                <input
                  type="radio"
                  name="authType"
                  value="OAUTH"
                  checked={authType === 'OAUTH'}
                  onChange={() => { setAuthType('OAUTH'); setOauthStatus('idle') }}
                />
                OAuth (GitLab 로그인)
              </label>
            </div>
          </div>

          {/* PAT 입력 */}
          {authType === 'PAT' && (
            <div className={styles.field}>
              <label className={styles.label} htmlFor="accessToken">Access Token</label>
              <input
                id="accessToken"
                type="password"
                className={styles.input}
                placeholder="glpat-xxxxxxxxxxxxxxxxxxxx"
                value={accessToken}
                onChange={(e) => setAccessToken(e.target.value)}
                required
              />
            </div>
          )}

          {/* OAuth 영역 */}
          {authType === 'OAUTH' && (
            <div className={styles.oauthSection}>
              {oauthStatus === 'idle' && (
                <p className={styles.oauthDesc}>
                  GitLab 계정으로 로그인하여 프로젝트에 접근 권한을 부여합니다.
                </p>
              )}
              {oauthStatus === 'waiting' && (
                <p className={styles.oauthWaiting}>
                  ⏳ GitLab 로그인 팝업에서 인증을 완료해 주세요...
                </p>
              )}
              {oauthStatus === 'done' && (
                <p className={styles.oauthSuccess}>✅ 인증 완료! 프로젝트를 등록하는 중...</p>
              )}
              {oauthStatus === 'error' && (
                <p className={styles.oauthErrorMsg}>❌ {oauthError}</p>
              )}
              {(oauthStatus === 'idle' || oauthStatus === 'error') && (
                <button
                  type="button"
                  className={styles.oauthBtn}
                  onClick={handleOAuthLogin}
                  disabled={!gitlabUrl}
                >
                  GitLab으로 로그인
                </button>
              )}
            </div>
          )}

          {/* 공통 에러 */}
          {createProject.isError && (
            <div className={styles.errorMsg}>
              {createProject.error instanceof Error
                ? createProject.error.message
                : '프로젝트 추가에 실패했습니다.'}
            </div>
          )}

          {/* 버튼 영역 */}
          <div className={styles.actions}>
            <button type="button" className={styles.cancelBtn} onClick={onClose}>
              취소
            </button>
            {authType === 'PAT' && (
              <button
                type="submit"
                className={styles.submitBtn}
                disabled={createProject.isPending}
              >
                {createProject.isPending ? '추가 중...' : '프로젝트 추가'}
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  )
}
