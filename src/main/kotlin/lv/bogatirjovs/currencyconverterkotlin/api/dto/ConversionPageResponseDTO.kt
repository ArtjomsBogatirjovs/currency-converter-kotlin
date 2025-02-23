package lv.bogatirjovs.currencyconverterkotlin.api.dto

data class ConversionPageResponseDTO(
    val conversions: List<ConversionResultDTO>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)