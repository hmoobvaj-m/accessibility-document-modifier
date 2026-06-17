package io.github.hmoobvajm.pdfinspector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hmoobvajm.pdfinspector.model.InspectionResult;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * CLI entry point for inspecting a PDF and writing JSON result.
 */
public final class PDFInspectorCLI {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_OPERATIONAL_ERROR = 1;
    static final int EXIT_USAGE_ERROR = 2;

    private final PDFInspectionService inspectionService;
    private final ObjectMapper objectMapper;

    public PDFInspectorCLI() {
        this(new PDFInspectionService(), new ObjectMapper());
    }

    PDFInspectorCLI(PDFInspectionService inspectionService, ObjectMapper objectMapper) {
        this.inspectionService = Objects.requireNonNull(inspectionService, "inspectionService must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public static void main(String[] args) {
        int exitCode = new PDFInspectorCLI().run(args, System.out, System.err);

        if (exitCode != EXIT_SUCCESS) System.exit(exitCode);
    }

    int run(String[] args, PrintStream standardOutput, PrintStream standardError) {
        Objects.requireNonNull(args, "args must not be null");
        Objects.requireNonNull(standardOutput, "standardOutput must not be null");
        Objects.requireNonNull(standardError, "standardError must not be null");

        if (args.length != 1) {
            standardError.println("Usage: pdf-inspector <pdf-path>");
            return EXIT_USAGE_ERROR;
        }

        try {
            Path pdfPath = Path.of(args[0]);
            InspectionResult result = inspectionService.inspect(pdfPath);
            standardOutput.println(serialize(result));
            return EXIT_SUCCESS;
        } 
        
        catch (InvalidPathException exception) {
            standardError.println("Invalid PDF path: " + exception.getInput());
            return EXIT_USAGE_ERROR;
        } 
        
        catch (IOException exception) {
            standardError.println("PDF inspection failed: " + exception.getMessage());
            return EXIT_OPERATIONAL_ERROR;
        }
    }

    private String serialize(InspectionResult result) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
}