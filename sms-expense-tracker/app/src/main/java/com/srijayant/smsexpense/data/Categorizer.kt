package com.srijayant.smsexpense.data

/**
 * Assigns a spending [Category] by keyword-matching the merchant name and the
 * SMS body. Order matters: more specific categories are checked first.
 */
object Categorizer {

    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.FOOD to listOf(
            "swiggy", "zomato", "dominos", "domino's", "mcdonald", "kfc", "pizza",
            "burger", "cafe", "restaurant", "eatery", "dhaba", "biryani", "starbucks",
            "barbeque", "haldiram", "eatsure", "faasos", "dunkin", "subway"
        ),
        Category.GROCERIES to listOf(
            "bigbasket", "blinkit", "grofers", "zepto", "dmart", "d-mart", "instamart",
            "jiomart", "grocery", "supermarket", "kirana", "reliance fresh", "more retail",
            "nature's basket", "milk", "dairy"
        ),
        Category.FUEL to listOf(
            "petrol", "diesel", "fuel", "hpcl", "iocl", "bpcl", "indian oil",
            "bharat petroleum", "hindustan petroleum", "shell", "filling station"
        ),
        Category.TRAVEL to listOf(
            "uber", "ola", "rapido", "irctc", "railway", "redbus", "makemytrip",
            "goibibo", "cleartrip", "yatra", "indigo", "air india", "vistara", "spicejet",
            "akasa", "airlines", "metro", "fastag", "toll", "oyo", "airbnb", "hotel",
            "travels", "cab"
        ),
        Category.BILLS to listOf(
            "electricity", "msedcl", "bescom", "tneb", "water bill", "gas bill",
            "broadband", "wifi", "airtel", "jio", "vodafone", " vi ", "bsnl", "recharge",
            "dth", "tata sky", "tatasky", "postpaid", "prepaid", "bill payment", "bbps",
            "billdesk", "mahanagar gas", "indane", "hp gas", "society maintenance"
        ),
        Category.ENTERTAINMENT to listOf(
            "netflix", "prime video", "hotstar", "disney", "spotify", "youtube",
            "bookmyshow", "pvr", "inox", "cinepolis", "gaming", "playstation", "steam",
            "sonyliv", "zee5", "jiocinema"
        ),
        Category.HEALTH to listOf(
            "pharmacy", "pharmeasy", "1mg", "netmeds", "apollo", "hospital", "clinic",
            "diagnostic", "lab test", "medplus", "practo", "cult.fit", "cultfit", "gym",
            "medical"
        ),
        Category.EDUCATION to listOf(
            "school", "college", "university", "udemy", "coursera", "upgrad", "byjus",
            "unacademy", "tuition", "course fee", "exam fee", "bits pilani"
        ),
        Category.RENT to listOf(
            "rent", "nobroker pay", "landlord", "housing.com", "lease"
        ),
        Category.INVESTMENT to listOf(
            "zerodha", "groww", "upstox", "kuvera", "coin", "mutual fund", "sip",
            "nps", "ppf", "fd booked", "rd installment", "etmoney", "indmoney",
            "smallcase", "stock", "demat"
        ),
        Category.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "snapdeal",
            "tata cliq", "croma", "reliance digital", "vijay sales", "decathlon", "ikea",
            "lifestyle", "shoppers stop", "pantaloons", "westside", "zudio", "mall",
            "store", "retail", "shop"
        ),
        Category.ATM to listOf(
            "atm", "cash withdrawal", "cash wdl", "csh wdl", "self cheque"
        )
    )

    fun categorize(merchant: String, body: String, type: TransactionType): Category {
        val haystack = "${merchant.lowercase()} ${body.lowercase()}"
        for ((category, keywords) in rules) {
            if (keywords.any { haystack.contains(it) }) return category
        }
        // Untagged UPI person-to-person payments and bank transfers.
        if (haystack.contains("upi") || haystack.contains("neft") ||
            haystack.contains("imps") || haystack.contains("rtgs")
        ) return Category.TRANSFER

        return if (type == TransactionType.CREDIT) Category.TRANSFER else Category.OTHER
    }
}
