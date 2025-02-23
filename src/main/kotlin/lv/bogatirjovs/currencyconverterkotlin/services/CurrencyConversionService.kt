package lv.bogatirjovs.currencyconverterkotlin.services

import lv.bogatirjovs.currencycalculatorkotlin.jooq.tables.pojos.CurrencyConversion
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionPageResponseDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionRequestDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionResultDTO
import lv.bogatirjovs.currencyconverterkotlin.enums.ConversionStatus
import lv.bogatirjovs.currencyconverterkotlin.exceptions.DaoConversionFetchException
import lv.bogatirjovs.currencyconverterkotlin.exceptions.DaoConversionInsertionException
import lv.bogatirjovs.currencyconverterkotlin.repositories.CurrencyConversionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.math.ceil

@Service
class CurrencyConversionService(
    private val repository: CurrencyConversionRepository,
    private val externalRateService: ExternalRateService,

    @Value("\${currency-conversion.defaultFee}")
    private val defaultFee: BigDecimal

) {
    private val logger = LoggerFactory.getLogger(CurrencyConversionService::class.java)
    fun createConversion(request: ConversionRequestDTO): CurrencyConversion {
        val conversion = CurrencyConversion(
            id = null,
            amount = request.amount,
            fromCurrency = request.fromCurrency,
            toCurrency = request.toCurrency,
            fee = defaultFee,
            status = ConversionStatus.PENDING.statusName,
            conversionRate = null,
            result = null,
            createdAt = LocalDateTime.now()
        )
        return conversion
    }

    fun processConversionRequest(request: ConversionRequestDTO): Long {
        val conversion = createConversion(request)
        repository.insert(conversion)
        val conversionId = conversion.id ?: throw DaoConversionInsertionException(CurrencyConversion::class.simpleName)
        return conversionId
    }

    @Async
    fun processConversionAsync(conversionId: Long, amount: BigDecimal, fromCurrency: String, toCurrency: String) {
        try {
            //Thread.sleep(10000)
            val rate = externalRateService.getConversionRate(fromCurrency, toCurrency)
            val result = externalRateService.convertAmount(amount, defaultFee, rate)

            val conversion = repository.fetchOneById(conversionId)
                ?: throw DaoConversionFetchException(CurrencyConversion::class.simpleName)

            val updatedConversion = conversion.copy(
                status = ConversionStatus.DONE.statusName,
                conversionRate = rate,
                result = result
            )
            repository.update(updatedConversion)
        } catch (ex: Exception) {
            logger.error("Error processing conversion id $conversionId", ex)
            repository.fetchOneById(conversionId)?.let { conversion ->
                val updatedConversion = conversion.copy(status = ConversionStatus.FAILED.statusName)
                repository.update(updatedConversion)
            }
        }
    }

    fun getConversionResult(conversionId: Long): ConversionResultDTO {
        val conversion = repository.fetchOneById(conversionId)
            ?: throw DaoConversionFetchException(CurrencyConversion::class.simpleName)
        return conversionToResultDTO(conversion)
    }

    fun conversionToResultDTO(conversion: CurrencyConversion): ConversionResultDTO {
        return ConversionResultDTO(
            id = conversion.id!!,
            amount = conversion.amount!!,
            fromCurrency = conversion.fromCurrency!!,
            toCurrency = conversion.toCurrency!!,
            fee = conversion.fee!!,
            status = conversion.status!!,
            conversionRate = if (conversion.status == ConversionStatus.DONE.statusName) conversion.conversionRate else null,
            result = if (conversion.status == ConversionStatus.DONE.statusName) conversion.result else null,
            createdAt = conversion.createdAt!!
        )
    }

    fun getConversionPage(
        page: Int,
        size: Int,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): ConversionPageResponseDTO {
        val offset = page * size

        val conversions: List<CurrencyConversion> = repository.fetchConversions(offset, size, startTime, endTime)
        val totalElements: Long = repository.countConversions(startTime, endTime)
        var totalPages = 0
        if (size > 0) {
            totalPages = ceil(totalElements.toDouble() / size).toInt()
        }

        val conversionDTOs = conversions.map { conversion ->
            conversionToResultDTO(conversion)
        }

        return ConversionPageResponseDTO(
            conversions = conversionDTOs,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages
        )
    }
}