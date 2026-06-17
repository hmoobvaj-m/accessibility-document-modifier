package io.github.hmoobvajm.pdfinspector.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InspectionWarningTest {

    @Test
    void createsInspectionWarningWithValidValues() {
        InspectionWarning warning = new InspectionWarning("FIGURE_GEOMETRY_MISSING", "No bounding box could be resolved for the figure.", 3, "figure-7");

        assertAll(
                () -> assertEquals("FIGURE_GEOMETRY_MISSING", warning.code()),
                () -> assertEquals("No bounding box could be resolved for the figure.", warning.message()),
                () -> assertEquals(3, warning.pageNumber()),
                () -> assertEquals("figure-7", warning.relatedObjectId())
        );
    }

    @Test
    void allowsDocumentLevelWarningWithoutPageOrRelatedObject() {
        InspectionWarning warning = new InspectionWarning("STRUCTURE_TREE_MISSING", "The PDF does not contain a logical structure tree.", null, null);

        assertAll(
                () -> assertNull(warning.pageNumber()),
                () -> assertNull(warning.relatedObjectId())
        );
    }

    @Test
    void rejectsNullCode() {
        assertThrows(NullPointerException.class, () -> new InspectionWarning(null, "Warning message", 1, null));
    }

    @Test
    void rejectsBlankCode() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("", "Warning message", 1, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("   ", "Warning message", 1, null))
        );
    }

    @Test
    void rejectsNullMessage() {
        assertThrows(NullPointerException.class, () -> new InspectionWarning("WARNING_CODE", null, 1, null));
    }

    @Test
    void rejectsBlankMessage() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("WARNING_CODE", "", 1, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("WARNING_CODE", "   ", 1, null))
        );
    }

    @Test
    void rejectsNonPositivePageNumber() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("WARNING_CODE", "Warning message", 0, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("WARNING_CODE", "Warning message", -1, null))
        );
    }

    @Test
    void rejectsBlankRelatedObjectId() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("WARNING_CODE", "Warning message", 1, "")),
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionWarning("WARNING_CODE", "Warning message", 1, "   "))
        );
    }
}