import React from 'react'
import ReactDOM from 'react-dom/client'
import { AuthApp } from './apps/auth/App'
import { AdminApp } from './apps/admin/App'
import { MerchantApp } from './apps/merchant/App'
import { ClientApp } from './apps/client/App'
import './i18n'
import './styles/globals.css'

function resolveRootApp() {
  const pathname = window.location.pathname
  if (pathname.startsWith('/system')) return <AdminApp />
  if (pathname.startsWith('/merchant')) return <MerchantApp />
  if (pathname.startsWith('/client')) return <ClientApp />
  return <AuthApp />
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    {resolveRootApp()}
  </React.StrictMode>,
)
