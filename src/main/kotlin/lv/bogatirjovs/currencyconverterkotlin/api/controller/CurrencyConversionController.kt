package lv.bogatirjovs.currencyconverterkotlin.api.controller

import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionPageResponseDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionRequestDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionResponseDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionResultDTO
import lv.bogatirjovs.currencyconverterkotlin.services.CurrencyConversionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/conversion")
class CurrencyConversionController(
    private val conversionService: CurrencyConversionService
) {

    companion object {
        const val DEFAULT_PAGE_SIZE = "20"
    }

    @PostMapping
    fun createConversion(
        @RequestBody request: ConversionRequestDTO
    ): ResponseEntity<ConversionResponseDTO> {
        try {
            val conversionId = conversionService.processConversionRequest(request)
            conversionService.processConversionAsync(
                conversionId,
                request.amount,
                request.fromCurrency,
                request.toCurrency
            )
            return ResponseEntity.ok(ConversionResponseDTO(conversionId))
        } catch (ex: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{id}")
    fun getConversionResult(
        @PathVariable id: Long
    ): ResponseEntity<ConversionResultDTO> {
        try {
            val result = conversionService.getConversionResult(id)
            return ResponseEntity.ok(result)
        } catch (ex: Exception) {
            return ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/page")
    fun getConversionPage(
        @RequestParam(required = false, defaultValue = "0")
        page: Int,
        @RequestParam(required = false, defaultValue = DEFAULT_PAGE_SIZE)
        size: Int,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startTime: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endTime: LocalDateTime?
    ): ResponseEntity<ConversionPageResponseDTO> {
        val result = conversionService.getConversionPage(page, size, startTime, endTime)
        return ResponseEntity.ok(result)
    }
}