package com.example.amarhishab

data class Transaction(
    val id: Long = 0,
    val date: String,
    val type: String, // INCOME or EXPENSE
    val description: String,
    val category: String,
    val amount: Double
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "date" to date, "type" to type,
        "description" to description, "category" to category, "amount" to amount
    )
}
