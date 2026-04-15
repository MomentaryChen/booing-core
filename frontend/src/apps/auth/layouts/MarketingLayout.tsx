import { Outlet } from 'react-router-dom'

export function MarketingLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <Outlet />
    </div>
  )
}
