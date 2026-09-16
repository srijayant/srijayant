# SpendScope

SpendScope is an Android app that privately scans SMS transaction alerts, detects expenses, categorizes them, and presents a month-by-month spending report.

## Features

- Reads messages only after explicit `READ_SMS` permission is granted
- Recognizes common INR debit, card, ATM, and UPI transaction alerts
- Separates incoming credits, refunds, and unrelated messages
- Reports monthly total, average transaction, top category, category breakdown, and recent expenses
- Suggests categories locally with high, medium, or low confidence from Indian merchant and SMS clues
- Groups suggestions by merchant so you can confirm or change many matching expenses once
- Lets you assign a merchant to an Indian-context bucket and applies that choice to every matching expense
- Exports a deduplicated full-history debit and credit dataset through Android's document picker
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

Expense detection is heuristic because bank SMS formats differ. The offline classifier looks for debit/payment language, extracts the first INR amount, derives a merchant where possible, and combines known Indian merchant matches, message keywords, and transaction types into a category suggestion with a confidence level. The review flow groups matching merchants, lets you confirm or change each suggestion, and then applies that personal rule to every month.

Personal rules store only a SHA-256 merchant fingerprint and category in the app's private preferences. SMS bodies, amounts, and reports are not persisted.

## JSON export

Tap **Export** and choose a destination with Android's system document picker. SpendScope scans the full SMS history in memory and exports derived transaction fields including sender, timestamp, debit/credit/refund type, amount in paise, instrument, account suffix, merchant or payee, VPA, reference number, balance, suggested category, confidence, and collapsed duplicate count.

Raw SMS bodies and internal message fingerprints are never included. Duplicate notifications are collapsed conservatively when they share the same transaction reference, sender, type, and amount, or when the normalized message is identical.

## SMS permission and distribution

Google Play treats SMS permissions as restricted but lists SMS-based money management as an eligible exception. Play distribution still requires a permission declaration, prominent disclosure, privacy policy, and approval. Message data is not logged or transmitted by this app.
