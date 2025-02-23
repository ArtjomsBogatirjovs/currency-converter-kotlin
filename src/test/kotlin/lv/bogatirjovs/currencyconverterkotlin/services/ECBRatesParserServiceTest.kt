package lv.bogatirjovs.currencyconverterkotlin.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class ECBRatesParserServiceTest {

    private val service = ECBRatesParserService()

    @Test
    fun `parseRatesFromInputStream returns correct rates map`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <Cube currency="USD" rate="1.2345"/>
                <Cube currency="JPY" rate="132.45"/>
            </root>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        val rates = service.parseRatesFromInputStream(inputStream)

        assertEquals(BigDecimal("1.0"), rates["EUR"])
        assertEquals(BigDecimal("1.2345"), rates["USD"])
        assertEquals(BigDecimal("132.45"), rates["JPY"])
        assertEquals(3, rates.size, "There should be exactly 3 entries: EUR, USD, and JPY")
    }

    @Test
    fun `parseRatesFromInputStream ignores Cube elements without valid rate attribute`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <Cube currency="USD" rate="1.2345"/>
                <Cube currency="GBP"/> 
            </root>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        val rates = service.parseRatesFromInputStream(inputStream)

        assertEquals(BigDecimal("1.0"), rates["EUR"])
        assertEquals(BigDecimal("1.2345"), rates["USD"])
        assertFalse(rates.containsKey("GBP"))
        assertEquals(2, rates.size)
    }

    @Test
    fun `parseRatesFromInputStream returns only EUR when no Cube with currency found`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <Cube time="2023-01-01"/>
            </root>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        val rates = service.parseRatesFromInputStream(inputStream)

        assertEquals(BigDecimal("1.0"), rates["EUR"])
        assertEquals(1, rates.size)
    }
}