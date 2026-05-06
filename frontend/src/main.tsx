import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import './index.css'
import { queryClient } from './queryClient'
import keycloak from './lib/keycloak'
import { useAuthStore } from './stores/useAuthStore'

keycloak.onTokenExpired = () => {
  keycloak.updateToken(70).catch(() => keycloak.logout())
}

keycloak
  .init({
    onLoad: 'login-required',
    checkLoginIframe: false,
    pkceMethod: 'S256',
  })
  .then((authenticated) => {
    if (authenticated) {
      useAuthStore.getState().initFromKeycloak(keycloak)

      const rootEl = document.getElementById('root')
      if (!rootEl) throw new Error('root element not found')

      createRoot(rootEl).render(
        <StrictMode>
          <QueryClientProvider client={queryClient}>
            <App />
          </QueryClientProvider>
        </StrictMode>,
      )
    }
  })
