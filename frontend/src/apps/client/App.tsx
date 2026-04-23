import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '@/shared/components/ProtectedRoute'
import { ClientLayout } from './layouts/ClientLayout'
import { BookingHomePage } from './pages/BookingHomePage'
import { SearchPage } from './pages/SearchPage'
import { BookingPage } from './pages/BookingPage'
import { MyBookingsPage } from './pages/MyBookingsPage'
import { ProfilePage } from './pages/ProfilePage'

export function ClientApp() {
  return (
    <BrowserRouter basename="/client">
      <Routes>
        <Route
          element={
            <ProtectedRoute requiredRole="CLIENT">
              <ClientLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<BookingHomePage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="booking/:resourceId" element={<BookingPage />} />
          <Route path="my-bookings" element={<MyBookingsPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="*" element={<Navigate to="." replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
