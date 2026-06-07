import Stripe from "stripe";

// Server-side Stripe client. Reads the secret key from the environment so the
// key never reaches the browser. Use a test-mode key (sk_test_...) for testing.
const secretKey = process.env.STRIPE_SECRET_KEY;

if (!secretKey) {
  // Thrown lazily at request time (not at import) only if a route actually
  // tries to use Stripe without a configured key, which gives a clear error.
  console.warn(
    "[stripe] STRIPE_SECRET_KEY is not set — checkout will fail until it is configured in .env.local"
  );
}

export const stripe = new Stripe(secretKey ?? "", {
  // Pin the API version shipped with the installed SDK for predictable behaviour.
  apiVersion: "2026-05-27.dahlia",
  appInfo: { name: "MyDrive" },
});

export const isStripeConfigured = Boolean(secretKey);
