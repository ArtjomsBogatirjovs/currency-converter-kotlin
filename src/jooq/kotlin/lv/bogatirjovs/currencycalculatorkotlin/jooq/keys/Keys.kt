/*
 * This file is generated by jOOQ.
 */
package lv.bogatirjovs.currencycalculatorkotlin.jooq.keys


import lv.bogatirjovs.currencycalculatorkotlin.jooq.tables.CurrencyConversion
import lv.bogatirjovs.currencycalculatorkotlin.jooq.tables.records.CurrencyConversionRecord

import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal



// -------------------------------------------------------------------------
// UNIQUE and PRIMARY KEY definitions
// -------------------------------------------------------------------------

val CURRENCY_CONVERSION_PKEY: UniqueKey<CurrencyConversionRecord> = Internal.createUniqueKey(CurrencyConversion.CURRENCY_CONVERSION, DSL.name("currency_conversion_pkey"), arrayOf(CurrencyConversion.CURRENCY_CONVERSION.ID), true)
