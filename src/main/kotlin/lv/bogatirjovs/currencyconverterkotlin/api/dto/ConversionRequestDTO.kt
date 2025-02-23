package lv.bogatirjovs.currencyconverterkotlin.api.dto

import java.math.BigDecimal

data class ConversionRequestDTO(
    val amount: BigDecimal,
    val fromCurrency: String,
    val toCurrency: String
)