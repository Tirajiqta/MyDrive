import { NextRequest, NextResponse } from "next/server";
import { stripe, isStripeConfigured } from "@/lib/stripe";
import { PLANS, type PlanId } from "@/lib/plans";

// POST /api/checkout
// Body: { plan: "pro" | "business" }
// Creates a Stripe Checkout Session in subscription mode and returns its URL.
// The browser then redirects the user to Stripe's hosted checkout page.
export async function POST(req: NextRequest) {
  if (!isStripeConfigured) {
    return NextResponse.json(
      {
        message:
          "Stripe is not configured. Set STRIPE_SECRET_KEY in .env.local (use a sk_test_... key).",
      },
      { status: 500 }
    );
  }

  let planId: PlanId;
  try {
    const body = (await req.json()) as { plan?: PlanId };
    if (!body.plan || !(body.plan in PLANS)) {
      return NextResponse.json({ message: "Unknown plan." }, { status: 400 });
    }
    planId = body.plan;
  } catch {
    return NextResponse.json({ message: "Invalid request body." }, { status: 400 });
  }

  const plan = PLANS[planId];
  const origin =
    req.headers.get("origin") ??
    req.nextUrl.origin ??
    "http://localhost:3000";

  try {
    const session = await stripe.checkout.sessions.create({
      mode: "subscription",
      // Build the price on the fly so nothing needs to be pre-created in the
      // Stripe dashboard — ideal for test mode.
      line_items: [
        {
          quantity: 1,
          price_data: {
            currency: plan.currency,
            unit_amount: plan.amount,
            recurring: { interval: "month" },
            product_data: {
              name: `MyDrive ${plan.name}`,
              description: `${plan.storageGB} GB — ${plan.tagline}`,
            },
          },
        },
      ],
      // Let Stripe collect the email; remove allow_promotion_codes if unwanted.
      allow_promotion_codes: true,
      success_url: `${origin}/subscription?status=success&session_id={CHECKOUT_SESSION_ID}`,
      cancel_url: `${origin}/subscription?status=canceled`,
      metadata: { plan: plan.id },
    });

    return NextResponse.json({ url: session.url });
  } catch (err) {
    console.error("[checkout] failed to create session:", err);
    const message =
      err instanceof Error ? err.message : "Failed to create checkout session.";
    return NextResponse.json({ message }, { status: 500 });
  }
}
