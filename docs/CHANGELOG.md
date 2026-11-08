# Changelog

## Audit-driven fixes

- Deleting a client with reservations, or a reservation with payments, is now refused (it used to leave orphan records that disappeared from lists but were still counted on the dashboard).
- Rental days are rounded instead of truncated: a daylight-saving night of 23 hours no longer removes a billed day.
- Pricing rule extracted into `RentalRules` (unchanged formula) and covered by tests.
- 12 unit tests added (JUnit + Robolectric).
- Repository cleanup for publication: demo data uses clearly fictitious contact details; the preloaded database and photos of unknown origin are not published.

## First version

- Roles, fleet with photos and GPS, clients, reservations with overlap control, payments with Stripe test mode, notifications.
