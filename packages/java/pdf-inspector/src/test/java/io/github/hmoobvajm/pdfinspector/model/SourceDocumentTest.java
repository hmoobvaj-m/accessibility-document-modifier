package io.github.hmoobvajm.pdfinspector.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SourceDocumentTest {

    private static final String VALID_SHA256 = "a".repeat(64);

    @Test
    void createsSourceDocumentWithValidValues() {
        SourceDocument sourceDocument = new SourceDocument("accessible-document.pdf", 1_024L, VALID_SHA256);

        assertAll(
                () -> assertEquals("accessible-document.pdf", sourceDocument.fileName()),
                () -> assertEquals(1_024L, sourceDocument.sizeBytes()),
                () -> assertEquals(VALID_SHA256, sourceDocument.sha256())
        );
    }

    @Test
    void rejectsNullFileName() {
        assertThrows(NullPointerException.class, () -> new SourceDocument(null, 1_024L, VALID_SHA256));
    }

    @Test
    void rejectsBlankFileName() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new SourceDocument("", 1_024L, VALID_SHA256)),
                () -> assertThrows(IllegalArgumentException.class, () -> new SourceDocument("   ", 1_024L, VALID_SHA256))
        );
    }

    @Test
    void rejectsNonPositiveFileSize() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new SourceDocument("accessible-document.pdf", 0L, VALID_SHA256)),
                () -> assertThrows(IllegalArgumentException.class, () -> new SourceDocument("accessible-document.pdf", -1L, VALID_SHA256))
        );
    }

    @Test
    void rejectsNullSha256() {
        assertThrows(NullPointerException.class, () -> new SourceDocument("accessible-document.pdf", 1_024L, null));
    }

    @Test
    void rejectsSha256WithIncorrectLength() {
        assertThrows(IllegalArgumentException.class, () -> new SourceDocument("accessible-document.pdf", 1_024L, "abc123"));
    }

    @Test
    void rejectsSha256ContainingNonHexadecimalCharacters() {
        String invalidSha256 = "g".repeat(64);

        assertThrows(IllegalArgumentException.class, () -> new SourceDocument("accessible-document.pdf", 1_024L, invalidSha256));
    }

    @Test
    void rejectsUppercaseSha256() {
        String uppercaseSha256 = "A".repeat(64);

        assertThrows(IllegalArgumentException.class, () -> new SourceDocument("accessible-document.pdf", 1_024L, uppercaseSha256));
    }
}