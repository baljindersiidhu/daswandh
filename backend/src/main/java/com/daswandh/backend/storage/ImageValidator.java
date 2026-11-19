package com.daswandh.backend.storage;

import com.daswandh.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

// Decides what a file REALLY is by looking at its first bytes ("magic bytes").
// The file name and the Content-Type header are chosen by the client, so we ignore them.
@Component
public class ImageValidator {

    public static final int MAX_BYTES = 5 * 1024 * 1024;

    public record DetectedImage(String extension, String contentType) {}

    public DetectedImage validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("The uploaded file is empty");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BadRequestException("File is too large (max 5 MB)");
        }
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return new DetectedImage("jpg", "image/jpeg");
        }
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new DetectedImage("png", "image/png");
        }
        // WebP = "RIFF" + 4 size bytes + "WEBP"
        if (bytes.length >= 12 && startsWith(bytes, 'R', 'I', 'F', 'F')
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return new DetectedImage("webp", "image/webp");
        }
        throw new BadRequestException("Only JPEG, PNG or WebP images are allowed");
    }

    private boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) { // & 0xFF turns the signed byte into 0..255
                return false;
            }
        }
        return true;
    }
}