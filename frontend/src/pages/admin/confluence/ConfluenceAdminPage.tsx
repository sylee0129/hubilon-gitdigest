import { useState } from 'react'
import Header from '../../../components/layout/Header'
import SidebarLayout from '../../../components/layout/SidebarLayout'
import SpaceConfigForm from './SpaceConfigForm'
import SpaceConfigTable from './SpaceConfigTable'
import TeamConfigForm from './TeamConfigForm'
import TeamConfigTable from './TeamConfigTable'
import styles from './ConfluenceAdminPage.module.css'

type Tab = 'space' | 'team'

export default function ConfluenceAdminPage() {
  const [activeTab, setActiveTab] = useState<Tab>('space')

  return (
    <div className={styles.layout}>
      <Header />

      <SidebarLayout>
        <main className={styles.main}>
          <div className={styles.pageHeader}>
            <h1 className={styles.pageTitle}>Confluence 설정</h1>
            <span className={styles.pageBadge}>ADMIN</span>
          </div>

          <div className={styles.tabList}>
            <button
              className={`${styles.tabBtn} ${activeTab === 'space' ? styles.tabActive : ''}`}
              onClick={() => setActiveTab('space')}
            >
              실별 설정
            </button>
            <button
              className={`${styles.tabBtn} ${activeTab === 'team' ? styles.tabActive : ''}`}
              onClick={() => setActiveTab('team')}
            >
              팀별 설정
            </button>
          </div>

          {activeTab === 'space' && (
            <>
              <div className={styles.section}>
                <div className={styles.sectionHeader}>
                  <div className={styles.sectionTitle}>실별 Confluence 연동 설정</div>
                </div>
                <div className={styles.sectionBody}>
                  <SpaceConfigForm />
                </div>
              </div>

              <div className={styles.section}>
                <div className={styles.sectionHeader}>
                  <div className={styles.sectionTitle}>등록된 실별 설정</div>
                </div>
                <div className={styles.sectionBody}>
                  <SpaceConfigTable />
                </div>
              </div>
            </>
          )}

          {activeTab === 'team' && (
            <>
              <div className={styles.section}>
                <div className={styles.sectionHeader}>
                  <div className={styles.sectionTitle}>팀별 Parent Page 설정</div>
                </div>
                <div className={styles.sectionBody}>
                  <TeamConfigForm />
                </div>
              </div>

              <div className={styles.section}>
                <div className={styles.sectionHeader}>
                  <div className={styles.sectionTitle}>등록된 팀별 설정</div>
                </div>
                <div className={styles.sectionBody}>
                  <TeamConfigTable />
                </div>
              </div>
            </>
          )}
        </main>
      </SidebarLayout>
    </div>
  )
}
