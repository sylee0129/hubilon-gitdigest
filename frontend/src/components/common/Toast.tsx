import { useEffect } from 'react'
import styles from './Toast.module.css'

interface ToastProps {
  message: string
  visible: boolean
  onClose: () => void
}

export default function Toast({ message, visible, onClose }: ToastProps) {
  useEffect(() => {
    if (!visible) return
    const timer = setTimeout(onClose, 4000)
    return () => clearTimeout(timer)
  }, [visible, onClose])

  if (!visible) return null

  return (
    <div className={styles.toast}>
      <span>{message}</span>
      <button className={styles.close} onClick={onClose}>✕</button>
    </div>
  )
}
