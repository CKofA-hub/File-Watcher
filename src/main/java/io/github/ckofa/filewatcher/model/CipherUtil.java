package io.github.ckofa.filewatcher.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class that provides methods for encrypting and decrypting text
 * using the Caesar cipher algorithm. The encryption shifts letters and digits
 * cyclically within their respective alphabets, while non-alphabetic characters
 * remain unchanged.
 * <p>
 * This class is designed as a utility and cannot be instantiated.
 * Non-alphabetic characters (such as Cyrillic letters, symbols, or punctuation)
 * will remain unchanged during encryption and decryption.
 * </p>
 *
 * <h4>Example Usage:</h4>
 * <pre>
 * String encrypted = CipherUtil.encode("Hello123");
 * String decrypted = CipherUtil.decode(encrypted);
 * </pre>
 */
public final class CipherUtil {

    private static final Logger log = LoggerFactory.getLogger(CipherUtil.class);

    /**
     * Offset value for Caesar cipher shift.
     */
    private static final int DEFAULT_SHIFT = 10;

    private static final String LETTERS_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final String DIGITS_STRING = "0123456789";

    private static final Set<Character> LETTERS = LETTERS_STRING.chars()
            .mapToObj(c -> (char) c)
            .collect(Collectors.toSet());
    private static final Set<Character> DIGITS = DIGITS_STRING.chars()
            .mapToObj(c -> (char) c)
            .collect(Collectors.toSet());

    private CipherUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Encrypts a string using the Caesar cipher with the default shift value.
     *
     * @param str   string for encryption.
     * @return encrypted string, or an empty string if an empty string is passed.
     * @throws IllegalArgumentException if the input string is null.
     */
    public static String encode(String str) throws IllegalArgumentException {
        return caesarCipher(str, DEFAULT_SHIFT);
    }

    /**
     * Encrypts a string using the Caesar cipher with a custom shift value.
     *
     * @param str       string for encryption.
     * @param shift     the shift value.
     * @return encrypted string, or an empty string if an empty string is passed.
     * @throws IllegalArgumentException if the input string is null.
     */
    public static String encode(String str, int shift) throws IllegalArgumentException {
        return caesarCipher(str, shift);
    }

    /**
     * Decrypts a string encrypted using the Caesar cipher with the default shift value.
     *
     * @param str   string for decryption.
     * @return decrypted string, or an empty string if an empty string is passed.
     * @throws IllegalArgumentException if the input string is null.
     * @implNote This method reverses the encryption applied by {@link #encode(String)}
     */
    public static String decode(String str) throws IllegalArgumentException {
        return caesarCipher(str, -DEFAULT_SHIFT);
    }

    /**
     * Decrypts a string encrypted using the Caesar cipher with a custom shift value.
     *
     * @param str    string for decryption.
     * @param shift  the shift value.
     * @return the decrypted string, or an empty string if an empty string is passed.
     * @throws IllegalArgumentException if the input string is null.
     * @implNote This method reverses the encryption applied by {@link #encode(String, int)}
     */
    public static String decode(String str, int shift) throws IllegalArgumentException {
        return caesarCipher(str, -shift);
    }

    /**
     * Encrypts or decrypts a string using the Caesar cipher algorithm.
     *
     * @param str      the string to process.
     * @param shift    value of cyclic shift; for decryption, the shift is applied in the opposite direction.
     * @return the processed string, or an empty string if an empty string is passed.
     * @throws IllegalArgumentException if the input string is null.
     */
    private static String caesarCipher(String str, int shift) {
        if (str == null) {
            log.error("encode() or decode() called with null input");
            throw new IllegalArgumentException("Input string cannot be null");
        }
        if (str.isEmpty()) {
            log.info("encode() or decode() called with empty string");
            return str;
        }

        StringBuilder result = new StringBuilder(str.length());
        for (char c : str.toCharArray()) {
            if (LETTERS.contains(c)) {
                result.append(shiftInAlphabet(c, shift, LETTERS_STRING));
            } else if (DIGITS.contains(c)) {
                result.append(shiftInAlphabet(c, shift, DIGITS_STRING));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Performs a cyclic shift of a character within a given alphabet.
     *
     * @param c        the character to shift.
     * @param shift    the shift value.
     * @param alphabet the alphabet to use for shifting.
     * @return the shifted character.
     * @throws IllegalArgumentException if the provided character is not present in the specified alphabet.
     */
    private static char shiftInAlphabet(char c, int shift, String alphabet) {
        int index = alphabet.indexOf(c);
        if (index == -1) {
            log.error("Attempted to shift an invalid character: '{}'", c);
            throw new IllegalArgumentException("An invalid character was passed for encryption: " + c);
        }
        int newIndex = Math.floorMod(index + shift, alphabet.length());
        return alphabet.charAt(newIndex);
    }
}
