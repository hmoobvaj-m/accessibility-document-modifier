package io.github.hmoobvajm.pdfinspector.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructureTagTest {

    @Test
    void createsStructureTagWithValidValues() {
        StructureTag child = new StructureTag("P", "P", null, "en-US", null, "Paragraph text", 1, List.of());
        StructureTag structureTag = new StructureTag("Sect", "Sect", "Introduction", "en-US", null, null, 1, List.of(child));

        assertAll(
                () -> assertEquals("Sect", structureTag.structureType()),
                () -> assertEquals("Sect", structureTag.standardStructureType()),
                () -> assertEquals("Introduction", structureTag.title()),
                () -> assertEquals("en-US", structureTag.language()),
                () -> assertNull(structureTag.alternateDescription()),
                () -> assertNull(structureTag.actualText()),
                () -> assertEquals(1, structureTag.pageNumber()),
                () -> assertEquals(List.of(child), structureTag.children())
        );
    }

    @Test
    void allowsNullOptionalMetadata() {
        StructureTag structureTag = new StructureTag(null, null, null, null, null, null, null, List.of());

        assertAll(
                () -> assertNull(structureTag.structureType()),
                () -> assertNull(structureTag.standardStructureType()),
                () -> assertNull(structureTag.title()),
                () -> assertNull(structureTag.language()),
                () -> assertNull(structureTag.alternateDescription()),
                () -> assertNull(structureTag.actualText()),
                () -> assertNull(structureTag.pageNumber()),
                () -> assertEquals(List.of(), structureTag.children())
        );
    }

    @Test
    void allowsBlankMetadataForInspectionEvidence() {
        StructureTag structureTag = new StructureTag("", "   ", "", "", "", "", 1, List.of());

        assertAll(
                () -> assertEquals("", structureTag.structureType()),
                () -> assertEquals("   ", structureTag.standardStructureType()),
                () -> assertEquals("", structureTag.title()),
                () -> assertEquals("", structureTag.language()),
                () -> assertEquals("", structureTag.alternateDescription()),
                () -> assertEquals("", structureTag.actualText())
        );
    }

    @Test
    void rejectsZeroPageNumber() {
        assertThrows(IllegalArgumentException.class, () -> new StructureTag("P", "P", null, null, null, null, 0, List.of()));
    }

    @Test
    void rejectsNegativePageNumber() {
        assertThrows(IllegalArgumentException.class, () -> new StructureTag("P", "P", null, null, null, null, -1, List.of()));
    }

    @Test
    void rejectsNullChildrenList() {
        assertThrows(NullPointerException.class, () -> new StructureTag("P", "P", null, null, null, null, 1, null));
    }

    @Test
    void rejectsNullChildElement() {
        List<StructureTag> children = new ArrayList<>();
        children.add(null);

        assertThrows(NullPointerException.class, () -> new StructureTag("Sect", "Sect", null, null, null, null, 1, children));
    }

    @Test
    void defensivelyCopiesChildrenList() {
        StructureTag child = new StructureTag("P", "P", null, null, null, null, 1, List.of());
        List<StructureTag> mutableChildren = new ArrayList<>();
        mutableChildren.add(child);

        StructureTag structureTag = new StructureTag("Sect", "Sect", null, null, null, null, 1, mutableChildren);
        mutableChildren.clear();

        assertEquals(List.of(child), structureTag.children());
    }

    @Test
    void exposesUnmodifiableChildrenList() {
        StructureTag child = new StructureTag("P", "P", null, null, null, null, 1, List.of());
        StructureTag structureTag = new StructureTag("Sect", "Sect", null, null, null, null, 1, List.of(child));

        assertThrows(UnsupportedOperationException.class, () -> structureTag.children().add(child));
    }
}