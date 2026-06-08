"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { Check, Sparkles, CreditCard, CheckCircle2 } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/Button";
import { PageSpinner } from "@/components/ui/Spinner";
import { PLANS, formatPrice, type Plan, type PlanId } from "@/lib/plans";
import { subscriptionApi } from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

interface SessionInfo {
  status: string;
  paymentStatus: string;
  plan: string | null;
  customerEmail: string | null;
  amountTotal: number | null;
  currency: string | null;
}

function PlanCard({
  plan,
  loading,
  onSubscribe,
}: {
  plan: Plan;
  loading: boolean;
  onSubscribe: (id: PlanId) => void;
}) {
  return (
    <div
      className={`relative flex flex-col rounded-2xl border p-6 bg-white ${
        plan.highlighted
          ? "border-indigo-500 shadow-lg shadow-indigo-100"
          : "border-gray-200"
      }`}
    >
      {plan.highlighted && (
        <span className="absolute -top-3 left-6 inline-flex items-center gap-1 rounded-full bg-indigo-600 px-3 py-1 text-xs font-semibold text-white">
          <Sparkles className="w-3 h-3" />
          Most popular
        </span>
      )}

      <h3 className="text-lg font-semibold text-gray-900">{plan.name}</h3>
      <p className="text-sm text-gray-500 mt-1">{plan.tagline}</p>

      <div className="mt-5 flex items-baseline gap-1">
        <span className="text-3xl font-bold text-gray-900">
          {formatPrice(plan.amount, plan.currency)}
        </span>
        <span className="text-sm text-gray-500">/ month</span>
      </div>

      <ul className="mt-6 flex flex-col gap-3 flex-1">
        {plan.features.map((f) => (
          <li key={f} className="flex items-start gap-2 text-sm text-gray-700">
            <Check className="w-4 h-4 text-indigo-600 mt-0.5 flex-shrink-0" />
            {f}
          </li>
        ))}
      </ul>

      <Button
        className="mt-6 w-full"
        variant={plan.highlighted ? "primary" : "secondary"}
        loading={loading}
        onClick={() => onSubscribe(plan.id)}
      >
        <CreditCard className="w-4 h-4" />
        Subscribe
      </Button>
    </div>
  );
}

function SubscriptionContent() {
  const router = useRouter();
  const params = useSearchParams();
  const { refreshUser } = useAuth();
  const status = params.get("status");
  const sessionId = params.get("session_id");

  const [loadingPlan, setLoadingPlan] = useState<PlanId | null>(null);
  const [session, setSession] = useState<SessionInfo | null>(null);

  // Handle the redirect back from Stripe.
  useEffect(() => {
    if (status === "canceled") {
      toast.error("Checkout canceled — no charge was made.");
      router.replace("/subscription");
    }
    if (status === "success" && sessionId) {
      fetch(`/api/checkout/session?session_id=${encodeURIComponent(sessionId)}`)
        .then((r) => r.json())
        .then(async (data: SessionInfo) => {
          if (data.status !== "complete") {
            toast.error("Payment not completed.");
            return;
          }
          setSession(data);
          // Persist the purchased plan on the backend so the user's storage
          // limit actually changes, then refresh the cached user.
          if (data.plan) {
            try {
              await subscriptionApi.activate({
                plan: data.plan,
                stripeSessionId: sessionId,
              });
              await refreshUser();
            } catch {
              toast.error(
                "Payment succeeded but we couldn't update your plan. Contact support."
              );
            }
          }
        })
        .catch(() => toast.error("Could not confirm your subscription."));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, sessionId]);

  const handleSubscribe = async (planId: PlanId) => {
    setLoadingPlan(planId);
    try {
      const res = await fetch("/api/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ plan: planId }),
      });
      const data = (await res.json()) as { url?: string; message?: string };
      if (!res.ok || !data.url) {
        throw new Error(data.message ?? "Failed to start checkout.");
      }
      // Redirect to Stripe's hosted checkout page.
      window.location.href = data.url;
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Something went wrong.");
      setLoadingPlan(null);
    }
  };

  // ─── Success screen ──────────────────────────────────────────────────────
  if (session) {
    const planName = session.plan ? PLANS[session.plan as PlanId]?.name : null;
    return (
      <div className="flex flex-col h-full">
        <Header title="Subscription" />
        <div className="px-8 py-6 flex-1 overflow-y-auto">
          <div className="max-w-md mx-auto bg-white rounded-2xl border border-gray-100 p-8 text-center mt-10">
            <div className="w-14 h-14 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="w-8 h-8 text-green-600" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900">
              You&apos;re subscribed{planName ? ` to ${planName}` : ""}!
            </h2>
            <p className="text-sm text-gray-500 mt-2">
              {session.amountTotal != null && session.currency
                ? `${formatPrice(session.amountTotal, session.currency)} / month`
                : "Your subscription is active."}
            </p>
            {session.customerEmail && (
              <p className="text-sm text-gray-500 mt-1">
                A receipt was sent to {session.customerEmail}.
              </p>
            )}
            <Button
              className="mt-6"
              onClick={() => router.push("/drive")}
            >
              Go to My Drive
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // ─── Pricing screen ────────────────────────────────────────────────────────
  return (
    <div className="flex flex-col h-full">
      <Header title="Subscription" />
      <div className="px-8 py-6 flex-1 overflow-y-auto">
        <div className="max-w-3xl mx-auto">
          <div className="text-center mb-8">
            <h2 className="text-2xl font-bold text-gray-900">
              Upgrade your storage
            </h2>
            <p className="text-gray-500 mt-2">
              Pick a plan that fits your needs. Cancel anytime.
            </p>
          </div>

          <div className="grid gap-6 sm:grid-cols-2">
            {Object.values(PLANS).map((plan) => (
              <PlanCard
                key={plan.id}
                plan={plan}
                loading={loadingPlan === plan.id}
                onSubscribe={handleSubscribe}
              />
            ))}
          </div>

          <p className="text-center text-xs text-gray-400 mt-8">
            Payments are processed securely by Stripe. In test mode use card{" "}
            <code className="font-mono text-gray-500">4242 4242 4242 4242</code>{" "}
            with any future expiry, any CVC and any postal code.
          </p>
        </div>
      </div>
    </div>
  );
}

export default function SubscriptionPage() {
  return (
    <Suspense fallback={<PageSpinner />}>
      <SubscriptionContent />
    </Suspense>
  );
}
