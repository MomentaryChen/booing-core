import React from 'react';
import ReactDOM from 'react-dom/client';
import { AdminApp } from './apps/admin/App';
import '@/i18n';
import '@/styles/globals.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AdminApp />
  </React.StrictMode>
);
