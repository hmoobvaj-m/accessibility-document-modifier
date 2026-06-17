package io.github.hmoobvajm.pdfinspector.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InspectionResultTest {

    private static final String VALID_SHA256 = "a".repeat(64);

    @Test
    void createsInspectionResultWithValidValues() {
        SourceDocument sourceDocument = new SourceDocument("accessible-document.pdf", 1_024L, VALID_SHA256);
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(), "Process diagram", null, "Figure");
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of(figure));
        StructureTag structureTag = new StructureTag("Document", "Document", null, "en-US", null, null, null, List.of());
        InspectionWarning warning = new InspectionWarning("FIGURE_REVIEW_REQUIRED", "The figure requires manual review.", 1, "figure-1");

        InspectionResult result = new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(page), List.of(structureTag), List.of(warning));

        assertAll(
                () -> assertEquals(InspectionResult.SUPPORTED_SCHEMA_VERSION, result.schemaVersion()),
                () -> assertEquals(sourceDocument, result.sourceDocument()),
                () -> assertEquals(1, result.pageCount()),
                () -> assertEquals(InspectionResult.SUPPORTED_COORDINATE_SYSTEM, result.coordinateSystem()),
                () -> assertEquals(List.of(page), result.pages()),
                () -> assertEquals(List.of(structureTag), result.structureTree()),
                () -> assertEquals(List.of(warning), result.warnings())
        );
    }

    @Test
    void allowsZeroPageInspectionResult() {
        SourceDocument sourceDocument = new SourceDocument("empty-document.pdf", 1L, VALID_SHA256);
        InspectionResult result = new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), List.of());

        assertAll(
                () -> assertEquals(0, result.pageCount()),
                () -> assertEquals(List.of(), result.pages()),
                () -> assertEquals(List.of(), result.structureTree()),
                () -> assertEquals(List.of(), result.warnings())
        );
    }

    @Test
    void rejectsNullSchemaVersion() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(NullPointerException.class, () -> new InspectionResult(null, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), List.of()));
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult("2.0", sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), List.of()));
    }

    @Test
    void rejectsNullSourceDocument() {
        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, null, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), List.of()));
    }

    @Test
    void rejectsNegativePageCount() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, -1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), List.of()));
    }

    @Test
    void rejectsNullCoordinateSystem() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, null, List.of(), List.of(), List.of()));
    }

    @Test
    void rejectsUnsupportedCoordinateSystem() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, "pdf_points_bottom_left", List.of(), List.of(), List.of()));
    }

    @Test
    void rejectsNullPageList() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, null, List.of(), List.of()));
    }

    @Test
    void rejectsPageCountThatDoesNotMatchPageListSize() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of());

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(page), List.of(), List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), List.of()))
        );
    }

    @Test
    void rejectsNullPageElement() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        List<PageInspection> pages = new ArrayList<>();
        pages.add(null);

        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, pages, List.of(), List.of()));
    }

    @Test
    void rejectsNonSequentialPageNumbers() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        PageInspection pageTwo = new PageInspection(2, 612.0, 792.0, List.of());

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(pageTwo), List.of(), List.of()));
    }

    @Test
    void rejectsDuplicateFigureIdsAcrossPages() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        FigureInspection firstFigure = new FigureInspection("figure-1", 1, List.of(), null, null, "Figure");
        FigureInspection secondFigure = new FigureInspection("figure-1", 2, List.of(), null, null, "Figure");
        PageInspection firstPage = new PageInspection(1, 612.0, 792.0, List.of(firstFigure));
        PageInspection secondPage = new PageInspection(2, 612.0, 792.0, List.of(secondFigure));

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 2, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(firstPage, secondPage), List.of(), List.of()));
    }

    @Test
    void rejectsNullStructureTree() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), null, List.of()));
    }

    @Test
    void rejectsNullStructureTreeElement() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        List<StructureTag> structureTree = new ArrayList<>();
        structureTree.add(null);

        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), structureTree, List.of()));
    }

    @Test
    void rejectsStructureTagPageNumberThatExceedsPageCount() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of());
        StructureTag structureTag = new StructureTag("P", "P", null, null, null, null, 2, List.of());

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(page), List.of(structureTag), List.of()));
    }

    @Test
    void rejectsNestedStructureTagPageNumberThatExceedsPageCount() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of());
        StructureTag child = new StructureTag("P", "P", null, null, null, null, 2, List.of());
        StructureTag parent = new StructureTag("Sect", "Sect", null, null, null, null, null, List.of(child));

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(page), List.of(parent), List.of()));
    }

    @Test
    void rejectsNullWarningList() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);

        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), null));
    }

    @Test
    void rejectsNullWarningElement() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        List<InspectionWarning> warnings = new ArrayList<>();
        warnings.add(null);

        assertThrows(NullPointerException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 0, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(), List.of(), warnings));
    }

    @Test
    void rejectsWarningPageNumberThatExceedsPageCount() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of());
        InspectionWarning warning = new InspectionWarning("WARNING_CODE", "Warning message", 2, null);

        assertThrows(IllegalArgumentException.class, () -> new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(page), List.of(), List.of(warning)));
    }

    @Test
    void defensivelyCopiesAggregateLists() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of());
        StructureTag structureTag = new StructureTag("Document", "Document", null, null, null, null, null, List.of());
        InspectionWarning warning = new InspectionWarning("WARNING_CODE", "Warning message", 1, null);
        List<PageInspection> pages = new ArrayList<>(List.of(page));
        List<StructureTag> structureTree = new ArrayList<>(List.of(structureTag));
        List<InspectionWarning> warnings = new ArrayList<>(List.of(warning));

        InspectionResult result = new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, pages, structureTree, warnings);
        pages.clear();
        structureTree.clear();
        warnings.clear();

        assertAll(
                () -> assertEquals(List.of(page), result.pages()),
                () -> assertEquals(List.of(structureTag), result.structureTree()),
                () -> assertEquals(List.of(warning), result.warnings())
        );
    }

    @Test
    void exposesUnmodifiableAggregateLists() {
        SourceDocument sourceDocument = new SourceDocument("document.pdf", 1_024L, VALID_SHA256);
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of());
        StructureTag structureTag = new StructureTag("Document", "Document", null, null, null, null, null, List.of());
        InspectionWarning warning = new InspectionWarning("WARNING_CODE", "Warning message", 1, null);
        InspectionResult result = new InspectionResult(InspectionResult.SUPPORTED_SCHEMA_VERSION, sourceDocument, 1, InspectionResult.SUPPORTED_COORDINATE_SYSTEM, List.of(page), List.of(structureTag), List.of(warning));

        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> result.pages().add(page)),
                () -> assertThrows(UnsupportedOperationException.class, () -> result.structureTree().add(structureTag)),
                () -> assertThrows(UnsupportedOperationException.class, () -> result.warnings().add(warning))
        );
    }
}