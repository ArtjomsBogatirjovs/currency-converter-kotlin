package lv.bogatirjovs.currencyconverterkotlin.services

import org.springframework.stereotype.Service
import org.w3c.dom.Element
import java.io.InputStream
import java.math.BigDecimal
import javax.xml.parsers.DocumentBuilderFactory

@Service
class ECBRatesParserService {
    private val cube: String = "Cube"
    private val currencyAttribute: String = "currency"
    private val rateAttribute: String = "rate"

    fun parseRatesFromInputStream(inputStream: InputStream): MutableMap<String, BigDecimal> {
        val rates: MutableMap<String, BigDecimal> = HashMap()
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        val cubes = document.getElementsByTagName(cube)
        val values = mutableListOf<Pair<String, Double>>()
        values += "EUR" to 1.0
        for (i in 0 until cubes.length) {
            val cube = cubes.item(i) as? Element ?: continue
            if (cube.hasAttribute(currencyAttribute)) {
                val symbol = cube.getAttribute(currencyAttribute)
                val value = cube.getAttribute(rateAttribute).toDoubleOrNull() ?: continue
                values += symbol to value
            }
        }
        values.map {
            rates.put(it.first, BigDecimal.valueOf(it.second))
        }
        return rates
    }
}