import React from 'react'
import ReactDOM from 'react-dom/client'
import { MerchantApp } from './apps/merchant/App'
import './i18n'
import './styles/globals.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <MerchantApp />
  </React.StrictMode>,
)
