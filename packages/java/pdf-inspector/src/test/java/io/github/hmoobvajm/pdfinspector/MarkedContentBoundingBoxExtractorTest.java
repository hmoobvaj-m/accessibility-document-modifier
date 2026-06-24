package io.github.hmoobvajm.pdfinspector;

import io.github.hmoobvajm.pdfinspector.model.BoundingBox;
import io.github.hmoobvajm.pdfinspector.model.StructureContentReference;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkedContentBoundingBoxExtractorTest {
    private final MarkedContentBoundingBoxExtractor extractor = new MarkedContentBoundingBoxExtractor();

    @Test
    void returnsEmptyListWhenContentReferencesAreEmpty() throws IOException {
        try (PDDocument document = new PDDocument()) {
            assertTrue(extractor.extract(document, List.of()).isEmpty());
        }
    }

    @Test
    void rejectsNullDocument() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> extractor.extract(null, List.of()));

        assertEquals("document must not be null", exception.getMessage());
    }

    @Test
    void rejectsNullContentReferences() throws IOException {
        try (PDDocument document = new PDDocument()) {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> extractor.extract(document, null));

            assertEquals("contentReferences must not be null", exception.getMessage());
        }
    }

    @Test
    void rejectsNullContentReferenceElements() throws IOException {
        try (PDDocument document = new PDDocument()) {
            List<StructureContentReference> contentReferences = Collections.singletonList(null);

            NullPointerException exception = assertThrows(NullPointerException.class, () -> extractor.extract(document, contentReferences));

            assertEquals("contentReferences must not contain null elements", exception.getMessage());
        }
    }

    @Test
    void returnsImmutableBoundingBoxList() throws IOException {
        try (PDDocument document = new PDDocument()) {
            List<BoundingBox> boundingBoxes = extractor.extract(document, List.of());

            assertThrows(UnsupportedOperationException.class, () -> boundingBoxes.add(null));
        }
    }

    @Test
    void extractsBoundingBoxForMarkedTextContent() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            writeMarkedText(document, page, 0, 72.0f, 720.0f, "Marked text");

            List<BoundingBox> boundingBoxes = extractor.extract(document, List.of(new StructureContentReference(1, 0)));

            assertAll(
                    () -> assertEquals(1, boundingBoxes.size()),
                    () -> assertTrue(boundingBoxes.getFirst().width() > 0.0),
                    () -> assertTrue(boundingBoxes.getFirst().height() > 0.0)
            );
        }
    }

    @Test
    void ignoresMarkedTextWithDifferentMarkedContentId() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            writeMarkedText(document, page, 1, 72.0f, 720.0f, "Marked text");

            List<BoundingBox> boundingBoxes = extractor.extract(document, List.of(new StructureContentReference(1, 0)));

            assertTrue(boundingBoxes.isEmpty());
        }
    }

    @Test
    void usesPageNumberWhenSameMarkedContentIdAppearsOnMultiplePages() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage firstPage = new PDPage(PDRectangle.LETTER);
            PDPage secondPage = new PDPage(PDRectangle.LETTER);
            document.addPage(firstPage);
            document.addPage(secondPage);

            writeMarkedText(document, firstPage, 0, 72.0f, 720.0f, "First page text");
            writeMarkedText(document, secondPage, 0, 144.0f, 620.0f, "Second page text");

            List<BoundingBox> firstPageBoundingBoxes = extractor.extract(document, List.of(new StructureContentReference(1, 0)));
            List<BoundingBox> secondPageBoundingBoxes = extractor.extract(document, List.of(new StructureContentReference(2, 0)));

            assertAll(
                    () -> assertEquals(1, firstPageBoundingBoxes.size()),
                    () -> assertEquals(1, secondPageBoundingBoxes.size()),
                    () -> assertTrue(firstPageBoundingBoxes.getFirst().x() < secondPageBoundingBoxes.getFirst().x())
            );
        }
    }

    private static void writeMarkedText(PDDocument document, PDPage page, int markedContentId, float x, float y, String text) throws IOException {
        COSDictionary markedContentProperties = new COSDictionary();
        markedContentProperties.setInt(COSName.MCID, markedContentId);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginMarkedContent(COSName.getPDFName("Span"), PDPropertyList.create(markedContentProperties));
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12.0f);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.endMarkedContent();
        }
    }
}