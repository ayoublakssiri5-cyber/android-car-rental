# DriveFlow Stripe backend (test mode)

Small Express server that creates a Stripe PaymentIntent for the Android app's PaymentSheet.

## Setup

```bash
cd stripe-backend
npm install
cp .env.example .env      # Windows PowerShell: Copy-Item .env.example .env
```

Put your Stripe **test** keys in `.env`. Never commit `.env` (it is ignored by Git).

## Run

```bash
npm start
```

Endpoints:
- `GET /health` — health check
- `POST /payment-sheet` — body `{ "amount": <integer, smallest currency unit>, "currency": "mad", "reservationId": "<id>" }`, returns the publishable key and the PaymentIntent client secret

The Android emulator reaches the server at `http://10.0.2.2:4242/payment-sheet`. Use a Stripe test card such as `4242 4242 4242 4242`.

## Limitations

- No webhook: the server does not confirm payments; the app records a payment when the PaymentSheet reports success.
- Plain HTTP on the emulator only; use HTTPS and a configurable URL for any real deployment.
