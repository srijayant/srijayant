# Expense Pulse

Android app that reads your **SMS inbox**, extracts bank/UPI debit alerts, and builds a **monthly expense report**.

## Features

- **SMS import** — scans inbox for debit / spend / UPI alerts (HDFC, SBI, ICICI, Axis, Kotak, Paytm, PhonePe, GPay, and similar)
- **Auto-categorization** — Food, Shopping, Travel, Bills, Entertainment, Health, Transfers, ATM, Other
- **Monthly report** — total spent, category breakdown (donut), daily spend bars, transaction list
- **Manual entries** — add spends when SMS is missing or incomplete
- **Local-only** — Room database on device; no cloud sync

## Requirements

- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android device/emulator API 26+
- `READ_SMS` permission (requested at launch)

## Open & run

1. Open the `sms-expense-tracker` folder in Android Studio
2. Let Gradle sync
3. Run on a physical device (SMS inbox is empty on most emulators unless you seed messages)
4. Allow SMS access when prompted, then tap **Refresh** to import

```bash
cd sms-expense-tracker
./gradlew :app:assembleDebug   # after generating the wrapper in Android Studio once
```

## How parsing works

`ExpenseParser` looks for debit keywords (`debited`, `spent`, `paid`, …), extracts `₹` / `Rs` / `INR` amounts, pulls a merchant when present (`at`, `to`, `VPA`), and maps merchants to categories. Credit-only alerts are ignored so income does not inflate spend.

## Privacy

SMS and expenses stay on your phone. The app never uploads messages. Revoke SMS permission anytime in system settings; manual entry still works.

## Project layout

```
sms-expense-tracker/
  app/src/main/java/com/srijayant/expense/
    data/          # Room, SMS reader, parser, repository
    ui/            # Compose screens, charts, theme
    viewmodel/     # UI state + sync
    MainActivity.kt
```
