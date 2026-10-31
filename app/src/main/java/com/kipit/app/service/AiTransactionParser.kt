package com.kipit.app.service

import com.kipit.app.core.model.AiParsedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object AiTransactionParser {

    private const val DEFAULT_MODEL = "gemini-2.5-flash"

    suspend fun parse(
        prompt: String,
        availableWallets: List<String>,
        availableCategories: List<String>,
        apiKey: String?,
        model: String = DEFAULT_MODEL
    ): AiParsedTransaction = withContext(Dispatchers.IO) {
        if (!apiKey.isNullOrBlank()) {
            val geminiResult = parseWithGemini(prompt, availableWallets, availableCategories, apiKey, model)
            if (geminiResult.isSuccess) {
                return@withContext geminiResult.getOrThrow()
            }
        }
        parseOfflineFallback(prompt, availableWallets, availableCategories)
    }

    suspend fun parseWithGemini(
        prompt: String,
        availableWallets: List<String>,
        availableCategories: List<String>,
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): Result<AiParsedTransaction> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val targetModel = model.ifBlank { DEFAULT_MODEL }
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"
            val url = URL(endpoint)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val systemInstruction = "You are Kipit's AI transaction parser. Your job is to extract financial transaction data from natural language. Output MUST be strict JSON with keys: amount (float, e.g. 150.50), mode ('Expense', 'Income', 'Split', 'Transfer', 'Park'), merchantOrNote (string), category (pick best from available categories or provide an intuitive label), wallet (pick best matching wallet), destinationWallet (if transfer), splitCount (if split, integer, default 2), debtor (if split, name of debtor). Available Wallets: [${availableWallets.joinToString(", ")}]. Available Categories: [${availableCategories.joinToString(", ")}]."

            // Escape strings for JSON payload
            val escapedPrompt = escapeJson(prompt)
            val escapedSysInst = escapeJson(systemInstruction)

            val requestJson = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": "$escapedPrompt" }
                      ]
                    }
                  ],
                  "systemInstruction": {
                    "parts": [
                      { "text": "$escapedSysInst" }
                    ]
                  },
                  "generationConfig": {
                    "responseMimeType": "application/json",
                    "temperature": 0.1
                  }
                }
            """.trimIndent()

            conn.outputStream.use { os ->
                os.write(requestJson.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                return@withContext Result.failure(Exception("Gemini API error ($responseCode): $errorStream"))
            }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            
            // Extract the generated text from candidates
            val textPartRegex = Regex("""\"text\"\s*:\s*\"((?:\\\"|[^\"])*)\"""")
            val textMatch = textPartRegex.find(responseText)
            val generatedText = if (textMatch != null) {
                unescapeJson(textMatch.groupValues[1])
            } else {
                responseText
            }

            val parsed = parseJsonPayload(generatedText, availableWallets, availableCategories)
            if (parsed != null) {
                Result.success(parsed)
            } else {
                Result.failure(Exception("Could not parse AI response JSON"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    fun parseJsonPayload(
        rawJson: String,
        availableWallets: List<String>,
        availableCategories: List<String>
    ): AiParsedTransaction? {
        return try {
            val cleanJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val amountStr = extractJsonValue(cleanJson, "amount") ?: return null
            val amountDouble = amountStr.toDoubleOrNull() ?: 0.0
            val amountCentavos = Math.round(amountDouble * 100.0)

            val modeRaw = extractJsonValue(cleanJson, "mode") ?: "Expense"
            val mode = when (modeRaw.lowercase(Locale.US)) {
                "income" -> "Income"
                "split" -> "Split"
                "transfer" -> "Transfer"
                "park" -> "Park"
                else -> "Expense"
            }

            val rawCategory = extractJsonValue(cleanJson, "category")?.trim() ?: ""
            val matchedCategory = availableCategories.find { it.equals(rawCategory, ignoreCase = true) }
                ?: rawCategory.ifBlank { if (mode == "Income") "Income" else "Food" }

            val rawWallet = extractJsonValue(cleanJson, "wallet")?.trim() ?: ""
            val sortedWallets = availableWallets.sortedByDescending { it.length }
            val matchedWallet = sortedWallets.find { it.equals(rawWallet, ignoreCase = true) }
                ?: sortedWallets.find { rawWallet.contains(it, ignoreCase = true) }
                ?: sortedWallets.firstOrNull() ?: "Cash"

            val rawDest = extractJsonValue(cleanJson, "destinationWallet")?.trim()
            val matchedDest = if (!rawDest.isNullOrBlank() && rawDest != "null") {
                sortedWallets.find { it.equals(rawDest, ignoreCase = true) }
                    ?: sortedWallets.find { it != matchedWallet }
            } else null

            val merchantOrNote = extractJsonValue(cleanJson, "merchantOrNote")?.trim() ?: ""
            val splitCountStr = extractJsonValue(cleanJson, "splitCount")
            val splitCount = splitCountStr?.toIntOrNull() ?: if (mode == "Split") 2 else null

            val debtor = extractJsonValue(cleanJson, "debtor")?.trim()?.takeIf { it != "null" && it.isNotBlank() }

            AiParsedTransaction(
                amountCentavos = amountCentavos,
                mode = mode,
                merchantOrNote = merchantOrNote,
                categoryName = matchedCategory,
                walletName = matchedWallet,
                destinationWalletName = matchedDest,
                splitCount = splitCount,
                debtorName = debtor,
                isAiGenerated = true
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = Regex("""\"$key\"\s*:\s*(?:\"((?:\\\"|[^\"])*)\"|([0-9\.]+)|(null)|(true|false))""")
        val match = pattern.find(json) ?: return null
        val stringVal = match.groups[1]?.value
        if (stringVal != null) {
            return unescapeJson(stringVal)
        }
        val numVal = match.groups[2]?.value
        if (numVal != null) return numVal
        return null
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(s: String): String {
        return s.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    fun parseOfflineFallback(
        prompt: String,
        availableWallets: List<String>,
        availableCategories: List<String>
    ): AiParsedTransaction {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase(Locale.US)

        // 1. Detect Mode
        val mode = when {
            Regex("""\b(split|ambag|hati)\b""").containsMatchIn(lower) -> "Split"
            Regex("""\b(income|salary|sweldo|sahod|freelance|earnings|deposit)\b""").containsMatchIn(lower) -> "Income"
            Regex("""\b(transfer|lipat|send to|move)\b""").containsMatchIn(lower) -> "Transfer"
            Regex("""\b(park|ipon|save for later|impulse)\b""").containsMatchIn(lower) -> "Park"
            else -> "Expense"
        }

        // 2. Extract Amount
        val amountRegex = Regex("""(?:(?:php|₱|p)\s*)?(\d{1,8}(?:\.\d{1,2})?)\b""", RegexOption.IGNORE_CASE)
        val match = amountRegex.findAll(trimmed).lastOrNull()
        val amountCentavos = if (match != null) {
            val numStr = match.groupValues[1]
            (numStr.toDoubleOrNull()?.times(100.0))?.toLong() ?: 0L
        } else 0L

        // 3. Match Wallet (sort descending by length so GCash is matched before Cash)
        val sortedWallets = availableWallets.sortedByDescending { it.length }
        val matchedWallet = sortedWallets.find { lower.contains(it.lowercase(Locale.US)) }
            ?: when {
                lower.contains("gcash") -> sortedWallets.find { it.contains("gcash", ignoreCase = true) }
                lower.contains("maya") || lower.contains("paymaya") -> sortedWallets.find { it.contains("maya", ignoreCase = true) }
                lower.contains("bpi") -> sortedWallets.find { it.contains("bpi", ignoreCase = true) }
                lower.contains("bdo") -> sortedWallets.find { it.contains("bdo", ignoreCase = true) }
                lower.contains("bank") -> sortedWallets.find { it.contains("bank", ignoreCase = true) }
                lower.contains("cash") -> sortedWallets.find { it.contains("cash", ignoreCase = true) }
                else -> null
            } ?: sortedWallets.firstOrNull() ?: "Cash"

        // 4. Match Destination Wallet (for Transfer)
        val destinationWallet = if (mode == "Transfer") {
            sortedWallets.find { it != matchedWallet && lower.contains(it.lowercase(Locale.US)) }
                ?: sortedWallets.firstOrNull { it != matchedWallet }
        } else null

        // 5. Match or Infer Category
        val matchedCategory = when (mode) {
            "Income" -> {
                when {
                    lower.contains("video") -> "Video editing income"
                    lower.contains("freelance") -> "Freelance income"
                    lower.contains("salary") || lower.contains("sweldo") -> "Salary"
                    else -> availableCategories.find { it.contains("Income", ignoreCase = true) } ?: "Income"
                }
            }
            else -> {
                val sortedCategories = availableCategories.sortedByDescending { it.length }
                sortedCategories.find { lower.contains(it.lowercase(Locale.US)) }
                    ?: when {
                        Regex("""\b(food|lunch|dinner|breakfast|snack|coffee|jollibee|mcdo|mcdonalds|starbucks|restaurant|cafe|burger|pizza|ramen)\b""").containsMatchIn(lower) ->
                            availableCategories.find { it.equals("Food", ignoreCase = true) } ?: "Food"
                        Regex("""\b(transport|transportation|grab|taxi|fare|gas|angkas|joyride|mrt|lrt|jeep|bus|commute)\b""").containsMatchIn(lower) ->
                            availableCategories.find { it.equals("Transportation", ignoreCase = true) } ?: "Transportation"
                        Regex("""\b(utility|utilities|bill|electric|meralco|water|maynilad|wifi|internet)\b""").containsMatchIn(lower) ->
                            availableCategories.find { it.equals("Utilities", ignoreCase = true) } ?: "Utilities"
                        Regex("""\b(grocery|groceries|supermarket|market|puregold|sm|robinsons)\b""").containsMatchIn(lower) ->
                            availableCategories.find { it.equals("Groceries", ignoreCase = true) } ?: "Groceries"
                        Regex("""\b(game|tech|gaming|steam|ps5|gadget|phone)\b""").containsMatchIn(lower) ->
                            availableCategories.find { it.equals("Tech & Gaming", ignoreCase = true) } ?: "Tech & Gaming"
                        else -> availableCategories.firstOrNull() ?: "General"
                    }
            }
        }

        // 6. Split Debtor
        var debtor: String? = null
        var splitCount: Int? = null
        if (mode == "Split") {
            splitCount = 2
            val withMatch = Regex("""(?:with|kay|ni)\s+([a-zA-Z]+)""", RegexOption.IGNORE_CASE).find(trimmed)
            if (withMatch != null) {
                debtor = withMatch.groupValues[1].replaceFirstChar { it.uppercase() }
            }
        }

        // 7. Clean Merchant / Note
        var note = trimmed
        if (match != null) {
            note = note.replace(match.value, "")
        }
        val keywordsToRemove = listOf("split with", "with", "kay", "ni", "cash", "gcash", "bpi", "maya", "bdo", "transfer to", "from", "at", "for")
        for (kw in keywordsToRemove) {
            note = note.replace(Regex("""\b$kw\b""", RegexOption.IGNORE_CASE), "")
        }
        note = note.replace(Regex("""\s+"""), " ").trim()
        if (note.isBlank()) {
            note = when (mode) {
                "Income" -> "Income"
                "Transfer" -> "Transfer"
                "Split" -> "Shared expense"
                else -> "Expense"
            }
        }

        return AiParsedTransaction(
            amountCentavos = amountCentavos,
            mode = mode,
            merchantOrNote = note,
            categoryName = matchedCategory,
            walletName = matchedWallet,
            destinationWalletName = destinationWallet,
            splitCount = splitCount,
            debtorName = debtor,
            isAiGenerated = false
        )
    }
}
