package lv.bogatirjovs.currencyconverterkotlin.api.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ConversionResultDTO(
    val id: Long,
    val amount: BigDecimal,
    val fromCurrency: String,
    val toCurrency: String,
    val fee: BigDecimal,
    val status: String,
    val conversionRate: BigDecimal? = null,
    val result: BigDecimal? = null,
    val createdAt: LocalDateTime
)