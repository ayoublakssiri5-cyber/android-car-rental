# Testing

## How to run

```bash
./gradlew testDebugUnitTest          # Windows: gradlew.bat testDebugUnitTest
```

Use JDK 17 or 21 (for example the JDK bundled with Android Studio).

## Last verified run

- Date: 2026-11-08, Windows 11, JDK 21 (Android Studio JBR), Gradle 9.4.1, AGP 9.1.0
- Command: `gradlew --no-daemon testDebugUnitTest assembleDebug`
- Result: **BUILD SUCCESSFUL — 12 tests, 0 failures, 0 errors, 0 skipped**

| Suite | Tests | Covers |
|---|---|---|
| `RentalRulesTest` | 4 | Days between dates, daylight-saving change (Europe/Paris), invalid dates, price |
| `DatabaseHelperTest` (Robolectric, real SQLite) | 8 | Login and unique usernames, overlap with confirmed reservations only, pending/cancelled not blocking, partial payments and remaining balance, deletion guards for cars, clients and reservations, demo accounts |

## Not covered

- UI and navigation (no instrumented tests).
- Stripe payments (require the server and Stripe test keys).
