package io.github.hmoobvajm.pdfinspector;

import io.github.hmoobvajm.pdfinspector.model.FigureInspection;
import io.github.hmoobvajm.pdfinspector.model.StructureTag;
import io.github.hmoobvajm.pdfinspector.model.StructureContentReference;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class FigureExtractorTest {

    private final FigureExtractor extractor = new FigureExtractor();

    @Test
    void returnsEmptyListWhenStructureTreeIsEmpty() {
        assertTrue(extractor.extract(List.of()).isEmpty());
    }

    @Test
    void extractsFigureMetadataAndLeavesBoundingBoxesEmpty() {
        StructureTag figureTag = tag("Figure", "Figure", 2, "Diagram of the order process", "Order process diagram");

        List<FigureInspection> figures = extractor.extract(List.of(figureTag));

        assertEquals(1, figures.size());
        assertEquals("figure-1", figures.getFirst().figureId());
        assertEquals(2, figures.getFirst().pageNumber());
        assertTrue(figures.getFirst().boundingBoxes().isEmpty());
        assertEquals("Diagram of the order process", figures.getFirst().alternateDescription());
        assertEquals("Order process diagram", figures.getFirst().actualText());
        assertEquals("Figure", figures.getFirst().structureType());
    }

    @Test
    void recognizesRoleMappedFigureUsingStandardStructureType() {
        StructureTag figureTag = tag("CustomDiagram", "Figure", 1, "Custom tagged diagram", null);

        List<FigureInspection> figures = extractor.extract(List.of(figureTag));

        assertEquals(1, figures.size());
        assertEquals("CustomDiagram", figures.getFirst().structureType());
    }

    @Test
    void ignoresNonFigureStructureTags() {
        StructureTag paragraphTag = tag("P", "P", 1, null, null);
        StructureTag tableTag = tag("Table", "Table", 1, null, null);

        assertTrue(extractor.extract(List.of(paragraphTag, tableTag)).isEmpty());
    }

    @Test
    void extractsNestedFiguresInDocumentOrder() {
        StructureTag firstFigure = tag("Figure", "Figure", 1, "First figure", null);
        StructureTag secondFigure = tag("Figure", "Figure", 2, "Second figure", null);
        StructureTag section = tag("Sect", "Sect", null, null, null, firstFigure);
        StructureTag document = tag("Document", "Document", null, null, null, section, secondFigure);

        List<FigureInspection> figures = extractor.extract(List.of(document));

        assertEquals(2, figures.size());
        assertEquals("figure-1", figures.get(0).figureId());
        assertEquals("First figure", figures.get(0).alternateDescription());
        assertEquals("figure-2", figures.get(1).figureId());
        assertEquals("Second figure", figures.get(1).alternateDescription());
    }

    @Test
    void inheritsPageNumberFromAncestor() {
        StructureTag figure = tag("Figure", "Figure", null, "Inherited-page figure", null);
        StructureTag section = tag("Sect", "Sect", 3, null, null, figure);

        List<FigureInspection> figures = extractor.extract(List.of(section));

        assertEquals(3, figures.getFirst().pageNumber());
    }

    @Test
    void resolvesPageNumberFromSingleDescendantPage() {
        StructureTag markedContent = tag("Span", "Span", 4, null, null);
        StructureTag figure = tag("Figure", "Figure", null, "Descendant-page figure", null, markedContent);

        List<FigureInspection> figures = extractor.extract(List.of(figure));

        assertEquals(4, figures.getFirst().pageNumber());
    }

    @Test
    void rejectsFigureWithoutResolvablePageNumber() {
        StructureTag figure = tag("Figure", "Figure", null, "Unresolved figure", null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> extractor.extract(List.of(figure)));

        assertEquals("Figure structure tag does not resolve to a page number", exception.getMessage());
    }

    @Test
    void rejectsFigureThatSpansMultiplePages() {
        StructureTag firstContentItem = tag("Span", "Span", 1, null, null);
        StructureTag secondContentItem = tag("Span", "Span", 2, null, null);
        StructureTag figure = tag("Figure", "Figure", null, "Multi-page figure", null, firstContentItem, secondContentItem);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> extractor.extract(List.of(figure)));

        assertEquals("Figure structure tag spans multiple pages and cannot be represented by FigureInspection", exception.getMessage());
    }

    @Test
    void returnsImmutableFigureList() {
        StructureTag figureTag = tag("Figure", "Figure", 1, "Figure", null);
        List<FigureInspection> figures = extractor.extract(List.of(figureTag));

        assertThrows(UnsupportedOperationException.class, () -> figures.add(new FigureInspection("figure-2", 1, List.of(), null, null, "Figure")));
    }

    @Test
    void rejectsNullStructureTree() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> extractor.extract(null));

        assertEquals("structureTree must not be null", exception.getMessage());
    }

    private static StructureTag tag(String structureType, String standardStructureType, Integer pageNumber, String alternateDescription, String actualText, StructureTag... children) {
        return new StructureTag(structureType, standardStructureType, null, null, alternateDescription, actualText, pageNumber, List.of(children), List.<StructureContentReference>of());
    }
}