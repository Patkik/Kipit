package com.kipit.app

import com.kipit.app.service.AiTransactionParser
import org.junit.Assert.*
import org.junit.Test

class AiTransactionParserTest {

    private val sampleWallets = listOf("Cash", "GCash", "BPI", "Maya")
    private val sampleCategories = listOf("Food", "Transportation", "Utilities", "Groceries", "Tech & Gaming")

    @Test
    fun testOfflineFallback_simpleExpense() {
        val result = AiTransactionParser.parseOfflineFallback(
            prompt = "Lunch at Jollibee 250 cash",
            availableWallets = sampleWallets,
            availableCategories = sampleCategories
        )

        assertEquals(25000L, result.amountCentavos)
        assertEquals("Expense", result.mode)
        assertEquals("Cash", result.walletName)
        assertEquals("Food", result.categoryName)
        assertTrue(result.merchantOrNote.contains("Jollibee", ignoreCase = true))
    }

    @Test
    fun testOfflineFallback_income() {
        val result = AiTransactionParser.parseOfflineFallback(
            prompt = "Video editing freelance income 15000 BPI",
            availableWallets = sampleWallets,
            availableCategories = sampleCategories
        )

        assertEquals(1500000L, result.amountCentavos)
        assertEquals("Income", result.mode)
        assertEquals("BPI", result.walletName)
        assertTrue(result.categoryName.contains("Income", ignoreCase = true) || result.categoryName.contains("Video", ignoreCase = true))
    }

    @Test
    fun testOfflineFallback_splitExpense() {
        val result = AiTransactionParser.parseOfflineFallback(
            prompt = "Dinner 800 gcash split with John",
            availableWallets = sampleWallets,
            availableCategories = sampleCategories
        )

        assertEquals(80000L, result.amountCentavos)
        assertEquals("Split", result.mode)
        assertEquals("GCash", result.walletName)
        assertEquals("John", result.debtorName)
        assertEquals(2, result.splitCount)
    }

    @Test
    fun testJsonParsing_validResponse() {
        val json = """
            {
                "amount": 350.50,
                "mode": "Expense",
                "merchantOrNote": "Grab taxi ride",
                "category": "Transportation",
                "wallet": "GCash",
                "destinationWallet": null,
                "splitCount": null,
                "debtor": null
            }
        """.trimIndent()

        val parsed = AiTransactionParser.parseJsonPayload(json, sampleWallets, sampleCategories)
        assertNotNull(parsed)
        assertEquals(35050L, parsed!!.amountCentavos)
        assertEquals("Expense", parsed.mode)
        assertEquals("Grab taxi ride", parsed.merchantOrNote)
        assertEquals("Transportation", parsed.categoryName)
        assertEquals("GCash", parsed.walletName)
    }

    @Test
    fun testJsonParsing_withMarkdownCodeBlock() {
        val jsonWithMarkdown = """
            ```json
            {
                "amount": 1200.0,
                "mode": "Split",
                "merchantOrNote": "Buffet dinner",
                "category": "Food",
                "wallet": "BPI",
                "destinationWallet": null,
                "splitCount": 3,
                "debtor": "Sarah and Dave"
            }
            ```
        """.trimIndent()

        val parsed = AiTransactionParser.parseJsonPayload(jsonWithMarkdown, sampleWallets, sampleCategories)
        assertNotNull(parsed)
        assertEquals(120000L, parsed!!.amountCentavos)
        assertEquals("Split", parsed.mode)
        assertEquals("Food", parsed.categoryName)
        assertEquals("BPI", parsed.walletName)
        assertEquals(3, parsed.splitCount)
        assertEquals("Sarah and Dave", parsed.debtorName)
    }
}
