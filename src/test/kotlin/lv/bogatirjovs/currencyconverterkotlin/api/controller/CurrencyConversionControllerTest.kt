package lv.bogatirjovs.currencyconverterkotlin.api.controller


import com.fasterxml.jackson.databind.ObjectMapper
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionPageResponseDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionRequestDTO
import lv.bogatirjovs.currencyconverterkotlin.api.dto.ConversionResultDTO
import lv.bogatirjovs.currencyconverterkotlin.enums.ConversionStatus
import lv.bogatirjovs.currencyconverterkotlin.services.CurrencyConversionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(CurrencyConversionController::class)
class CurrencyConversionControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var conversionService: CurrencyConversionService

    @Test
    fun `POST createConversion returns OK with conversion ID`() {
        val requestDto = ConversionRequestDTO(
            amount = BigDecimal("100.0"),
            fromCurrency = "USD",
            toCurrency = "EUR"
        )
        Mockito.`when`(conversionService.processConversionRequest(requestDto)).thenReturn(1L)
        Mockito.doNothing().`when`(conversionService).processConversionAsync(
            1L,
            requestDto.amount,
            requestDto.fromCurrency,
            requestDto.toCurrency
        )

        mockMvc.perform(
            post("/api/conversion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))

        Mockito.verify(conversionService).processConversionRequest(requestDto)
        Mockito.verify(conversionService).processConversionAsync(
            1L,
            requestDto.amount,
            requestDto.fromCurrency,
            requestDto.toCurrency
        )
    }


    @Test
    fun `POST createConversion returns 500 on error`() {
        val requestDto = ConversionRequestDTO(
            amount = BigDecimal("100.0"),
            fromCurrency = "USD",
            toCurrency = "EUR"
        )
        Mockito.`when`(conversionService.processConversionRequest(requestDto)).thenThrow(RuntimeException("Error"))

        mockMvc.perform(
            post("/api/conversion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `GET getConversionResult returns OK with conversion result`() {
        val conversionResult = ConversionResultDTO(
            id = 1,
            amount = BigDecimal("100.0"),
            fromCurrency = "USD",
            toCurrency = "EUR",
            fee = BigDecimal("0.01"),
            status = ConversionStatus.DONE.statusName,
            conversionRate = BigDecimal("1.2"),
            result = BigDecimal("118.8"),
            createdAt = LocalDateTime.now()
        )
        Mockito.`when`(conversionService.getConversionResult(1L)).thenReturn(conversionResult)

        mockMvc.perform(get("/api/conversion/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.amount").value(100.0))
            .andExpect(jsonPath("$.fromCurrency").value("USD"))
            .andExpect(jsonPath("$.toCurrency").value("EUR"))
            .andExpect(jsonPath("$.fee").value(0.01))
            .andExpect(jsonPath("$.status").value(ConversionStatus.DONE.statusName))
            .andExpect(jsonPath("$.conversionRate").value(1.2))
            .andExpect(jsonPath("$.result").value(118.8))
    }

    @Test
    fun `GET getConversionResult returns 404 when conversion not found`() {
        Mockito.`when`(conversionService.getConversionResult(999L)).thenThrow(RuntimeException("Not found"))
        mockMvc.perform(get("/api/conversion/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET getConversionPage returns OK with paginated response`() {
        val page = 0
        val size = 20
        val startTime = LocalDateTime.of(2022, 1, 1, 0, 0)
        val endTime = LocalDateTime.of(2022, 12, 31, 23, 59)
        val conversionResult = ConversionResultDTO(
            id = 1,
            amount = BigDecimal("100.0"),
            fromCurrency = "USD",
            toCurrency = "EUR",
            fee = BigDecimal("0.01"),
            status = ConversionStatus.DONE.statusName,
            conversionRate = BigDecimal("1.2"),
            result = BigDecimal("118.8"),
            createdAt = LocalDateTime.now()
        )

        val conversionPageResponse = ConversionPageResponseDTO(
            conversions = listOf(conversionResult),
            page = page,
            size = size,
            totalElements = 1,
            totalPages = 1
        )
        Mockito.`when`(conversionService.getConversionPage(page, size, startTime, endTime))
            .thenReturn(conversionPageResponse)

        mockMvc.perform(
            get("/api/conversion/page")
                .param("page", page.toString())
                .param("size", size.toString())
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(page))
            .andExpect(jsonPath("$.size").value(size))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.conversions[0].id").value(1))
    }
}