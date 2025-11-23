import { Sidebar } from "@/components/layouts/sidebar";
import { DashboardHeader } from "@/components/layouts/dashboard-header";
import { ProtectedRoute } from "@/components/layouts/protected-route";

/**
 * Dashboard layout
 * Wraps all authenticated pages with sidebar and header
 * Includes route protection
 */
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProtectedRoute>
      <div className="flex h-screen overflow-hidden">
        <Sidebar />
        <div className="flex flex-1 flex-col overflow-hidden">
          <DashboardHeader />
          <main className="flex-1 overflow-y-auto bg-muted/20 p-6">
            {children}
          </main>
        </div>
      </div>
    </ProtectedRoute>
  );
}
