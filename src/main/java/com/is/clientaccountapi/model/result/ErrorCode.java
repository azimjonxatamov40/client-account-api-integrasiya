package com.is.clientaccountapi.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    EMPTY_PARAM(400, 5001, "Majburiy maydon to'ldirilmagan."),
    INVALID_PARAM(400, 5002, "Maydon qiymati noto'g'ri."),

    CLIENT_NOT_FOUND(404, 5006, "Mijoz topilmadi."),
    CLIENT_EXISTS(409, 5007, "Bu PINFL bilan mijoz allaqachon mavjud."),

    ACC_NOT_FOUND(404, 5004, "Hisob topilmadi."),
    ACC_ALREADY_CLOSED(409, 5008, "Hisob allaqachon yopilgan."),
    ACC_HAS_BALANCE(409, 5009, "Hisobda qoldiq bor, yopib bo'lmaydi."),
    ACC_CLOSED(409, 5010, "Hisob yopilgan."),
    INSUFFICIENT_FUNDS(409, 5011, "Hisobda mablag' yetarli emas."),
    CURRENCY_MISMATCH(409, 5012, "Hisoblar valyutasi bir xil emas."),

    DB_ERROR(500, 9000, "Bazada xatolik.");

    private final int httpStatus;
    private final int code;
    private final String cause;
}
