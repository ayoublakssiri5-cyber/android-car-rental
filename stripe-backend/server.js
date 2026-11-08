const cors = require("cors");
const express = require("express");
const Stripe = require("stripe");
require("dotenv").config();

const app = express();
const port = Number(process.env.PORT || 4242);

if (!process.env.STRIPE_SECRET_KEY || !process.env.STRIPE_PUBLISHABLE_KEY) {
  console.warn("Stripe keys missing. Copy .env.example to .env and add your test/live keys.");
}

const stripe = Stripe(process.env.STRIPE_SECRET_KEY || "sk_test_missing");

app.use(cors());
app.use(express.json());

app.get("/health", (req, res) => {
  res.json({ ok: true, service: "driveflow-stripe-backend" });
});

app.post("/payment-sheet", async (req, res) => {
  try {
    const amount = Number(req.body.amount);
    const currency = String(req.body.currency || "mad").toLowerCase();
    const reservationId = req.body.reservationId == null ? "" : String(req.body.reservationId);

    if (!Number.isInteger(amount) || amount < 100) {
      return res.status(400).json({ error: "Invalid amount. Send the amount in cents." });
    }

    const paymentIntent = await stripe.paymentIntents.create({
      amount,
      currency,
      automatic_payment_methods: { enabled: true },
      metadata: {
        app: "DriveFlow",
        reservationId
      }
    });

    res.json({
      publishableKey: process.env.STRIPE_PUBLISHABLE_KEY,
      paymentIntent: paymentIntent.client_secret
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: error.message || "Stripe error" });
  }
});

app.listen(port, "0.0.0.0", () => {
  console.log(`DriveFlow Stripe backend listening on http://localhost:${port}`);
});
