import { useRef, useEffect, type ReactNode } from 'react'
import Sidebar from './Sidebar'
import { useSidebarStore } from '../../stores/sidebarStore'
import styles from './SidebarLayout.module.css'

const SIDEBAR_MIN = 160
const SIDEBAR_MAX = 480

interface SidebarLayoutProps {
  children: ReactNode
}

export default function SidebarLayout({ children }: SidebarLayoutProps) {
  const { isCollapsed, sidebarWidth, setSidebarWidth } = useSidebarStore()
  const isResizing = useRef(false)

  useEffect(() => {
    const onMouseMove = (e: MouseEvent) => {
      if (!isResizing.current || isCollapsed) return
      const next = Math.min(SIDEBAR_MAX, Math.max(SIDEBAR_MIN, e.clientX))
      setSidebarWidth(next)
    }
    const onMouseUp = () => {
      isResizing.current = false
    }
    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseUp)
    return () => {
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', onMouseUp)
    }
  }, [isCollapsed, setSidebarWidth])

  return (
    <div className={styles.body}>
      <Sidebar width={sidebarWidth} />

      {!isCollapsed && (
        <div
          className={styles.resizeHandle}
          onMouseDown={() => { isResizing.current = true }}
        />
      )}

      <div className={styles.content}>
        {children}
      </div>
    </div>
  )
}
