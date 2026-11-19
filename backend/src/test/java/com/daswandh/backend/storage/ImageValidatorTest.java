package com.daswandh.backend.storage;

import com.daswandh.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageValidatorTest {

    private final ImageValidator validator = new ImageValidator();

    @Test
    void acceptsJpeg() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
        assertThat(validator.validate(jpeg).extension()).isEqualTo("jpg");
    }

    @Test
    void acceptsPng() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
        assertThat(validator.validate(png).extension()).isEqualTo("png");
    }

    @Test
    void acceptsWebp() {
        byte[] webp = "RIFF1234WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
        assertThat(validator.validate(webp).extension()).isEqualTo("webp");
    }

    @Test
    void rejectsAFileThatIsNotAnImageWhateverItIsCalled() {
        byte[] pdf = "%PDF-1.7 pretending to be a picture".getBytes(StandardCharsets.US_ASCII);
        assertThatThrownBy(() -> validator.validate(pdf)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsAnEmptyFile() {
        assertThatThrownBy(() -> validator.validate(new byte[0])).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsAFileOverTheSizeLimit() {
        byte[] tooBig = new byte[ImageValidator.MAX_BYTES + 1];
        tooBig[0] = (byte) 0xFF; tooBig[1] = (byte) 0xD8; tooBig[2] = (byte) 0xFF; // valid header, but too big
        assertThatThrownBy(() -> validator.validate(tooBig))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too large");
    }
}