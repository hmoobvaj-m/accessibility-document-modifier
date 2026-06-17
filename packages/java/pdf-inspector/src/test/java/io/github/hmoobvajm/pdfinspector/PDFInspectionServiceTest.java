package io.github.hmoobvajm.pdfinspector;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hmoobvajm.pdfinspector.model.InspectionResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PDFInspectionServiceTest {

    private final PDFInspectionService inspectionService = new PDFInspectionService();

    @TempDir
    Path tempDirectory;

    @Test
    void inspectsValidPdf() throws IOException {
        Path pdfPath = tempDirectory.resolve("accessible-document.pdf");
        createPdf(pdfPath, new PDPage(new PDRectangle(612.0f, 792.0f)));

        InspectionResult result = inspectionService.inspect(pdfPath);

        assertAll(
                () -> assertEquals(InspectionResult.SUPPORTED_SCHEMA_VERSION, result.schemaVersion()),
                () -> assertEquals("accessible-document.pdf", result.sourceDocument().fileName()),
                () -> assertEquals(Files.size(pdfPath), result.sourceDocument().sizeBytes()),
                () -> assertEquals(calculateSha256(pdfPath), result.sourceDocument().sha256()),
                () -> assertEquals(1, result.pageCount()),
                () -> assertEquals(InspectionResult.SUPPORTED_COORDINATE_SYSTEM, result.coordinateSystem()),
                () -> assertEquals(1, result.pages().size()),
                () -> assertEquals(612.0, result.pages().getFirst().widthPoints()),
                () -> assertEquals(792.0, result.pages().getFirst().heightPoints()),
                () -> assertEquals(1, result.pages().getFirst().pageNumber()),
                () -> assertEquals(0, result.pages().getFirst().figures().size()),
                () -> assertEquals(0, result.structureTree().size()),
                () -> assertEquals(0, result.warnings().size())
        );
    }

    @Test
    void calculatesLowercaseSha256ForSourceDocument() throws IOException {
        Path pdfPath = tempDirectory.resolve("hash-test.pdf");
        createPdf(pdfPath, new PDPage(new PDRectangle(612.0f, 792.0f)));

        InspectionResult result = inspectionService.inspect(pdfPath);

        assertAll(
                () -> assertEquals(calculateSha256(pdfPath), result.sourceDocument().sha256()),
                () -> assertTrue(result.sourceDocument().sha256().matches("[0-9a-f]{64}"))
        );
    }

    @Test
    void preservesPageOrderAndDimensions() throws IOException {
        Path pdfPath = tempDirectory.resolve("multiple-pages.pdf");
        PDPage firstPage = new PDPage(new PDRectangle(612.0f, 792.0f));
        PDPage secondPage = new PDPage(new PDRectangle(595.0f, 842.0f));

        createPdf(pdfPath, firstPage, secondPage);

        InspectionResult result = inspectionService.inspect(pdfPath);

        assertAll(
                () -> assertEquals(2, result.pageCount()),
                () -> assertEquals(1, result.pages().get(0).pageNumber()),
                () -> assertEquals(612.0, result.pages().get(0).widthPoints()),
                () -> assertEquals(792.0, result.pages().get(0).heightPoints()),
                () -> assertEquals(2, result.pages().get(1).pageNumber()),
                () -> assertEquals(595.0, result.pages().get(1).widthPoints()),
                () -> assertEquals(842.0, result.pages().get(1).heightPoints())
        );
    }

    @Test
    void usesCropBoxForVisiblePageDimensions() throws IOException {
        Path pdfPath = tempDirectory.resolve("cropped-page.pdf");
        PDPage page = new PDPage(new PDRectangle(612.0f, 792.0f));
        page.setCropBox(new PDRectangle(500.0f, 700.0f));

        createPdf(pdfPath, page);

        InspectionResult result = inspectionService.inspect(pdfPath);

        assertAll(
                () -> assertEquals(500.0, result.pages().getFirst().widthPoints()),
                () -> assertEquals(700.0, result.pages().getFirst().heightPoints())
        );
    }

    @Test
    void swapsPageDimensionsForQuarterTurnRotation() throws IOException {
        Path pdfPath = tempDirectory.resolve("rotated-page.pdf");
        PDPage page = new PDPage(new PDRectangle(612.0f, 792.0f));
        page.setRotation(90);

        createPdf(pdfPath, page);

        InspectionResult result = inspectionService.inspect(pdfPath);

        assertAll(
                () -> assertEquals(792.0, result.pages().getFirst().widthPoints()),
                () -> assertEquals(612.0, result.pages().getFirst().heightPoints())
        );
    }

    @Test
    void rejectsNullPath() {
        assertThrows(NullPointerException.class, () -> inspectionService.inspect(null));
    }

    @Test
    void rejectsMissingFile() {
        Path missingPath = tempDirectory.resolve("missing.pdf");

        assertThrows(NoSuchFileException.class, () -> inspectionService.inspect(missingPath));
    }

    @Test
    void rejectsDirectoryPath() {
        assertThrows(IOException.class, () -> inspectionService.inspect(tempDirectory));
    }

    @Test
    void rejectsEmptyFile() throws IOException {
        Path emptyPath = tempDirectory.resolve("empty.pdf");
        Files.createFile(emptyPath);

        assertThrows(IOException.class, () -> inspectionService.inspect(emptyPath));
    }

    @Test
    void rejectsMalformedPdf() throws IOException {
        Path malformedPath = tempDirectory.resolve("malformed.pdf");
        Files.writeString(malformedPath, "This file is not a PDF.");

        assertThrows(IOException.class, () -> inspectionService.inspect(malformedPath));
    }

    private static void createPdf(Path destination, PDPage... pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (PDPage page : pages) {
                document.addPage(page);
            }

            document.save(destination.toFile());
        }
    }

    private static String calculateSha256(Path sourcePath) throws IOException {
        try {
            byte[] sourceBytes = Files.readAllBytes(sourcePath);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sourceBytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable in the test runtime", exception);
        }
    }
}