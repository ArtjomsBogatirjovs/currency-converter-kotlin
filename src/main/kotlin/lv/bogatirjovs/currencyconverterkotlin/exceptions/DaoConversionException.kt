package lv.bogatirjovs.currencyconverterkotlin.exceptions

open class DaoConversionException(objectName: String?, operation: String) :
    RuntimeException("Error $operation $objectName")