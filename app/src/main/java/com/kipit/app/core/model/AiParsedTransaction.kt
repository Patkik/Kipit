package com.kipit.app.core.model

data class AiParsedTransaction(
    val amountCentavos: Long,
    val mode: String = "Expense",
    val merchantOrNote: String = "",
    val categoryName: String = "",
    val walletName: String = "",
    val destinationWalletName: String? = null,
    val splitCount: Int? = null,
    val debtorName: String? = null,
    val isAiGenerated: Boolean = true
)
