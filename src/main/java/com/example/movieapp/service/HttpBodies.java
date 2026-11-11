package com.example.movieapp.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

final class HttpBodies {

    private HttpBodies() {
    }

    static String decode(byte[] body) {
        if (body == null || body.length == 0) return null;
        if (body.length > 2 && (body[0] & 0xff) == 0x1f && (body[1] & 0xff) == 0x8b) {
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    static String printable(String raw, int max) {
        if (raw == null) return "null";
        String s = raw.replaceAll("[^\\p{Print}\\p{L}\\s]", "?").replaceAll("\\s+", " ").trim();
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
