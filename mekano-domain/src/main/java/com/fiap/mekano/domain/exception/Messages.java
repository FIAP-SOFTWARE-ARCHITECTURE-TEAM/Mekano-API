package com.fiap.mekano.domain.exception;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class Messages {

    private static final String BASE_NAME = "com.fiap.mekano.domain.exception.messages";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BASE_NAME);

    private Messages() {}

    public static String get(String key, Object... args) {
        return MessageFormat.format(BUNDLE.getString(key), args);
    }
}
