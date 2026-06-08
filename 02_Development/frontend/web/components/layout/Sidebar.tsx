"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  CloudIcon,
  CreditCard,
  FolderOpen,
  HelpCircle,
  Link2,
  LogOut,
  Settings,
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useI18n } from "@/contexts/I18nContext";
import { StorageBar } from "@/components/drive/StorageBar";
import { toast } from "sonner";
import { useRouter } from "next/navigation";

const navItems = [
  { href: "/drive", icon: FolderOpen, labelKey: "nav.drive" },
  { href: "/shares", icon: Link2, labelKey: "nav.shares" },
  { href: "/subscription", icon: CreditCard, labelKey: "nav.subscription" },
  { href: "/support", icon: HelpCircle, labelKey: "nav.support" },
  { href: "/settings", icon: Settings, labelKey: "nav.settings" },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const { t } = useI18n();
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await logout();
      router.push("/login");
    } catch {
      toast.error("Logout failed");
    }
  };

  return (
    <aside className="w-60 bg-gray-900 text-white flex flex-col h-screen flex-shrink-0">
      {/* Logo */}
      <Link
        href="/drive"
        className="flex items-center gap-3 px-5 py-5 border-b border-gray-800"
      >
        <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
          <CloudIcon className="w-5 h-5 text-white" />
        </div>
        <span className="font-semibold text-lg tracking-tight">MyDrive</span>
      </Link>

      {/* Nav */}
      <nav className="flex-1 py-4 overflow-y-auto">
        {navItems.map(({ href, icon: Icon, labelKey }) => {
          const active =
            href === "/drive"
              ? pathname === "/drive" || pathname.startsWith("/drive/folder")
              : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 px-5 py-2.5 mx-2 rounded-lg text-sm font-medium transition-colors
                ${
                  active
                    ? "bg-indigo-600 text-white"
                    : "text-gray-400 hover:bg-gray-800 hover:text-white"
                }`}
            >
              <Icon className="w-4 h-4" />
              {t(labelKey)}
            </Link>
          );
        })}
      </nav>

      {/* Storage */}
      {user && <StorageBar user={user} />}

      {/* User + Logout */}
      <div className="border-t border-gray-800 p-4">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-8 h-8 bg-indigo-600 rounded-full flex items-center justify-center text-sm font-semibold flex-shrink-0">
            {user?.username?.[0]?.toUpperCase() ?? "?"}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium truncate">{user?.username}</p>
            <p className="text-xs text-gray-400 truncate">{user?.email}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="flex items-center gap-2 w-full px-3 py-2 rounded-lg text-sm text-gray-400 hover:text-white hover:bg-gray-800 transition-colors"
        >
          <LogOut className="w-4 h-4" />
          {t("nav.signOut")}
        </button>
      </div>
    </aside>
  );
}
