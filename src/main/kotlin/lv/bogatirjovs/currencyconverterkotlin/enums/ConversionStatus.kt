package lv.bogatirjovs.currencyconverterkotlin.enums

enum class ConversionStatus(val statusName: String) {
    PENDING("PENDING"),
    DONE("DONE"),
    FAILED("FAILED")
}