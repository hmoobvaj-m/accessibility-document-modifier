package io.github.hmoobvajm.pdfinspector;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hmoobvajm.pdfinspector.model.StructureTag;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.junit.jupiter.api.Test;

class StructureTreeExtractorTest {

    private final StructureTreeExtractor extractor = new StructureTreeExtractor();

    @Test
    void returnsEmptyListWhenDocumentHasNoStructureTree() throws IOException {
        try (PDDocument document = new PDDocument()) {
            assertEquals(List.of(), extractor.extract(document));
        }
    }

    @Test
    void extractsStructureElementMetadata() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDStructureTreeRoot root = attachStructureTree(document);
            PDStructureElement figure = new PDStructureElement("Figure", root);
            figure.setTitle("Process diagram");
            figure.setLanguage("en-US");
            figure.setAlternateDescription("Diagram showing the purchase-to-pay process.");
            figure.setActualText("Purchase-to-pay process");
            figure.setPage(page);
            root.appendKid(figure);

            StructureTag result = extractor.extract(document).getFirst();

            assertAll(
                    () -> assertEquals("Figure", result.structureType()),
                    () -> assertEquals("Figure", result.standardStructureType()),
                    () -> assertEquals("Process diagram", result.title()),
                    () -> assertEquals("en-US", result.language()),
                    () -> assertEquals("Diagram showing the purchase-to-pay process.", result.alternateDescription()),
                    () -> assertEquals("Purchase-to-pay process", result.actualText()),
                    () -> assertEquals(1, result.pageNumber()),
                    () -> assertEquals(List.of(), result.children())
            );
        }
    }

    @Test
    void resolvesRoleMappedStandardStructureType() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = attachStructureTree(document);
            root.setRoleMap(Map.of("CustomHeading", "H1"));

            PDStructureElement heading = new PDStructureElement("CustomHeading", root);
            root.appendKid(heading);

            StructureTag result = extractor.extract(document).getFirst();

            assertAll(
                    () -> assertEquals("CustomHeading", result.structureType()),
                    () -> assertEquals("H1", result.standardStructureType())
            );
        }
    }

    @Test
    void extractsNestedStructureElementsInOrder() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = attachStructureTree(document);
            PDStructureElement section = new PDStructureElement("Sect", root);
            PDStructureElement heading = new PDStructureElement("H1", section);
            PDStructureElement paragraph = new PDStructureElement("P", section);

            root.appendKid(section);
            section.appendKid(heading);
            section.appendKid(paragraph);

            StructureTag result = extractor.extract(document).getFirst();

            assertAll(
                    () -> assertEquals("Sect", result.structureType()),
                    () -> assertEquals(2, result.children().size()),
                    () -> assertEquals("H1", result.children().get(0).structureType()),
                    () -> assertEquals("P", result.children().get(1).structureType())
            );
        }
    }

    @Test
    void preservesRootStructureElementOrder() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = attachStructureTree(document);
            root.appendKid(new PDStructureElement("H1", root));
            root.appendKid(new PDStructureElement("P", root));
            root.appendKid(new PDStructureElement("Figure", root));

            List<StructureTag> results = extractor.extract(document);

            assertAll(
                    () -> assertEquals(3, results.size()),
                    () -> assertEquals("H1", results.get(0).structureType()),
                    () -> assertEquals("P", results.get(1).structureType()),
                    () -> assertEquals("Figure", results.get(2).structureType())
            );
        }
    }

    @Test
    void ignoresMarkedContentIdentifiersAsSemanticChildren() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = attachStructureTree(document);
            PDStructureElement paragraph = new PDStructureElement("P", root);
            paragraph.appendKid(0);
            root.appendKid(paragraph);

            StructureTag result = extractor.extract(document).getFirst();

            assertEquals(List.of(), result.children());
        }
    }

    @Test
    void leavesPageNumberNullWhenPageReferenceIsNotInDocument() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = attachStructureTree(document);
            PDPage foreignPage = new PDPage(PDRectangle.LETTER);
            PDStructureElement paragraph = new PDStructureElement("P", root);
            paragraph.setPage(foreignPage);
            root.appendKid(paragraph);

            StructureTag result = extractor.extract(document).getFirst();

            assertNull(result.pageNumber());
        }
    }

    @Test
    void detectsCycleInStructureTree() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = attachStructureTree(document);
            PDStructureElement section = new PDStructureElement("Sect", root);
            root.appendKid(section);
            section.setKids(List.of((Object) section));

            IOException exception = assertThrows(IOException.class, () -> extractor.extract(document));

            assertTrue(exception.getMessage().contains("Cycle detected"));
        }
    }

    @Test
    void returnsUnmodifiableRootList() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = attachStructureTree(document);
            root.appendKid(new PDStructureElement("Document", root));

            List<StructureTag> results = extractor.extract(document);

            assertThrows(UnsupportedOperationException.class, results::clear);
        }
    }

    @Test
    void rejectsNullDocument() {
        assertThrows(NullPointerException.class, () -> extractor.extract(null));
    }

    private static PDStructureTreeRoot attachStructureTree(PDDocument document) {
        PDStructureTreeRoot root = new PDStructureTreeRoot();
        document.getDocumentCatalog().setStructureTreeRoot(root);
        return root;
    }
}