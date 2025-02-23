package lv.bogatirjovs.currencyconverterkotlin.services

import jakarta.annotation.PostConstruct
import lv.bogatirjovs.currencyconverterkotlin.utils.HttpClientUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse


@Service
class ExternalRateService(
    val ecbRatesParserService: ECBRatesParserService,
    @Value("\${currency-conversion.ecb-api}")
    private val ecbUrl: String
) {
    private val logger: Logger = LoggerFactory.getLogger(ExternalRateService::class.java)
    private val rates: MutableMap<String, BigDecimal> = HashMap()

    /**
     * Returns the conversion rate from the "from" currency to the "to" currency.
     * Rates are expected to be relative to EUR (i.e. EUR = 1.0).
     *
     * @param from The currency code to convert from.
     * @param to The currency code to convert to.
     * @return The conversion rate as a BigDecimal.
     * @throws IllegalArgumentException if either currency is not found in the rates map.
     */
    fun getConversionRate(from: String, to: String): BigDecimal {
        val fromCurrency = from.uppercase()
        val toCurrency = to.uppercase()

        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE
        }

        val fromRate = rates[fromCurrency]
            ?: throw IllegalArgumentException("Conversion rate for $fromCurrency is not available")
        val toRate = rates[toCurrency]
            ?: throw IllegalArgumentException("Conversion rate for $toCurrency is not available")

        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP)
    }

    fun convertAmount(amount: BigDecimal, fee: BigDecimal, rate: BigDecimal): BigDecimal {
        val netAmount = amount.multiply(BigDecimal.ONE.subtract(fee))
        return netAmount.multiply(rate)
    }

    @PostConstruct
    fun downloadTodayRates() {
        val httpClient = HttpClientUtils.createInsecureHttpClient()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(ecbUrl))
            .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            rates.putAll(ecbRatesParserService.parseRatesFromInputStream(response.body()))
        } catch (e: Exception) {
            logger.error("Error download ECB rates.", e)
        }
    }
}