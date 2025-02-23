package lv.bogatirjovs.currencyconverterkotlin.services

import io.mockk.*
import lv.bogatirjovs.currencyconverterkotlin.utils.HttpClientUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ExternalRateServiceTest {
    private val ecbRatesParserService: ECBRatesParserService = mockk()
    private val dummyEcbUrl = "https://dummy-ecb-url"
    private val externalRateService = ExternalRateService(ecbRatesParserService, dummyEcbUrl)

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getConversionRate returns correct rate when rates are available`() {
        val dummyRates = mutableMapOf(
            "EUR" to BigDecimal("1.0"),
            "USD" to BigDecimal("1.2"),
            "JPY" to BigDecimal("130.0")
        )
        val field = ExternalRateService::class.java.getDeclaredField("rates")
        field.isAccessible = true
        field.set(externalRateService, dummyRates)

        val expectedRate = BigDecimal("130.0").divide(BigDecimal("1.2"), 6, RoundingMode.HALF_UP)
        val actualRate = externalRateService.getConversionRate("usd", "jpy")
        assertEquals(expectedRate, actualRate)

        assertEquals(BigDecimal.ONE, externalRateService.getConversionRate("usd", "usd"))
    }

    @Test
    fun `getConversionRate throws exception if currency not found`() {
        val dummyRates = mutableMapOf(
            "EUR" to BigDecimal("1.0"),
            "USD" to BigDecimal("1.2")
        )
        val field = ExternalRateService::class.java.getDeclaredField("rates")
        field.isAccessible = true
        field.set(externalRateService, dummyRates)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            externalRateService.getConversionRate("USD", "JPY")
        }
        assertTrue(exception.message!!.contains("Conversion rate for JPY is not available"))
    }

    @Test
    fun `downloadTodayRates populates rates using ECBRatesParserService`() {
        val dummyParsedRates = mutableMapOf(
            "EUR" to BigDecimal("1.0"),
            "USD" to BigDecimal("1.2"),
            "JPY" to BigDecimal("130.0")
        )
        every { ecbRatesParserService.parseRatesFromInputStream(any()) } returns dummyParsedRates

        val dummyResponse: HttpResponse<java.io.InputStream> = mockk()
        every { dummyResponse.body() } returns ByteArrayInputStream("dummy content".toByteArray())

        val dummyHttpClient: HttpClient = mockk()
        every {
            dummyHttpClient.send(
                any<HttpRequest>(),
                any<HttpResponse.BodyHandler<java.io.InputStream>>()
            )
        } returns dummyResponse

        mockkObject(HttpClientUtils)
        every { HttpClientUtils.createInsecureHttpClient() } returns dummyHttpClient

        externalRateService.downloadTodayRates()

        val field = ExternalRateService::class.java.getDeclaredField("rates")
        field.isAccessible = true
        val ratesMap = field.get(externalRateService) as MutableMap<*, *>
        assertEquals(dummyParsedRates, ratesMap)
        verify { ecbRatesParserService.parseRatesFromInputStream(any()) }

        unmockkObject(HttpClientUtils)
    }
}