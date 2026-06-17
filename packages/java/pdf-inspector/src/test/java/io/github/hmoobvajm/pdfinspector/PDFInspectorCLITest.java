package io.github.hmoobvajm.pdfinspector;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PDFInspectorCLITest {

    private final PDFInspectorCLI cli = new PDFInspectorCLI();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDirectory;

    @Test
    void writesInspectionResultAsJsonAndReturnsSuccess() throws IOException {
        Path pdfPath = tempDirectory.resolve("accessible-document.pdf");
        createPdf(pdfPath, new PDPage(new PDRectangle(612.0f, 792.0f)));

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        int exitCode;

        try (PrintStream standardOutput = createPrintStream(outputBuffer); PrintStream standardError = createPrintStream(errorBuffer)) {
            exitCode = cli.run(new String[]{pdfPath.toString()}, standardOutput, standardError);
        }

        JsonNode json = objectMapper.readTree(outputBuffer.toString(StandardCharsets.UTF_8));

        assertAll(
                () -> assertEquals(PDFInspectorCLI.EXIT_SUCCESS, exitCode),
                () -> assertEquals(InspectionResultValues.SCHEMA_VERSION, json.path("schemaVersion").asText()),
                () -> assertEquals("accessible-document.pdf", json.path("sourceDocument").path("fileName").asText()),
                () -> assertEquals(1, json.path("pageCount").asInt()),
                () -> assertEquals(InspectionResultValues.COORDINATE_SYSTEM, json.path("coordinateSystem").asText()),
                () -> assertEquals(1, json.path("pages").size()),
                () -> assertEquals(612.0, json.path("pages").get(0).path("widthPoints").asDouble()),
                () -> assertEquals(792.0, json.path("pages").get(0).path("heightPoints").asDouble()),
                () -> assertTrue(json.path("structureTree").isArray()),
                () -> assertTrue(json.path("warnings").isArray()),
                () -> assertEquals("", errorBuffer.toString(StandardCharsets.UTF_8))
        );
    }

    @Test
    void returnsUsageErrorWhenNoArgumentsAreProvided() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        int exitCode;

        try (PrintStream standardOutput = createPrintStream(outputBuffer); PrintStream standardError = createPrintStream(errorBuffer)) {
            exitCode = cli.run(new String[]{}, standardOutput, standardError);
        }

        assertAll(
                () -> assertEquals(PDFInspectorCLI.EXIT_USAGE_ERROR, exitCode),
                () -> assertEquals("", outputBuffer.toString(StandardCharsets.UTF_8)),
                () -> assertEquals("Usage: pdf-inspector <pdf-path>" + System.lineSeparator(), errorBuffer.toString(StandardCharsets.UTF_8))
        );
    }

    @Test
    void returnsUsageErrorWhenTooManyArgumentsAreProvided() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        int exitCode;

        try (PrintStream standardOutput = createPrintStream(outputBuffer); PrintStream standardError = createPrintStream(errorBuffer)) {
            exitCode = cli.run(new String[]{"first.pdf", "second.pdf"}, standardOutput, standardError);
        }

        assertAll(
                () -> assertEquals(PDFInspectorCLI.EXIT_USAGE_ERROR, exitCode),
                () -> assertEquals("", outputBuffer.toString(StandardCharsets.UTF_8)),
                () -> assertEquals("Usage: pdf-inspector <pdf-path>" + System.lineSeparator(), errorBuffer.toString(StandardCharsets.UTF_8))
        );
    }

    @Test
    void returnsOperationalErrorWhenPdfDoesNotExist() {
        Path missingPath = tempDirectory.resolve("missing.pdf");
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        int exitCode;

        try (PrintStream standardOutput = createPrintStream(outputBuffer); PrintStream standardError = createPrintStream(errorBuffer)) {
            exitCode = cli.run(new String[]{missingPath.toString()}, standardOutput, standardError);
        }

        String errorOutput = errorBuffer.toString(StandardCharsets.UTF_8);

        assertAll(
                () -> assertEquals(PDFInspectorCLI.EXIT_OPERATIONAL_ERROR, exitCode),
                () -> assertEquals("", outputBuffer.toString(StandardCharsets.UTF_8)),
                () -> assertTrue(errorOutput.startsWith("PDF inspection failed:")),
                () -> assertTrue(errorOutput.contains("missing.pdf"))
        );
    }

    @Test
    void returnsUsageErrorForInvalidPathSyntax() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        int exitCode;

        try (PrintStream standardOutput = createPrintStream(outputBuffer); PrintStream standardError = createPrintStream(errorBuffer)) {
            exitCode = cli.run(new String[]{"invalid\0path.pdf"}, standardOutput, standardError);
        }

        assertAll(
                () -> assertEquals(PDFInspectorCLI.EXIT_USAGE_ERROR, exitCode),
                () -> assertEquals("", outputBuffer.toString(StandardCharsets.UTF_8)),
                () -> assertTrue(errorBuffer.toString(StandardCharsets.UTF_8).startsWith("Invalid PDF path:"))
        );
    }

    @Test
    void rejectsNullArgumentArray() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        try (PrintStream standardOutput = createPrintStream(outputBuffer); PrintStream standardError = createPrintStream(errorBuffer)) {
            assertThrows(NullPointerException.class, () -> cli.run(null, standardOutput, standardError));
        }
    }

    @Test
    void rejectsNullStandardOutput() {
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        try (PrintStream standardError = createPrintStream(errorBuffer)) {
            assertThrows(NullPointerException.class, () -> cli.run(new String[]{}, null, standardError));
        }
    }

    @Test
    void rejectsNullStandardError() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

        try (PrintStream standardOutput = createPrintStream(outputBuffer)) {
            assertThrows(NullPointerException.class, () -> cli.run(new String[]{}, standardOutput, null));
        }
    }

    @Test
    void rejectsNullConstructorDependencies() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new PDFInspectorCLI(null, new ObjectMapper())),
                () -> assertThrows(NullPointerException.class, () -> new PDFInspectorCLI(new PDFInspectionService(), null))
        );
    }

    private static PrintStream createPrintStream(ByteArrayOutputStream destination) {
        return new PrintStream(destination, true, StandardCharsets.UTF_8);
    }

    private static void createPdf(Path destination, PDPage... pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (PDPage page : pages) {
                document.addPage(page);
            }

            document.save(destination.toFile());
        }
    }

    private static final class InspectionResultValues {

        private static final String SCHEMA_VERSION = "1.0";
        private static final String COORDINATE_SYSTEM = "pdf_points_top_left";

        private InspectionResultValues() {
        }
    }
}