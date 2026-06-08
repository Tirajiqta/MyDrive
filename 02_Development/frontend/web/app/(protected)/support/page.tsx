"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ChevronDown, LifeBuoy, Plus } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { useI18n } from "@/contexts/I18nContext";
import {
  faqApi,
  supportApi,
  type FaqResponse,
  type SupportTicketResponse,
} from "@/lib/api";

const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;

function FaqItem({ faq }: { faq: FaqResponse }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="border border-gray-200 rounded-xl bg-white overflow-hidden">
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm font-medium text-gray-900"
      >
        {faq.translatedQuestion ?? faq.internalQuestionKey}
        <ChevronDown
          className={`w-4 h-4 text-gray-400 transition-transform ${open ? "rotate-180" : ""}`}
        />
      </button>
      {open && (
        <p className="px-4 pb-4 text-sm text-gray-600 whitespace-pre-line">
          {faq.translatedAnswer}
        </p>
      )}
    </div>
  );
}

function statusColor(status: string): string {
  switch (status) {
    case "OPEN":
      return "bg-blue-50 text-blue-700";
    case "IN_PROGRESS":
      return "bg-amber-50 text-amber-700";
    case "RESOLVED":
    case "CLOSED":
      return "bg-green-50 text-green-700";
    default:
      return "bg-gray-100 text-gray-600";
  }
}

export default function SupportPage() {
  const { t, locale } = useI18n();
  const [faqs, setFaqs] = useState<FaqResponse[]>([]);
  const [tickets, setTickets] = useState<SupportTicketResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<(typeof PRIORITIES)[number]>("MEDIUM");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([faqApi.list(locale), supportApi.listTickets()])
      .then(([f, ticketList]) => {
        setFaqs(f);
        setTickets(ticketList);
      })
      .catch(() => toast.error("Failed to load support content."))
      .finally(() => setLoading(false));
  }, [locale]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject.trim() || !description.trim()) {
      toast.error("Please fill in the subject and description.");
      return;
    }
    setSubmitting(true);
    try {
      const created = await supportApi.createTicket({ subject, description, priority });
      setTickets((prev) => [created, ...prev]);
      setSubject("");
      setDescription("");
      setPriority("MEDIUM");
      toast.success(t("support.ticketCreated"));
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to submit ticket.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col h-full">
      <Header title={t("support.title")} />
      <div className="px-8 py-6 flex-1 overflow-y-auto">
        <div className="max-w-3xl mx-auto flex flex-col gap-10">
          {loading ? (
            <div className="flex justify-center py-10">
              <Spinner />
            </div>
          ) : (
            <>
              {/* FAQ */}
              <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  {t("support.faqTitle")}
                </h2>
                <div className="flex flex-col gap-3">
                  {faqs.length === 0 ? (
                    <p className="text-sm text-gray-500">{t("support.noFaqs")}</p>
                  ) : (
                    faqs.map((faq) => <FaqItem key={faq.id} faq={faq} />)
                  )}
                </div>
              </section>

              {/* New ticket */}
              <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <LifeBuoy className="w-5 h-5 text-indigo-600" />
                  {t("support.contactTitle")}
                </h2>
                <form
                  onSubmit={handleSubmit}
                  className="flex flex-col gap-4 bg-white border border-gray-200 rounded-2xl p-6"
                >
                  <Input
                    label={t("support.subject")}
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    placeholder={t("support.subjectPlaceholder")}
                    maxLength={255}
                  />
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1.5">
                      {t("support.description")}
                    </label>
                    <textarea
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      rows={5}
                      placeholder={t("support.descriptionPlaceholder")}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1.5">
                      {t("support.priority")}
                    </label>
                    <select
                      value={priority}
                      onChange={(e) =>
                        setPriority(e.target.value as (typeof PRIORITIES)[number])
                      }
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    >
                      {PRIORITIES.map((p) => (
                        <option key={p} value={p}>
                          {p.charAt(0) + p.slice(1).toLowerCase()}
                        </option>
                      ))}
                    </select>
                  </div>
                  <Button type="submit" loading={submitting} className="self-start">
                    <Plus className="w-4 h-4" />
                    {t("support.submitTicket")}
                  </Button>
                </form>
              </section>

              {/* My tickets */}
              <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">
                  {t("support.yourTickets")}
                </h2>
                {tickets.length === 0 ? (
                  <p className="text-sm text-gray-500">
                    {t("support.noTickets")}
                  </p>
                ) : (
                  <div className="flex flex-col gap-3">
                    {tickets.map((t) => (
                      <div
                        key={t.id}
                        className="bg-white border border-gray-200 rounded-xl p-4"
                      >
                        <div className="flex items-center justify-between gap-3">
                          <p className="text-sm font-medium text-gray-900">
                            {t.subject}
                          </p>
                          <span
                            className={`text-xs font-medium px-2 py-0.5 rounded-full ${statusColor(
                              t.status
                            )}`}
                          >
                            {t.status}
                          </span>
                        </div>
                        <p className="text-sm text-gray-600 mt-1 whitespace-pre-line">
                          {t.description}
                        </p>
                        <p className="text-xs text-gray-400 mt-2">
                          Priority: {t.priority}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
