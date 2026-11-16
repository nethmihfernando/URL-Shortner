package com.example.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortCodeGeneratorTest {

    @Test
    void generatesBase62CodesOfConfiguredLength() {
        ShortCodeGenerator generator = new ShortCodeGenerator(7);
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.generate();
            assertTrue(code.matches("[0-9A-Za-z]{7}"), "unexpected code: " + code);
            codes.add(code);
        }
        assertEquals(1000, codes.size());
    }

    @Test
    void rejectsLengthsOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(3));
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(33));
    }
}
