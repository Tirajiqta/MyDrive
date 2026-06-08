// ─── Lightweight i18n ─────────────────────────────────────────────────────────
//
// A dependency-free translation layer. Message catalogues are keyed by locale
// code (matching the backend `languages` table: "en", "bg"). Components read
// strings through the `useI18n()` hook (see contexts/I18nContext.tsx) and the
// `t("namespace.key")` helper. Adding a language is a matter of adding an entry
// to MESSAGES with the same key set.

export type Locale = "en" | "bg";

export const DEFAULT_LOCALE: Locale = "en";

export const LOCALE_LABELS: Record<Locale, string> = {
  en: "English",
  bg: "Български",
};

type Messages = Record<string, string>;

const en: Messages = {
  "nav.drive": "My Drive",
  "nav.shares": "Shared Links",
  "nav.subscription": "Subscription",
  "nav.support": "Help & Support",
  "nav.settings": "Settings",
  "nav.signOut": "Sign out",

  "common.save": "Save",
  "common.cancel": "Cancel",
  "common.submit": "Submit",
  "common.loading": "Loading…",

  "settings.title": "Settings",
  "settings.profile": "Profile",
  "settings.memberSince": "Member since",
  "settings.language": "Language",
  "settings.storage": "Storage",
  "settings.used": "used",
  "settings.of": "of",
  "settings.free": "free",
  "settings.changePassword": "Change password",
  "settings.currentPassword": "Current password",
  "settings.newPassword": "New password",
  "settings.confirmPassword": "Confirm new password",
  "settings.updatePassword": "Update password",
  "settings.languageHint": "Choose the language used across the app.",

  "support.title": "Help & Support",
  "support.faqTitle": "Frequently asked questions",
  "support.noFaqs": "No FAQs available yet.",
  "support.contactTitle": "Contact support",
  "support.subject": "Subject",
  "support.subjectPlaceholder": "Briefly describe your issue",
  "support.description": "Description",
  "support.descriptionPlaceholder": "Tell us what's going on…",
  "support.priority": "Priority",
  "support.submitTicket": "Submit ticket",
  "support.yourTickets": "Your tickets",
  "support.noTickets": "You haven't opened any tickets yet.",
  "support.ticketCreated": "Ticket submitted — we'll get back to you soon.",
};

const bg: Messages = {
  "nav.drive": "Моят диск",
  "nav.shares": "Споделени връзки",
  "nav.subscription": "Абонамент",
  "nav.support": "Помощ и поддръжка",
  "nav.settings": "Настройки",
  "nav.signOut": "Изход",

  "common.save": "Запази",
  "common.cancel": "Отказ",
  "common.submit": "Изпрати",
  "common.loading": "Зареждане…",

  "settings.title": "Настройки",
  "settings.profile": "Профил",
  "settings.memberSince": "Член от",
  "settings.language": "Език",
  "settings.storage": "Хранилище",
  "settings.used": "използвани",
  "settings.of": "от",
  "settings.free": "свободни",
  "settings.changePassword": "Смяна на парола",
  "settings.currentPassword": "Текуща парола",
  "settings.newPassword": "Нова парола",
  "settings.confirmPassword": "Потвърди новата парола",
  "settings.updatePassword": "Обнови паролата",
  "settings.languageHint": "Изберете езика, използван в приложението.",

  "support.title": "Помощ и поддръжка",
  "support.faqTitle": "Често задавани въпроси",
  "support.noFaqs": "Все още няма въпроси.",
  "support.contactTitle": "Свържете се с поддръжката",
  "support.subject": "Тема",
  "support.subjectPlaceholder": "Опишете накратко проблема си",
  "support.description": "Описание",
  "support.descriptionPlaceholder": "Разкажете ни какво се случва…",
  "support.priority": "Приоритет",
  "support.submitTicket": "Изпрати заявка",
  "support.yourTickets": "Вашите заявки",
  "support.noTickets": "Все още нямате отворени заявки.",
  "support.ticketCreated": "Заявката е изпратена — ще се свържем с вас скоро.",
};

export const MESSAGES: Record<Locale, Messages> = { en, bg };

export function isLocale(value: string | null | undefined): value is Locale {
  return value === "en" || value === "bg";
}

export function translate(locale: Locale, key: string): string {
  return MESSAGES[locale]?.[key] ?? MESSAGES[DEFAULT_LOCALE][key] ?? key;
}
