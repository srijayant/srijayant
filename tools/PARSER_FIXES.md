# PARSER_FIXES.md — findings from real scan (48,548 SMS → 1,391 "merchants")

Breakdown after classification: brand 380 · gateway/QR 336 · noise 225 · unknown name/shop 168 · person 140 · spouse 59 · refund 29 · self 24 · CC payment 15 · wallet 13.

## 1. Noise (16% of merchants are not merchants)
Merchant regex is grabbing text from promos, reminders, and NAV/invoice messages.
- Run reject filter BEFORE merchant extraction. Add rejects: `NAV OF`, `INVOICE DATED`, `BE PAID BY`, `AVOID (LATE|CHARGES|DISCONNECTION|…)`, `GET (FLAT )?RS`, `& GET A FREE`, `WIN VOUCHERS`, `PRE-APPROVED`, `RENEW THE SUBSCRIPTION`, `WEB CHECK-IN`, `NON PAYMENT SUPPLY`.
- Only extract merchant from structured slots (§3). Free-text `at|to` fallback only when no structured slot exists, and never return: dates, bare numbers, single stopwords (PAY, RS, YOU, US, WWW, STOP, TODAY), sender IDs (`^[A-Z]{2}-[A-Z0-9]{3,8}(-[A-Z])?$`), branch/road/chowk/station addresses, masked cards (`CARD \d{4}X`).

## 2. Money movements that are not spend
- **Self** (`TRANSFER_SELF`): VPAs/names containing `SRIJAYANT`, `JAYANT-SRIJAYANTSINGH`. Make this a user-editable "My identities" list in Settings, seeded on onboarding.
- **Spouse** (`Family Transfer` category, excluded from spend by default, toggle): `KRITIKA`, `KRITIKAIT09`.
- **CC payment** (`TRANSFER_SELF`): `CRED*`, `DREAMPLUG*`, `PAYTM CREDIT CARD BILL`, `Payment of Rs X has been received on your … Credit Card`.
- **Refund** (`REFUND`): `HAS BEEN CREDITED`, `HAS BEEN PROCESSED`, `WILL BE CREDITED BY`, `*.REFUNDS@*`, `GPAYREFUND`, `REV-UPI-*`, `*REVERSAL*`, `BHIMCASHBACK`. Merchant = text before `HAS BEEN|WILL BE`.
- **Wallet top-up** (`TRANSFER_SELF`): `ADDMONEY`, `ADD-MONEY@PAYTM`, `AIRTELMONEY`, `OLAMONEY`, gift cards, `GYFTR`, `EVOUCHERS`.
- **Dividend** (`CREDIT`, Income: Dividend): `<COMPANY> LTD dd-MMM-yy` credits.
- **Mutual fund NAV / allotment SMS**: reject (payment already captured separately).

## 3. Structured merchant slots (parse these first)
| Shape | Example | Merchant |
|---|---|---|
| Axis UPI | `UPI/P2M/<ref>/PORTER` | last segment; `P2A` → person |
| ICICI CC UPI | `UPI-<ref>-THESOULE.` | after 2nd dash, before `.` |
| NEFT/IMPS-style UPI | `NAME-VPA-IFSC-REF-REMARK.AVL` | NAME + VPA; **REMARK is a category hint** |
| Card POS | `at MYNTRA DESIGNS PVT L on` | between `at` and `on` |
| Refund | `Refund … from ZOMATO has been credited` | before `has been` |

**Remark field:** strip trailing `.AVL` (glued "Avl Bal"). Remark keywords map directly: `RENT`→Housing, `LIC|LIC\d`→Insurance, `NPS`→Investments, `LOAN|EMI`→EMI & Loans, `CAR`→Vehicle, `TICKETS`→Travel, `CHASMA`→Health, `BED`→Home & Furniture, `COOK`→Home Services, `CASHWITHDRAWAL|WITHDRAW`→Cash, `MUMMY|MUM|MAA`→Family Transfer. Generic `UPI`, `FOR`, `TEST` → ignore. Remark rule outranks seed dictionary, loses to user rule.

## 4. Normalization additions
- Strip suffixes: ` FROM PAYTM BALANCE`, ` FROM PAYTM WALLET`, ` HAS BEEN …`, ` FOR RS \d+`, ` FOR INR \d+`, ` IN BANGALORE`, ` INR BANGALORE IND`, ` MUMBAI IND`.
- Strip prefixes: `VPA `, `WWW `, `PAYTM-`, `UPI-`, `LTD-`, `TP-`.
- VPA handles: take local-part, drop `.PAYU`, `.RZP`, `.EBZ`, `.CF`, trailing digits (`BEWAKOOF1`, `ZOMATO1`, `BIGBASKET.99313006`).
- Compare with spaces removed, so `THE SOUL`, `THESOULE`, `THESOULEDSTORE` share one key.
- ICICI CC truncates to 8 chars: dictionary lookup is **prefix match** on the space-less key (min 5 chars).

## 5. Gateways / QR (24%)
`PAYTMQR*`, `Q\d+@YBL`, `GPAY-\d+@OKBIZAXIS`, `BHARATPE\d+@YESBANKLTD`, `PAYTM-\d+@PAYTM`, `*@AXL`, `*@PZ`, `RAZORPAY`, `PAYU`, `CASHFREE`, `EASEBUZZ`, `INSTAMOJO`, `CCAVENUE`, `BILLDESK`, `MSWIPE`, `EURONETGPAY`, `PHONEPEMERCHANT`, `GOOGLEPAY`, `OKBIZ*`.
- Each QR/VPA is a stable shop identity. Key user rules on the **full VPA**, not the gateway name, so tagging one QR once fixes every future payment to it.
- For online gateways (Razorpay/PayU/etc.), try order-SMS correlation: another SMS within ±10 min naming a brand → use that brand.
- Otherwise `needsReview`.

## 6. Persons
VPA local-part is 10 digits, or `name@ok(sbi|axis|icici|hdfcbank)|@ybl|@ibl|@paytm`, or `MR |MRS ` prefix, or 1–3 alpha words with no business token → `Personal Transfers`, `needsReview`. Business tokens that override: `ENTE|ENTER|AGENC|DISTRIBU|STORE|MART|TRADE|SONS|RETA|M S|SHREE|SAI|PVT|LTD`.

## 7. Seed dictionary
Use `assets/seeds/merchants_in.json` (regex rules, first hit wins, generated from this scan). Categories added beyond CLAUDE.md defaults: Kids, Alcohol, Clothing & Footwear, Online Shopping, Electronics, Home & Furniture, Jewellery & Watches, Personal Care, Home Services, Vehicle, Housing, Photo & Printing, Family Transfer, Wallet Top-up, Credit Card Payment, Dividend.

## 8. Fixtures to add
One redacted fixture per shape in §3 and per reject in §1/§2. Use the ICICI CC and Axis P2M messages from the user's screenshot as the first two.
