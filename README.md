# SpendScope

SpendScope is an Android app that privately scans SMS transaction alerts, detects expenses, categorizes them, and presents a month-by-month spending report.

## Features

- Reads messages only after explicit `READ_SMS` permission is granted
- Recognizes common INR debit, card, ATM, and UPI transaction alerts
- Separates incoming credits, refunds, and unrelated messages
- Reports monthly total, average transaction, top category, category breakdown, and recent expenses
- Lets you assign a merchant to an Indian-context bucket and applies that choice to every matching expense
- Lets you move between months and refresh on demand
- Processes everything on-device; there is no internet permission, analytics, account, or cloud storage

## Build

Requirements:

- JDK 17 or newer
- Android SDK 36 with Build Tools 35.0.0 or newer

```bash
./gradlew test
./gradlew assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/app-debug.apk` on an Android 8.0+ device.

## Detection notes

Expense detection is heuristic because bank SMS formats differ. The parser looks for debit/payment language, extracts the first INR amount, derives a merchant where possible, and maps Indian merchant keywords to a category. Tap an expense to create a personal merchant rule; the selected category is then applied to all matching transactions in every month.

Personal rules store only a SHA-256 merchant fingerprint and category in the app's private preferences. SMS bodies, amounts, and reports are not persisted.

## SMS permission and distribution

Google Play treats SMS permissions as restricted but lists SMS-based money management as an eligible exception. Play distribution still requires a permission declaration, prominent disclosure, privacy policy, and approval. Message data is not logged or transmitted by this app.
