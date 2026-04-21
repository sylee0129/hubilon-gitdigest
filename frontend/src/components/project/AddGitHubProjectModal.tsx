import { useState, useEffect, useRef, type FormEvent } from 'react'
import { useCreateProject } from '../../hooks/useProjects'
import { oauthApi } from '../../services/oauthApi'
import styles from './AddGitHubProjectModal.module.css'

interface Props {
  onClose: () => void
  onSuccess?: () => void
}

interface GitHubOAuthMessage {
  type: 'github-oauth' | 'github-oauth-error'
  token?: string
  message?: string
}

const GITHUB_REPO_PATTERN = /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+$/

export default function AddGitHubProjectModal({ onClose, onSuccess }: Props) {
  const [repoUrl, setRepoUrl] = useState('')
  const [authType, setAuthType] = useState<'PAT' | 'OAUTH'>('PAT')
  const [accessToken, setAccessToken] = useState('')
  const [urlError, setUrlError] = useState('')
  const [oauthStatus, setOauthStatus] = useState<'idle' | 'waiting' | 'done' | 'error'>('idle')
  const [oauthToken, setOauthToken] = useState('')
  const [oauthError, setOauthError] = useState('')
  const [oauthRepoUrl, setOauthRepoUrl] = useState('')
  const [oauthRepoUrlError, setOauthRepoUrlError] = useState('')

  const createProject = useCreateProject()
  const popupRef = useRef<Window | null>(null)


  // 기존 로직을 아래와 같이 수정하세요.
  const getApiOrigin = () => {
    const apiUrl = import.meta.env.VITE_API_URL as string;

    if (apiUrl) {
      try {
        // 절대 경로인 경우 origin 추출
        return new URL(apiUrl).origin;
      } catch {
        // 상대 경로거나 잘못된 형식인 경우 현재 location 사용
        return window.location.origin;
      }
    }

    // 로컬 개발 환경 대응
    return window.location.origin.replace(':3000', ':8080');
  };

  const apiOrigin = getApiOrigin();
  // const apiOrigin = import.meta.env.VITE_API_URL
  //   ? new URL(import.meta.env.VITE_API_URL as string).origin
  //   : window.location.origin.replace(':3000', ':8080')

  useEffect(() => {
    const handleMessage = (event: MessageEvent<GitHubOAuthMessage>) => {
      if (event.origin !== apiOrigin) return

      if (event.data?.type === 'github-oauth' && event.data.token) {
        setOauthStatus('done')
        setOauthToken(event.data.token)
        popupRef.current?.close()
      }

      if (event.data?.type === 'github-oauth-error') {
        setOauthStatus('error')
        setOauthError(event.data.message ?? 'OAuth 인증에 실패했습니다.')
        popupRef.current?.close()
      }
    }

    window.addEventListener('message', handleMessage)
    return () => window.removeEventListener('message', handleMessage)
  }, [apiOrigin])

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

  const validateUrl = (url: string) => {
    if (!url) return '저장소 URL을 입력해 주세요.'
    if (!GITHUB_REPO_PATTERN.test(url)) return 'URL 형식이 올바르지 않습니다. (예: https://github.com/owner/repo)'
    return ''
  }

  const handlePatSubmit = (e: FormEvent) => {
    e.preventDefault()
    const err = validateUrl(repoUrl)
    if (err) { setUrlError(err); return }
    setUrlError('')
    createProject.mutate(
      {
        gitlabUrl: repoUrl,
        authType: 'PAT',
        accessToken: accessToken || undefined,
        gitProvider: 'GITHUB',
      },
      {
        onSuccess: () => {
          onSuccess?.()
          onClose()
        },
      }
    )
  }

  const handleOAuthLogin = async () => {
    try {
      setOauthStatus('waiting')
      setOauthError('')
      const { authUrl } = await oauthApi.getGitHubAuthUrl()
      const popup = window.open(authUrl, 'github-oauth', 'width=860,height=640,scrollbars=yes')
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

  const handleOAuthSubmit = (e: FormEvent) => {
    e.preventDefault()
    const err = validateUrl(oauthRepoUrl)
    if (err) { setOauthRepoUrlError(err); return }
    setOauthRepoUrlError('')
    createProject.mutate(
      {
        gitlabUrl: oauthRepoUrl,
        authType: 'OAUTH',
        accessToken: oauthToken,
        gitProvider: 'GITHUB',
      },
      {
        onSuccess: () => {
          onSuccess?.()
          onClose()
        },
      }
    )
  }

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h2 className={styles.title}>GitHub 프로젝트 추가</h2>
          <button className={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        {/* 인증 방식 탭 */}
        <div className={styles.tabGroup}>
          <button
            type="button"
            className={`${styles.tab} ${authType === 'PAT' ? styles.tabActive : ''}`}
            onClick={() => { setAuthType('PAT'); setOauthStatus('idle') }}
          >
            PAT
          </button>
          <button
            type="button"
            className={`${styles.tab} ${authType === 'OAUTH' ? styles.tabActive : ''}`}
            onClick={() => { setAuthType('OAUTH'); setOauthStatus('idle') }}
          >
            OAuth
          </button>
        </div>

        {/* PAT 폼 */}
        {authType === 'PAT' && (
          <form onSubmit={handlePatSubmit} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="githubRepoUrl">GitHub 저장소 URL</label>
              <input
                id="githubRepoUrl"
                type="url"
                className={`${styles.input} ${urlError ? styles.inputError : ''}`}
                placeholder="https://github.com/owner/repo"
                value={repoUrl}
                onChange={(e) => { setRepoUrl(e.target.value); setUrlError('') }}
                required
              />
              {urlError && <p className={styles.fieldError}>{urlError}</p>}
              <p className={styles.fieldHint}>필요 권한: repo scope (private 저장소 포함)</p>
            </div>

            <div className={styles.field}>
              <label className={styles.label} htmlFor="githubAccessToken">Personal Access Token</label>
              <input
                id="githubAccessToken"
                type="password"
                className={styles.input}
                placeholder="ghp_xxxxxxxxxxxxxxxxxxxx"
                value={accessToken}
                onChange={(e) => setAccessToken(e.target.value)}
                required
              />
            </div>

            {createProject.isError && (
              <div className={styles.errorMsg}>
                {createProject.error instanceof Error
                  ? createProject.error.message
                  : '프로젝트 추가에 실패했습니다.'}
              </div>
            )}

            <div className={styles.actions}>
              <button type="button" className={styles.cancelBtn} onClick={onClose}>
                취소
              </button>
              <button
                type="submit"
                className={styles.submitBtn}
                disabled={createProject.isPending}
              >
                {createProject.isPending ? '추가 중...' : 'GitHub 프로젝트 추가'}
              </button>
            </div>
          </form>
        )}

        {/* OAuth 폼 */}
        {authType === 'OAUTH' && (
          <form onSubmit={handleOAuthSubmit} className={styles.form}>
            <div className={styles.oauthSection}>
              {oauthStatus === 'idle' && (
                <p className={styles.oauthDesc}>
                  GitHub 계정으로 로그인하여 프로젝트에 접근 권한을 부여합니다.
                  <br />
                  필요 권한: repo scope로 앱 승인 필요
                </p>
              )}
              {oauthStatus === 'waiting' && (
                <p className={styles.oauthWaiting}>
                  ⏳ GitHub 로그인 팝업에서 인증을 완료해 주세요...
                </p>
              )}
              {oauthStatus === 'done' && (
                <p className={styles.oauthSuccess}>✅ 인증 완료! 저장소 URL을 입력해 주세요.</p>
              )}
              {oauthStatus === 'error' && (
                <p className={styles.oauthErrorMsg}>❌ {oauthError}</p>
              )}
              {(oauthStatus === 'idle' || oauthStatus === 'error') && (
                <button
                  type="button"
                  className={styles.oauthBtn}
                  onClick={handleOAuthLogin}
                >
                  GitHub OAuth로 연결
                </button>
              )}
            </div>

            {oauthStatus === 'done' && (
              <div className={styles.field}>
                <label className={styles.label} htmlFor="githubOAuthRepoUrl">GitHub 저장소 URL</label>
                <input
                  id="githubOAuthRepoUrl"
                  type="url"
                  className={`${styles.input} ${oauthRepoUrlError ? styles.inputError : ''}`}
                  placeholder="https://github.com/owner/repo"
                  value={oauthRepoUrl}
                  onChange={(e) => { setOauthRepoUrl(e.target.value); setOauthRepoUrlError('') }}
                  required
                />
                {oauthRepoUrlError && <p className={styles.fieldError}>{oauthRepoUrlError}</p>}
              </div>
            )}

            {createProject.isError && (
              <div className={styles.errorMsg}>
                {createProject.error instanceof Error
                  ? createProject.error.message
                  : '프로젝트 추가에 실패했습니다.'}
              </div>
            )}

            <div className={styles.actions}>
              <button type="button" className={styles.cancelBtn} onClick={onClose}>
                취소
              </button>
              {oauthStatus === 'done' && (
                <button
                  type="submit"
                  className={styles.submitBtn}
                  disabled={createProject.isPending}
                >
                  {createProject.isPending ? '추가 중...' : 'GitHub 프로젝트 추가'}
                </button>
              )}
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
