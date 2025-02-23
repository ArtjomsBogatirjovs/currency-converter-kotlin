package lv.bogatirjovs.currencyconverterkotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class CurrencyConverterKotlinApplication

fun main(args: Array<String>) {
    runApplication<CurrencyConverterKotlinApplication>(*args)
}
