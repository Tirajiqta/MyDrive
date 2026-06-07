// ─── Subscription plans ────────────────────────────────────────────────────
//
// Shared between the subscription page (UI) and the Stripe checkout route
// handler (server). Prices are created on-the-fly via Stripe `price_data`, so
// nothing needs to be pre-configured in the Stripe dashboard — just plug in a
// test secret key and everything works in Stripe test mode.

export type PlanId = "pro" | "business";

export interface Plan {
  id: PlanId;
  name: string;
  /** Price in the smallest currency unit (cents) charged monthly. */
  amount: number;
  currency: string;
  storageGB: number;
  tagline: string;
  features: string[];
  highlighted?: boolean;
}

export const PLANS: Record<PlanId, Plan> = {
  pro: {
    id: "pro",
    name: "Pro",
    amount: 499, // €4.99
    currency: "eur",
    storageGB: 100,
    tagline: "For individuals who need more room.",
    features: [
      "100 GB of storage",
      "Document e-signing (eIDAS)",
      "Collaborative document editing",
      "Priority email support",
    ],
    highlighted: true,
  },
  business: {
    id: "business",
    name: "Business",
    amount: 999, // €9.99
    currency: "eur",
    storageGB: 1024,
    tagline: "For teams and power users.",
    features: [
      "1 TB of storage",
      "Everything in Pro",
      "Advanced sharing controls",
      "Dedicated support",
    ],
  },
};

export function formatPrice(amount: number, currency: string): string {
  return new Intl.NumberFormat("en-IE", {
    style: "currency",
    currency: currency.toUpperCase(),
  }).format(amount / 100);
}
