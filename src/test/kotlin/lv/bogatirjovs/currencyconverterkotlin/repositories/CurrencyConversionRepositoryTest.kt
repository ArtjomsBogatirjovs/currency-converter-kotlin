package lv.bogatirjovs.currencyconverterkotlin.repositories

import org.jooq.DSLContext
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.time.LocalDateTime

@Testcontainers
class CurrencyConversionRepositoryTest {
    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:17").apply {
            withDatabaseName("testCurrency")
            withUsername("testCurrency")
            withPassword("testCurrency")
        }
    }

    private lateinit var connection: Connection
    private lateinit var dsl: DSLContext
    private lateinit var configuration: org.jooq.Configuration
    private lateinit var repository: CurrencyConversionRepository

    @BeforeEach
    fun setUp() {
        connection = java.sql.DriverManager.getConnection(
            postgresContainer.jdbcUrl,
            postgresContainer.username,
            postgresContainer.password
        )

        configuration = DefaultConfiguration()
            .derive(connection)
            .derive(Settings().withRenderSchema(false))
        dsl = DSL.using(configuration)

        dsl.execute(
            """
            CREATE TABLE IF NOT EXISTS currency_conversion (
                id SERIAL PRIMARY KEY,
                amount DECIMAL(19,4) NOT NULL,
                from_currency VARCHAR(3) NOT NULL,
                to_currency VARCHAR(3) NOT NULL,
                fee DECIMAL(5,4) NOT NULL,
                status VARCHAR(20) NOT NULL,
                conversion_rate DECIMAL(19,6),
                result DECIMAL(19,6),
                created_at TIMESTAMP NOT NULL
            )
            """.trimIndent()
        )

        repository = CurrencyConversionRepository(configuration)
    }

    @AfterEach
    fun tearDown() {
        dsl.execute("DROP TABLE IF EXISTS currency_conversion")
        connection.close()
    }

    @Test
    fun `fetchConversions returns correct conversions with time filter and pagination`() {
        val now = LocalDateTime.now()

        dsl.execute(
            """
            INSERT INTO currency_conversion 
            (amount, from_currency, to_currency, fee, status, conversion_rate, result, created_at)
            VALUES (100.00, 'USD', 'EUR', 0.0100, 'DONE', 1.2, 118.8, '$now')
            """.trimIndent()
        )
        dsl.execute(
            """
            INSERT INTO currency_conversion 
            (amount, from_currency, to_currency, fee, status, conversion_rate, result, created_at)
            VALUES (150.00, 'GBP', 'USD', 0.0100, 'PENDING', NULL, NULL, '${now.minusDays(1)}')
            """.trimIndent()
        )
        dsl.execute(
            """
            INSERT INTO currency_conversion 
            (amount, from_currency, to_currency, fee, status, conversion_rate, result, created_at)
            VALUES (200.00, 'JPY', 'USD', 0.0100, 'DONE', 0.009, 1.8, '${now.minusDays(2)}')
            """.trimIndent()
        )

        val conversions = repository.fetchConversions(0, 10, null, null)
        assertEquals(3, conversions.size)
        assertEquals("USD", conversions.first().fromCurrency)
    }

    @Test
    fun `countConversions returns correct count with time filter`() {
        val now = LocalDateTime.now()
        dsl.execute(
            """
            INSERT INTO currency_conversion 
            (amount, from_currency, to_currency, fee, status, conversion_rate, result, created_at)
            VALUES (100.00, 'USD', 'EUR', 0.0100, 'DONE', 1.2, 118.8, '$now')
            """.trimIndent()
        )
        dsl.execute(
            """
            INSERT INTO currency_conversion 
            (amount, from_currency, to_currency, fee, status, conversion_rate, result, created_at)
            VALUES (150.00, 'GBP', 'USD', 0.0100, 'PENDING', NULL, NULL, '${now.minusDays(1)}')
            """.trimIndent()
        )

        val countAll = repository.countConversions(null, null)
        assertEquals(2L, countAll)

        val countFiltered = repository.countConversions(now.minusHours(12), null)
        assertEquals(1L, countFiltered)
    }
}