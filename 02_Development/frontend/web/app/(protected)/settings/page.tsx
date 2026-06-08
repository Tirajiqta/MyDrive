"use client";

import { useState } from "react";
import { toast } from "sonner";
import { auth, formatBytes } from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";
import { useI18n } from "@/contexts/I18nContext";
import { LOCALE_LABELS, type Locale } from "@/lib/i18n";
import { Header } from "@/components/layout/Header";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { User, Lock, HardDrive, Languages } from "lucide-react";

function Section({
  title,
  icon,
  children,
}: {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="bg-white rounded-xl border border-gray-100 p-6">
      <div className="flex items-center gap-3 mb-5 pb-4 border-b border-gray-50">
        <div className="w-8 h-8 bg-indigo-50 rounded-lg flex items-center justify-center">
          {icon}
        </div>
        <h2 className="font-semibold text-gray-900">{title}</h2>
      </div>
      {children}
    </div>
  );
}

export default function SettingsPage() {
  const { user, refreshUser } = useAuth();
  const { t, locale, setLocale } = useI18n();

  const [pw, setPw] = useState({
    currentPassword: "",
    newPassword: "",
    confirm: "",
  });
  const [pwLoading, setPwLoading] = useState(false);
  const [langSaving, setLangSaving] = useState(false);

  const handleLanguageChange = async (next: Locale) => {
    setLocale(next); // instant UI update
    setLangSaving(true);
    try {
      await auth.setLanguage(next);
      await refreshUser();
      toast.success(t("common.save"));
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to save language");
    } finally {
      setLangSaving(false);
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (pw.newPassword !== pw.confirm) {
      toast.error("Passwords do not match");
      return;
    }
    if (pw.newPassword.length < 6) {
      toast.error("Password must be at least 6 characters");
      return;
    }
    setPwLoading(true);
    try {
      await auth.changePassword({
        currentPassword: pw.currentPassword,
        newPassword: pw.newPassword,
      });
      toast.success("Password changed");
      setPw({ currentPassword: "", newPassword: "", confirm: "" });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to change password");
    } finally {
      setPwLoading(false);
    }
  };

  const storageUsed = user?.currentStorageUsedBytes ?? 0;
  const storageLimit = (user?.storageLimitGB ?? 0) * 1024 * 1024 * 1024;
  const storagePct = storageLimit > 0 ? (storageUsed / storageLimit) * 100 : 0;

  return (
    <div className="flex flex-col h-full">
      <Header title={t("settings.title")} />

      <div className="px-8 py-6 flex-1 overflow-y-auto">
        <div className="max-w-2xl mx-auto flex flex-col gap-6">
          {/* Profile */}
          <Section
            title={t("settings.profile")}
            icon={<User className="w-4 h-4 text-indigo-600" />}
          >
            <div className="flex items-center gap-5 mb-6">
              <div className="w-14 h-14 bg-indigo-600 rounded-full flex items-center justify-center text-white text-xl font-semibold">
                {user?.username?.[0]?.toUpperCase() ?? "?"}
              </div>
              <div>
                <p className="font-semibold text-gray-900">{user?.username}</p>
                <p className="text-sm text-gray-500">{user?.email}</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="bg-gray-50 rounded-lg px-4 py-3">
                <p className="text-gray-400 text-xs mb-1">
                  {t("settings.memberSince")}
                </p>
                <p className="font-medium text-gray-700">
                  {user?.createdAt
                    ? new Date(user.createdAt).toLocaleDateString(locale, {
                        month: "long",
                        year: "numeric",
                      })
                    : "—"}
                </p>
              </div>
              <div className="bg-gray-50 rounded-lg px-4 py-3">
                <p className="text-gray-400 text-xs mb-1">
                  {t("settings.language")}
                </p>
                <p className="font-medium text-gray-700 uppercase">
                  {user?.preferredLanguageCode ?? locale}
                </p>
              </div>
            </div>
          </Section>

          {/* Language */}
          <Section
            title={t("settings.language")}
            icon={<Languages className="w-4 h-4 text-indigo-600" />}
          >
            <p className="text-sm text-gray-500 mb-3">
              {t("settings.languageHint")}
            </p>
            <select
              value={locale}
              disabled={langSaving}
              onChange={(e) => handleLanguageChange(e.target.value as Locale)}
              className="w-full sm:w-64 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 disabled:opacity-60"
            >
              {(Object.keys(LOCALE_LABELS) as Locale[]).map((code) => (
                <option key={code} value={code}>
                  {LOCALE_LABELS[code]}
                </option>
              ))}
            </select>
          </Section>

          {/* Storage */}
          <Section
            title={t("settings.storage")}
            icon={<HardDrive className="w-4 h-4 text-indigo-600" />}
          >
            <div className="flex justify-between text-sm mb-2">
              <span className="text-gray-600">
                {formatBytes(storageUsed)} {t("settings.used")}
              </span>
              <span className="text-gray-400">
                {t("settings.of")} {user?.storageLimitGB} GB
              </span>
            </div>
            <div className="w-full bg-gray-100 rounded-full h-2 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all ${
                  storagePct > 90
                    ? "bg-red-500"
                    : storagePct > 70
                    ? "bg-yellow-400"
                    : "bg-indigo-500"
                }`}
                style={{ width: `${Math.min(storagePct, 100)}%` }}
              />
            </div>
            <p className="text-xs text-gray-400 mt-2">
              {(100 - storagePct).toFixed(1)}% {t("settings.free")}
            </p>
          </Section>

          {/* Change password */}
          <Section
            title={t("settings.changePassword")}
            icon={<Lock className="w-4 h-4 text-indigo-600" />}
          >
            <form
              onSubmit={handlePasswordChange}
              className="flex flex-col gap-4"
            >
              <Input
                label={t("settings.currentPassword")}
                type="password"
                value={pw.currentPassword}
                onChange={(e) =>
                  setPw((p) => ({ ...p, currentPassword: e.target.value }))
                }
                placeholder="••••••••"
                required
              />
              <Input
                label={t("settings.newPassword")}
                type="password"
                value={pw.newPassword}
                onChange={(e) =>
                  setPw((p) => ({ ...p, newPassword: e.target.value }))
                }
                placeholder="••••••••"
                required
              />
              <Input
                label={t("settings.confirmPassword")}
                type="password"
                value={pw.confirm}
                onChange={(e) =>
                  setPw((p) => ({ ...p, confirm: e.target.value }))
                }
                placeholder="••••••••"
                required
              />
              <div className="flex justify-end">
                <Button type="submit" loading={pwLoading}>
                  {t("settings.updatePassword")}
                </Button>
              </div>
            </form>
          </Section>
        </div>
      </div>
    </div>
  );
}
