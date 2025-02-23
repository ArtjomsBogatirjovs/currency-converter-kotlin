package lv.bogatirjovs.currencyconverterkotlin.db

import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqSchemaInitializer {

    private val logger = LoggerFactory.getLogger(JooqSchemaInitializer::class.java)

    @Bean
    fun initializeCurrencyConversionTable(dsl: DSLContext) = ApplicationRunner {
        val ddl = """
            CREATE SCHEMA IF NOT EXISTS currency;
            
            CREATE TABLE IF NOT EXISTS currency_conversion (
                id BIGSERIAL PRIMARY KEY,
                amount NUMERIC(19,4) NOT NULL,
                from_currency VARCHAR(3) NOT NULL,
                to_currency VARCHAR(3) NOT NULL,
                fee NUMERIC(5,4) NOT NULL,
                status VARCHAR(20) NOT NULL,
                conversion_rate NUMERIC(19,6),
                result NUMERIC(19,6),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """.trimIndent()

        logger.info("Creating 'currency_conversion' table with DDL:\n$ddl")
        dsl.execute(ddl)
        logger.info("Table 'currency_conversion' has been created or already exists.")
    }
}
