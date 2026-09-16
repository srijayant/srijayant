# SpendScope

SpendScope is an Android app that privately scans SMS transaction alerts, detects expenses, categorizes them, and presents a month-by-month spending report.

## Features

- Reads messages only after explicit `READ_SMS` permission is granted
- Recognizes common INR debit, card, ATM, and UPI transaction alerts
- Separates incoming credits, refunds, and unrelated messages
- Reports monthly total, average transaction, top category, category breakdown, and recent expenses
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

Expense detection is heuristic because bank SMS formats differ. The parser looks for debit/payment language, extracts the first INR amount, derives a merchant where possible, and maps transaction keywords to a category. Verify detected transactions before relying on totals for accounting or tax purposes.

## SMS permission and distribution

Google Play treats SMS permissions as restricted. A general expense tracker may not qualify for Play distribution with `READ_SMS`; direct/internal distribution is the practical default unless the app meets an approved policy exception and completes the required declaration. Message data is not logged, persisted, or transmitted by this app.
