import { useState, useEffect, FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import authApi from '../services/authApi'
import { teamApi, type Team } from '../services/teamApi'
import styles from './LoginPage.module.css'

export default function SignupPage() {
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [teamId, setTeamId] = useState<number | ''>('')
  const [teams, setTeams] = useState<Team[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    teamApi.getTeams()
      .then(setTeams)
      .catch(() => setTeams([]))
  }, [])

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (teamId === '') return
    setError(null)
    setLoading(true)

    try {
      await authApi.register({ name, email, password, teamId })
      navigate('/login', { state: { message: '회원가입이 완료됐습니다. 로그인해 주세요.' } })
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h1 className={styles.title}>
            Hubilon <span className={styles.titleAccent}>GitDigest</span>
          </h1>
          <p className={styles.subtitle}>새 계정을 만드세요</p>
        </div>

        <form className={styles.form} onSubmit={(e) => void handleSubmit(e)}>
          <div className={styles.fieldGroup}>
            <label className={styles.label} htmlFor="name">이름</label>
            <input
              id="name"
              type="text"
              className={styles.input}
              placeholder="홍길동"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoComplete="name"
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label} htmlFor="email">이메일</label>
            <input
              id="email"
              type="email"
              className={styles.input}
              placeholder="example@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label} htmlFor="password">비밀번호</label>
            <input
              id="password"
              type="password"
              className={styles.input}
              placeholder="비밀번호 입력"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="new-password"
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label} htmlFor="teamId">팀</label>
            <select
              id="teamId"
              className={styles.input}
              value={teamId}
              onChange={(e) => setTeamId(Number(e.target.value))}
              required
            >
              <option value="">팀 선택</option>
              {teams.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </div>

          {error && <p className={styles.errorMsg}>{error}</p>}

          <button
            type="submit"
            className={styles.submitBtn}
            disabled={loading}
          >
            {loading ? '가입 중...' : '회원가입'}
          </button>

          <p className={styles.signupLink}>
            이미 계정이 있으신가요? <Link to="/login">로그인</Link>
          </p>
        </form>
      </div>
    </div>
  )
}
