package com.hubilon.common.response;

public record Response<T>(boolean success, T data, String message) {

    public static <T> Response<T> ok(T data) {
        return new Response<>(true, data, null);
    }

    public static <T> Response<T> ok() {
        return new Response<>(true, null, null);
    }

    public static <T> Response<T> fail(String message) {
        return new Response<>(false, null, message);
    }
}
