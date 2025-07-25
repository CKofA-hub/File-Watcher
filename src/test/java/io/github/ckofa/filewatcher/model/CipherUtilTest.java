package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CipherUtilTest {

    @Test
    @DisplayName("The encode() and decode() method: should return an empty string if input is empty")
    void encodeAndDecode_whenInputIsEmpty_shouldReturnEmptyString() {
        int customShift = -15;
        String defaultEncodedStr = CipherUtil.encode("");
        String defaultDecodedStr = CipherUtil.decode("");
        String customEncodedStr = CipherUtil.encode("", customShift);
        String customDecodedStr = CipherUtil.decode("", customShift);
        assertAll(
                () -> assertTrue(defaultEncodedStr.isEmpty()),
                () -> assertTrue(defaultDecodedStr.isEmpty()),
                () -> assertTrue(customEncodedStr.isEmpty()),
                () -> assertTrue(customDecodedStr.isEmpty()));
    }

    @Test
    @DisplayName("The encode() and decode() method: should throw exception if input is null")
    void encodeAndDecode_whenInputIsNull_shouldThrowException() {
        int customShift = -15;
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> CipherUtil.encode(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> CipherUtil.decode(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> CipherUtil.encode(null, customShift)),
                () -> assertThrows(IllegalArgumentException.class, () -> CipherUtil.decode(null, customShift)));
    }

    @ParameterizedTest(name = "Test {index}: Default encoding and decoding \"{0}\" should return the original string")
    @ValueSource(strings = {"mMwMAcZ_mCM-8:HUDURw-RfGM6OmPP", "-13456463", "DFS2323#$#%^*", "    ", "\t", "\n"})
    @DisplayName("The encode(String str) and decode(String str) method: should encode and then decode and return the original string")
    void defaultEncodeAndDecode_whenAppliedSequentially_shouldReturnOriginalString(String str) {
        String encodedStr = CipherUtil.encode(str);
        String decodedStr = CipherUtil.decode(encodedStr);
        assertEquals(str, decodedStr);
    }

    @ParameterizedTest(name = "Test {index}: Custom encoding and decoding \"{0}\" with shift \"{1}\" should return the original string")
    @CsvSource({
            "mMwMAcZ_mCM-8:HUDURw-RfGM6OmPP,    -15",
            "-13456463,                         5",
            "DFS2323#$#%^*,                     30",
            "'\t',                              -3",
            "'\n',                              10"
    })
    @DisplayName("The encode(String str, int shift) and decode(String str, int shift) method: should encode and then decode and return the original string")
    void customEncodeAndDecode_whenAppliedSequentially_shouldReturnOriginalString(String str, int shift) {
        String encodedStr = CipherUtil.encode(str, shift);
        String decodedStr = CipherUtil.decode(encodedStr, shift);
        assertEquals(str, decodedStr);
    }
}