package com.vod.exception;

public class VideoSystemException extends RuntimeException {
    public VideoSystemException(String message) {
        super(message);
    }

    public VideoSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
