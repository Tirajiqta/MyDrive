import { NextRequest, NextResponse } from "next/server";
import { stripe, isStripeConfigured } from "@/lib/stripe";

// GET /api/checkout/session?session_id=cs_test_...
// Retrieves a completed Checkout Session so the success page can confirm what
// the user subscribed to.
export async function GET(req: NextRequest) {
  if (!isStripeConfigured) {
    return NextResponse.json({ message: "Stripe is not configured." }, { status: 500 });
  }

  const sessionId = req.nextUrl.searchParams.get("session_id");
  if (!sessionId) {
    return NextResponse.json({ message: "Missing session_id." }, { status: 400 });
  }

  try {
    const session = await stripe.checkout.sessions.retrieve(sessionId);
    return NextResponse.json({
      status: session.status, // "complete" | "open" | "expired"
      paymentStatus: session.payment_status,
      plan: session.metadata?.plan ?? null,
      customerEmail: session.customer_details?.email ?? null,
      amountTotal: session.amount_total,
      currency: session.currency,
    });
  } catch (err) {
    console.error("[checkout/session] retrieve failed:", err);
    return NextResponse.json({ message: "Could not retrieve session." }, { status: 500 });
  }
}
