This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

## Subscriptions (Stripe test mode)

The **Subscription** page (`/subscription`) lets users buy a recurring plan via
Stripe Checkout. To enable it:

1. Create a free Stripe account and grab your **test** secret key from
   <https://dashboard.stripe.com/test/apikeys> (it starts with `sk_test_`).
2. Put it in `.env.local`:

   ```bash
   STRIPE_SECRET_KEY=sk_test_xxx
   ```

3. Restart `npm run dev` and open `/subscription`.

On the Stripe checkout page use test card **`4242 4242 4242 4242`**, any future
expiry, any CVC and any postal code. No real money is charged in test mode.

Plans are defined in `lib/plans.ts`; prices are created on the fly via Stripe
`price_data`, so nothing needs to be pre-configured in the dashboard.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.

- [Docs](https://nextjs.org/docs?utm_source=create-next-app&utm_medium=appdir-template-tw&utm_campaign=create-next-app)

- [Learn](https://nextjs.org/learn?utm_source=create-next-app&utm_medium=appdir-template-tw&utm_campaign=create-next-app)

- [Examples](https://vercel.com/templates?framework=next.js&utm_source=create-next-app&utm_medium=appdir-template-tw&utm_campaign=create-next-app) 
