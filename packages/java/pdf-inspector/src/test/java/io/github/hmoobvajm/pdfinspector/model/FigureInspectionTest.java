package io.github.hmoobvajm.pdfinspector.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FigureInspectionTest {

    @Test
    void createsFigureInspectionWithValidValues() {
        BoundingBox boundingBox = new BoundingBox(10.0, 20.0, 100.0, 200.0);
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(boundingBox), "Diagram of a process", "Process diagram", "Figure");

        assertAll(
                () -> assertEquals("figure-1", figure.figureId()),
                () -> assertEquals(1, figure.pageNumber()),
                () -> assertEquals(List.of(boundingBox), figure.boundingBoxes()),
                () -> assertEquals("Diagram of a process", figure.alternateDescription()),
                () -> assertEquals("Process diagram", figure.actualText()),
                () -> assertEquals("Figure", figure.structureType())
        );
    }

    @Test
    void allowsMissingOptionalMetadata() {
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(), null, null, null);

        assertAll(
                () -> assertNull(figure.alternateDescription()),
                () -> assertNull(figure.actualText()),
                () -> assertNull(figure.structureType())
        );
    }

    @Test
    void allowsBlankOptionalMetadataAsInspectionEvidence() {
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(), "", "   ", "");

        assertAll(
                () -> assertEquals("", figure.alternateDescription()),
                () -> assertEquals("   ", figure.actualText()),
                () -> assertEquals("", figure.structureType())
        );
    }

    @Test
    void allowsEmptyBoundingBoxList() {
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(), null, null, "Figure");

        assertEquals(List.of(), figure.boundingBoxes());
    }

    @Test
    void rejectsNullFigureId() {
        assertThrows(NullPointerException.class, () -> new FigureInspection(null, 1, List.of(), null, null, "Figure"));
    }

    @Test
    void rejectsBlankFigureId() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new FigureInspection("", 1, List.of(), null, null, "Figure")),
                () -> assertThrows(IllegalArgumentException.class, () -> new FigureInspection("   ", 1, List.of(), null, null, "Figure"))
        );
    }

    @Test
    void rejectsNonPositivePageNumber() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new FigureInspection("figure-1", 0, List.of(), null, null, "Figure")),
                () -> assertThrows(IllegalArgumentException.class, () -> new FigureInspection("figure-1", -1, List.of(), null, null, "Figure"))
        );
    }

    @Test
    void rejectsNullBoundingBoxList() {
        assertThrows(NullPointerException.class, () -> new FigureInspection("figure-1", 1, null, null, null, "Figure"));
    }

    @Test
    void rejectsNullBoundingBoxElement() {
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boundingBoxes.add(null);

        assertThrows(NullPointerException.class, () -> new FigureInspection("figure-1", 1, boundingBoxes, null, null, "Figure"));
    }

    @Test
    void defensivelyCopiesBoundingBoxList() {
        BoundingBox boundingBox = new BoundingBox(10.0, 20.0, 100.0, 200.0);
        List<BoundingBox> mutableBoundingBoxes = new ArrayList<>();
        mutableBoundingBoxes.add(boundingBox);

        FigureInspection figure = new FigureInspection("figure-1", 1, mutableBoundingBoxes, null, null, "Figure");
        mutableBoundingBoxes.clear();

        assertEquals(List.of(boundingBox), figure.boundingBoxes());
    }

    @Test
    void exposesUnmodifiableBoundingBoxList() {
        BoundingBox boundingBox = new BoundingBox(10.0, 20.0, 100.0, 200.0);
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(boundingBox), null, null, "Figure");

        assertThrows(UnsupportedOperationException.class, () -> figure.boundingBoxes().add(boundingBox));
    }
}