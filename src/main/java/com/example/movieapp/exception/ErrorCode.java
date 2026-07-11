package com.example.movieapp.exception;

public enum ErrorCode {

    // Auth (1000-1099)
    USER_NOT_FOUND(1001, "Foydalanuvchi topilmadi"),
    INVALID_CREDENTIALS(1002, "Email yoki parol noto'g'ri"),
    USER_ALREADY_EXISTS(1003, "Bu email bilan foydalanuvchi allaqachon ro'yxatdan o'tgan"),
    SESSION_NOT_FOUND(1004, "Sessiya topilmadi, qayta kiring"),
    SESSION_EXPIRED(1005, "Sessiya muddati tugagan, qayta kiring"),
    ACCESS_DENIED(1006, "Sizda bu amalni bajarish huquqi yo'q"),

    // Series (2000-2099)
    SERIES_NOT_FOUND(2001, "Serial topilmadi"),
    SERIES_HAS_ACTIVE_SUBSCRIBERS(2002, "Bu serialga obunasi faol foydalanuvchilar mavjud, shuning uchun uni o'chirib bo'lmaydi"),

    // Episode (3000-3099)
    EPISODE_NOT_FOUND(3001, "Epizod topilmadi"),
    EPISODE_NOT_BELONG_TO_SERIES(3002, "Bu epizod ushbu serialga tegishli emas"),

    // Banner (4000-4099)
    BANNER_NOT_FOUND(4001, "Banner topilmadi"),

    // Movie Access (5000-5099)
    MOVIE_ACCESS_ALREADY_EXISTS(5001, "Foydalanuvchi bu serialga allaqachon kirish huquqiga ega"),
    MOVIE_ACCESS_NOT_FOUND(5002, "Foydalanuvchida bu serialga kirish huquqi mavjud emas"),
    NO_ACCESS_TO_SERIES(5003, "Siz bu serialni ko'ra olmaysiz. Obuna yoki kirish huquqi mavjud emas"),

    // Payment (6000-6099)
    PAYMENT_INVALID_REQUEST(6001, "To'lov ma'lumotlari noto'g'ri: subscriptionDays yoki seriesId ko'rsatilishi kerak"),

    // File (7000-7099)
    FILE_UPLOAD_FAILED(7001, "Faylni saqlashda xatolik yuz berdi"),

    // General (9000-9099)
    INTERNAL_ERROR(9001, "Server xatosi yuz berdi");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
