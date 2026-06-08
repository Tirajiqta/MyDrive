"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { useI18n } from "@/contexts/I18nContext";
import { isLocale } from "@/lib/i18n";
import { Sidebar } from "@/components/layout/Sidebar";
import { ChatBot } from "@/components/chat/ChatBot";
import { PageSpinner } from "@/components/ui/Spinner";

function ProtectedShell({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const { setLocale } = useI18n();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  // Apply the user's saved language preference once they're loaded.
  useEffect(() => {
    if (user && isLocale(user.preferredLanguageCode)) {
      setLocale(user.preferredLanguageCode);
    }
  }, [user, setLocale]);

  if (loading) return <PageSpinner />;
  if (!user) return null;

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <Sidebar />
      <main className="flex-1 overflow-y-auto">{children}</main>
      <ChatBot />
    </div>
  );
}

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthProvider>
      <ProtectedShell>{children}</ProtectedShell>
    </AuthProvider>
  );
}
