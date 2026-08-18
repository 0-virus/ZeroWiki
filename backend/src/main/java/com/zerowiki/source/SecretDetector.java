package com.zerowiki.source;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

final class SecretDetector {
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----");
    private static final Pattern AWS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");
    private static final Pattern API_KEY = Pattern.compile("(?i)\\b(?:api[_-]?key|secret[_-]?key|access[_-]?token)\\s*[:=]\\s*['\\\"]?[A-Za-z0-9_./+=-]{16,}");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bbearer\\s+[A-Za-z0-9._~-]{20,}");

    private SecretDetector() {
    }

    static boolean containsSecret(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        return PRIVATE_KEY.matcher(text).find() || AWS_KEY.matcher(text).find()
                || API_KEY.matcher(text).find() || BEARER.matcher(text).find();
    }
}
