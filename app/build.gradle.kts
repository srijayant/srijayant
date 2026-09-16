import java.text.Normalizer
import java.util.Locale

plugins {
    id("com.android.application")
}

fun normalizeMerchantSeed(value: String): String {
    var normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .uppercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()
    normalized = normalized
        .replace(Regex("^(?:VPA |WWW |PAYTM-|UPI-|LTD-|TP-)+"), "")
        .replace(
            Regex(
                "\\s+(?:FROM PAYTM (?:BALANCE|WALLET)|HAS BEEN.*|FOR (?:RS|INR)\\s*[\\d,.]+|IN BANGALORE|INR BANGALORE IND|MUMBAI IND)$"
            ),
            ""
        )
    if (normalized.contains("@")) {
        normalized = normalized.substringBefore("@")
            .replace(Regex("\\.(?:PAYU|RZP|EBZ|CF)$"), "")
    }
    normalized = normalized
        .replace(Regex("(?:-ORDER)?\\d+$"), "")
        .replace(Regex("[^A-Z0-9]"), "")
    return when {
        normalized == "THESOUL" || normalized.startsWith("THESOULE") -> "THESOULE"
        normalized.startsWith("ZOMATO") -> "ZOMATO"
        else -> normalized
    }
}

fun jsonEscape(value: String): String = buildString {
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
}

val generateMerchantSeeds by tasks.registering {
    val input = rootProject.file("tools/merchant_review.csv")
    val output = file("src/main/assets/seeds/merchant_exact.json")
    inputs.file(input)
    outputs.file(output)
    doLast {
        val entries = linkedMapOf<String, Pair<String, String>>()
        input.useLines { lines ->
            lines.drop(1).forEach { line ->
                val columns = line.split(",", limit = 4)
                if (columns.size == 4) {
                    val merchant = columns[0].trim()
                    val bucket = columns[1].trim()
                    val category = columns[2].trim()
                    val userFix = columns[3].trim()
                    if (bucket != "noise" && !(bucket.startsWith("you_tag_") && userFix.isEmpty())) {
                        val key = normalizeMerchantSeed(merchant)
                        val resolvedCategory = userFix.ifEmpty { category }
                        if (key.isNotEmpty() && resolvedCategory.isNotEmpty() && resolvedCategory != "—") {
                            entries.putIfAbsent(key, resolvedCategory to bucket)
                        }
                    }
                }
            }
        }
        output.parentFile.mkdirs()
        output.writeText(buildString {
            append("{\n")
            entries.entries.forEachIndexed { index, (key, value) ->
                append("  \"").append(jsonEscape(key)).append("\": {\"category\": \"")
                    .append(jsonEscape(value.first)).append("\", \"bucket\": \"")
                    .append(jsonEscape(value.second)).append("\"}")
                if (index < entries.size - 1) append(',')
                append('\n')
            }
            append("}\n")
        })
    }
}

android {
    namespace = "com.srijayant.spendscope"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.srijayant.spendscope"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateMerchantSeeds)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
