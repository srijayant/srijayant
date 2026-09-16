# SMS Expense Tracker

An Android app that reads your SMS inbox, extracts bank / card / UPI transaction
alerts, and turns them into a monthly expense report — entirely on-device.

## Features

- **SMS scanning** — reads the last 6 months of inbox messages via the
  `READ_SMS` runtime permission (requested with a clear explanation screen).
- **Transaction parsing** — regex-based parser tuned for Indian bank formats
  (HDFC, ICICI, SBI, Axis, Kotak, UPI apps, card networks). Rejects OTPs,
  promotional offers, payment requests, and failed/reversed transactions.
- **Auto-categorization** — Food, Groceries, Fuel, Travel, Bills,
  Entertainment, Health, Shopping, Rent, Investments, ATM cash, Transfers, etc.
- **Monthly report** — spent vs. received summary, donut chart and per-category
  breakdown with percentages, and a full transaction list. Swap months with the
  arrow controls.
- **Privacy-first** — no network permission, no storage, no analytics. Messages
  are parsed in memory on every scan and never leave the device.

## Screens

1. **Permission screen** — explains why SMS access is needed before asking.
2. **Report screen** — month selector, Spent/Received cards, "Where it went"
   category breakdown, transaction list with merchant, category, date, and
   signed amount.

## Tech stack

- Kotlin 2.0, Jetpack Compose (Material 3), MVVM with `StateFlow`
- min SDK 26 (Android 8.0), target SDK 34
- No third-party dependencies beyond AndroidX

## Build

```bash
cd sms-expense-tracker
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # parser unit tests
```

Requires JDK 17+ and the Android SDK (platform 34).

## Project layout

```
app/src/main/java/com/srijayant/smsexpense/
├── MainActivity.kt              # entry point, permission bootstrap
├── data/
│   ├── Transaction.kt           # models: Transaction, SmsMessage, Category
│   ├── SmsReader.kt             # Telephony content-provider inbox reader
│   ├── TransactionParser.kt     # SMS → Transaction extraction rules
│   └── Categorizer.kt           # keyword-based category assignment
└── ui/
    ├── ExpenseViewModel.kt      # scan orchestration + monthly aggregation
    ├── ExpenseApp.kt            # Compose screens (permission, report)
    └── theme/Theme.kt           # Material 3 theme + category palette
```

## Notes on Play Store distribution

Google Play restricts the `READ_SMS` permission to apps whose core purpose
requires it. This app is intended for personal / sideloaded use; publishing on
Play would require a permission declaration and review.
