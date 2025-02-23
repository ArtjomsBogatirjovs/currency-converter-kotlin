package lv.bogatirjovs.currencyconverterkotlin.services

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import lv.bogatirjovs.currencycalculatorkotlin.jooq.tables.pojos.CurrencyConversion
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionPageResponseDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionRequestDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionResultDTO
import lv.bogatirjovs.currencyconverterkotlin.enums.ConversionStatus
import lv.bogatirjovs.currencyconverterkotlin.exceptions.DaoConversionFetchException
import lv.bogatirjovs.currencyconverterkotlin.exceptions.DaoConversionInsertionException
import lv.bogatirjovs.currencyconverterkotlin.repositories.CurrencyConversionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.math.ceil

class CurrencyConversionServiceTest {

    @RelaxedMockK
    private lateinit var repository: CurrencyConversionRepository

    @MockK
    private lateinit var externalRateService: ExternalRateService

    private lateinit var service: CurrencyConversionService

    private val fee = BigDecimal("0.01")

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service = CurrencyConversionService(repository, externalRateService, fee)
    }

    @Test
    fun `createConversion returns valid conversion object`() {
        val request = ConversionRequestDTO(
            amount = BigDecimal("100.00"),
            fromCurrency = "USD",
            toCurrency = "EUR"
        )

        val conversion: CurrencyConversion = service.createConversion(request)

        assertNull(conversion.id)
        assertEquals(request.amount, conversion.amount)
        assertEquals(request.fromCurrency, conversion.fromCurrency)
        assertEquals(request.toCurrency, conversion.toCurrency)
        assertEquals(conversion.fee, fee)
        assertEquals(ConversionStatus.PENDING.statusName, conversion.status)
        assertNull(conversion.conversionRate)
        assertNull(conversion.result)
        assertNotNull(conversion.createdAt)
    }

    @Test
    fun `processConversionRequest returns an id when insertion succeeds`() {
        val request = ConversionRequestDTO(
            amount = BigDecimal("100.00"),
            fromCurrency = "USD",
            toCurrency = "EUR"
        )

        val slot = slot<CurrencyConversion>()
        every { repository.insert(capture(slot)) } answers {
            slot.captured.id = 1
        }

        val conversionId = service.processConversionRequest(request)

        assertEquals(1, conversionId)
        verify {
            repository.insert(match<CurrencyConversion> {
                it.status == ConversionStatus.PENDING.statusName
                        && it.amount == request.amount
                        && it.fromCurrency == request.fromCurrency
                        && it.toCurrency == request.toCurrency
            })
        }
    }

    @Test
    fun `processConversionRequest throws exception when id remains null`() {
        val request = ConversionRequestDTO(
            amount = BigDecimal("100.00"),
            fromCurrency = "USD",
            toCurrency = "EUR"
        )

        every { repository.insert(any<CurrencyConversion>()) } just Runs
        assertThrows<DaoConversionInsertionException> { service.processConversionRequest(request) }
    }

    @Test
    fun `processConversionAsync updates conversion to DONE on success`() {
        val conversionId = 1L
        val amount = BigDecimal("100.00")
        val fromCurrency = "USD"
        val toCurrency = "EUR"
        val rate = BigDecimal("1.2")

        val expectedResult = amount.multiply(BigDecimal.ONE.subtract(fee))
            .multiply(rate).setScale(6, RoundingMode.HALF_UP)

        every { externalRateService.getConversionRate(fromCurrency, toCurrency) } returns rate
        every { externalRateService.convertAmount(amount, fee, rate) } returns expectedResult

        val initialConversion = CurrencyConversion(
            id = conversionId,
            amount = amount,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            fee = fee,
            status = ConversionStatus.PENDING.statusName,
            conversionRate = null,
            result = null,
            createdAt = LocalDateTime.now()
        )
        every { repository.fetchOneById(conversionId) } returns initialConversion
        every { repository.update(any<CurrencyConversion>()) } just Runs

        service.processConversionAsync(conversionId, amount, fromCurrency, toCurrency)

        verify {
            repository.update(match<CurrencyConversion> {
                it.status == ConversionStatus.DONE.statusName &&
                        it.conversionRate == rate &&
                        it.result == expectedResult
            })
        }
    }

    @Test
    fun `processConversionAsync updates conversion to FAILED on exception`() {
        val conversionId = 2L
        val amount = BigDecimal("100.00")
        val fromCurrency = "USD"
        val toCurrency = "EUR"
        every {
            externalRateService.getConversionRate(
                fromCurrency,
                toCurrency
            )
        } throws RuntimeException("Service error")

        val initialConversion = CurrencyConversion(
            id = conversionId,
            amount = amount,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            fee = fee,
            status = ConversionStatus.PENDING.statusName,
            conversionRate = null,
            result = null,
            createdAt = LocalDateTime.now()
        )
        every { repository.fetchOneById(conversionId) } returns initialConversion
        every { repository.update(any<CurrencyConversion>()) } just Runs

        service.processConversionAsync(conversionId, amount, fromCurrency, toCurrency)

        verify { repository.update(match<CurrencyConversion> { it.status == ConversionStatus.FAILED.statusName }) }
    }

    @Test
    fun `getConversionResult returns DTO with conversionRate and result when status DONE`() {
        val conversionId = 3L
        val now = LocalDateTime.now()
        val conversion = CurrencyConversion(
            id = conversionId,
            amount = BigDecimal("100.00"),
            fromCurrency = "USD",
            toCurrency = "EUR",
            fee = fee,
            status = ConversionStatus.DONE.statusName,
            conversionRate = BigDecimal("1.2"),
            result = BigDecimal("118.8"),
            createdAt = now
        )
        every { repository.fetchOneById(conversionId) } returns conversion

        val dto: ConversionResultDTO = service.getConversionResult(conversionId)

        assertEquals(conversionId, dto.id)
        assertEquals(BigDecimal("100.00"), dto.amount)
        assertEquals("USD", dto.fromCurrency)
        assertEquals("EUR", dto.toCurrency)
        assertEquals(fee, dto.fee)
        assertEquals(ConversionStatus.DONE.statusName, dto.status)
        assertEquals(BigDecimal("1.2"), dto.conversionRate)
        assertEquals(BigDecimal("118.8"), dto.result)
        assertEquals(now, dto.createdAt)
    }

    @Test
    fun `getConversionResult throws exception when conversion not found`() {
        val conversionId = 4L
        every { repository.fetchOneById(conversionId) } returns null

        val ex = assertThrows(DaoConversionFetchException::class.java) {
            service.getConversionResult(conversionId)
        }
        assertTrue(ex.message!!.contains("CurrencyConversion"))
    }

    @Test
    fun `getConversionPage returns empty page when no conversions exist`() {
        val page = 0
        val size = 20
        val startTime: LocalDateTime? = null
        val endTime: LocalDateTime? = null

        every { repository.fetchConversions(any(), any(), any(), any()) } returns emptyList()
        every { repository.countConversions(any(), any()) } returns 0L

        val result: ConversionPageResponseDTO = service.getConversionPage(page, size, startTime, endTime)

        assertEquals(page, result.page)
        assertEquals(size, result.size)
        assertEquals(0L, result.totalElements)
        assertEquals(0, result.totalPages)
        assertTrue(result.conversions.isEmpty())
    }

    @Test
    fun `getConversionPage returns correct page data when conversions exist`() {
        val page = 1
        val size = 2
        val offset = page * size
        val startTime: LocalDateTime? = LocalDateTime.of(2022, 1, 1, 0, 0)
        val endTime: LocalDateTime? = LocalDateTime.of(2022, 12, 31, 23, 59)

        val conversion = CurrencyConversion(
            id = 2,
            amount = BigDecimal("150.00"),
            fromCurrency = "GBP",
            toCurrency = "USD",
            fee = fee,
            status = ConversionStatus.DONE.statusName,
            conversionRate = BigDecimal("1.3"),
            result = BigDecimal("195.0"),
            createdAt = LocalDateTime.of(2022, 7, 15, 12, 0)
        )

        every { repository.fetchConversions(offset, size, startTime, endTime) } returns listOf(conversion)
        every { repository.countConversions(startTime, endTime) } returns 3L

        val result: ConversionPageResponseDTO = service.getConversionPage(page, size, startTime, endTime)

        assertEquals(page, result.page)
        assertEquals(size, result.size)
        assertEquals(3L, result.totalElements)

        val expectedTotalPages = ceil(3.0 / size).toInt()

        assertEquals(expectedTotalPages, result.totalPages)

        assertEquals(1, result.conversions.size)

        val dto: ConversionResultDTO = result.conversions.first()

        assertEquals(conversion.id, dto.id)
        assertEquals(conversion.amount, dto.amount)
        assertEquals(conversion.fromCurrency, dto.fromCurrency)
        assertEquals(conversion.toCurrency, dto.toCurrency)
        assertEquals(conversion.fee, dto.fee)
        assertEquals(conversion.status, dto.status)
        assertEquals(conversion.conversionRate, dto.conversionRate)
        assertEquals(conversion.result, dto.result)
        assertEquals(conversion.createdAt, dto.createdAt)
    }
}
