package lv.bogatirjovs.currencyconverterkotlin.repositories

import lv.bogatirjovs.currencycalculatorkotlin.jooq.tables.CurrencyConversion.Companion.CURRENCY_CONVERSION
import lv.bogatirjovs.currencycalculatorkotlin.jooq.tables.daos.CurrencyConversionDao
import lv.bogatirjovs.currencycalculatorkotlin.jooq.tables.pojos.CurrencyConversion

import org.jooq.Configuration
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CurrencyConversionRepository(jooqConfig: Configuration) : CurrencyConversionDao(jooqConfig) {
    private val dsl = jooqConfig.dsl()

    fun fetchConversions(
        offset: Int,
        size: Int,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): List<CurrencyConversion> {
        var condition = DSL.noCondition();
        if (startTime != null) {
            condition = condition.and(CURRENCY_CONVERSION.CREATED_AT.ge(startTime))
        }
        if (endTime != null) {
            condition = condition.and(CURRENCY_CONVERSION.CREATED_AT.le(endTime))
        }
        return dsl.selectFrom(CURRENCY_CONVERSION)
            .where(condition)
            .orderBy(CURRENCY_CONVERSION.CREATED_AT.desc())
            .limit(size)
            .offset(offset)
            .fetchInto(CurrencyConversion::class.java)
    }

    fun countConversions(startTime: LocalDateTime?, endTime: LocalDateTime?): Long {
        var condition = DSL.noCondition()
        if (startTime != null) {
            condition = condition.and(CURRENCY_CONVERSION.CREATED_AT.ge(startTime))
        }
        if (endTime != null) {
            condition = condition.and(CURRENCY_CONVERSION.CREATED_AT.le(endTime))
        }
        return dsl.selectCount()
            .from(CURRENCY_CONVERSION)
            .where(condition)
            .fetchOne(0, Long::class.java) ?: 0L
    }
}