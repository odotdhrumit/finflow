package com.example.parser

object CategoryKeywordMatcher {

    private val keywordCategoryMap = mapOf(
        "Food" to listOf(
            "zomato", "swiggy", "ubereats", "mcdonalds", "kfc", "dominos", "pizza",
            "burger", "restaurant", "cafe", "starbucks", "diner", "bakery",
            "biryani", "food", "tea", "coffee", "bhojanalaya"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "zara", "h&m", "hnm",
            "nykaa", "tatacliq", "store", "mart", "retail", "clothing", "apparel",
            "supermarket", "dmart", "reliance", "bigbasket", "blinkit", "zepto", "instamart"
        ),
        "Transport" to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "railway", "fuel", "petrol",
            "diesel", "hpcl", "bpcl", "iocl", "shell", "fastag", "toll", "parking",
            "auto", "cab", "flight", "indigo", "airindia"
        ),
        "Bills" to listOf(
            "electricity", "power", "bescom", "tneb", "mahadiscom", "cesc", "water",
            "gas", "cylinder", "indane", "bharatgas", "airtel", "jio", "vi", "vodafone",
            "broadband", "wifi", "recharge", "billdesk", "bbps"
        ),
        "Entertainment" to listOf(
            "netflix", "prime", "spotify", "hotstar", "disney", "youtube", "pvr",
            "inox", "cinepolis", "movie", "bookmyshow", "playstation", "steam",
            "gaming", "apple.com/bill", "itunes"
        ),
        "Health" to listOf(
            "pharmacy", "apollo", "medplus", "1mg", "pharmeasy", "hospital", "clinic",
            "doctor", "dentist", "diagnostic", "pathology", "lab", "medicine"
        ),
        "Salary" to listOf(
            "salary", "payroll", "stipend", "wages", "bonus", "dividend", "interest"
        ),
        "Education" to listOf(
            "school", "college", "university", "fee", "tuition", "course", "udemy",
            "coursera", "book", "stationery"
        ),
        "Home" to listOf(
            "rent", "maintenance", "ikea", "furniture", "hardware", "plumber",
            "electrician", "urban company", "homelane", "livspace"
        ),
        "Investment" to listOf(
            "zerodha", "groww", "angelone", "upstox", "kuvera", "indmoney", "mutual fund",
            "sip", "shares", "bse", "nse", "camsonline", "kfintech"
        ),
        "Cash Withdrawal" to listOf(
            "atm", "cash wdl", "cash withdrawal", "atm wdl", "nfs"
        )
    )

    fun suggestCategory(text: String, isIncome: Boolean = false): String {
        if (isIncome) {
            val lower = text.lowercase()
            if (lower.contains("salary") || lower.contains("payroll") || lower.contains("wages")) {
                return "Salary"
            }
            if (lower.contains("refund") || lower.contains("cashback")) {
                return "Bills"
            }
            if (lower.contains("dividend") || lower.contains("interest")) {
                return "Investment"
            }
            return "Salary"
        }

        val normalized = text.lowercase()
        for ((category, keywords) in keywordCategoryMap) {
            for (kw in keywords) {
                if (normalized.contains(kw)) {
                    return category
                }
            }
        }
        return "Other"
    }

    fun getDefaultCategories(): List<com.example.data.entity.Category> {
        return listOf(
            com.example.data.entity.Category(name = "Food", iconName = "Restaurant", colorHex = "#F59E0B", isDefault = true),
            com.example.data.entity.Category(name = "Shopping", iconName = "ShoppingBag", colorHex = "#EC4899", isDefault = true),
            com.example.data.entity.Category(name = "Transport", iconName = "DirectionsCar", colorHex = "#3B82F6", isDefault = true),
            com.example.data.entity.Category(name = "Bills", iconName = "Receipt", colorHex = "#EF4444", isDefault = true),
            com.example.data.entity.Category(name = "Home", iconName = "Home", colorHex = "#8B5CF6", isDefault = true),
            com.example.data.entity.Category(name = "Education", iconName = "School", colorHex = "#06B6D4", isDefault = true),
            com.example.data.entity.Category(name = "Health", iconName = "LocalHospital", colorHex = "#10B981", isDefault = true),
            com.example.data.entity.Category(name = "Entertainment", iconName = "Movie", colorHex = "#F43F5E", isDefault = true),
            com.example.data.entity.Category(name = "Salary", iconName = "AttachMoney", colorHex = "#10B981", isIncome = true, isDefault = true),
            com.example.data.entity.Category(name = "Investment", iconName = "TrendingUp", colorHex = "#6366F1", isDefault = true),
            com.example.data.entity.Category(name = "Loan", iconName = "AccountBalance", colorHex = "#D97706", isDefault = true),
            com.example.data.entity.Category(name = "Savings", iconName = "Savings", colorHex = "#14B8A6", isDefault = true),
            com.example.data.entity.Category(name = "Cash Withdrawal", iconName = "Atm", colorHex = "#64748B", isDefault = true),
            com.example.data.entity.Category(name = "Transfer", iconName = "SwapHoriz", colorHex = "#8B5CF6", isDefault = true),
            com.example.data.entity.Category(name = "Other", iconName = "MoreHoriz", colorHex = "#94A3B8", isDefault = true)
        )
    }
}
