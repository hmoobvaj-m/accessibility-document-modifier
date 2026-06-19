package io.github.hmoobvajm.pdfinspector;

import io.github.hmoobvajm.pdfinspector.model.FigureInspection;
import io.github.hmoobvajm.pdfinspector.model.InspectionResult;
import io.github.hmoobvajm.pdfinspector.model.PageInspection;
import io.github.hmoobvajm.pdfinspector.model.SourceDocument;
import io.github.hmoobvajm.pdfinspector.model.StructureTag;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Loads a PDF and converts its extracted information into the project's language-neutral inspection model.
 */
public final class PDFInspectionService {

    private static final int HASH_BUFFER_SIZE = 8_192;

    private final StructureTreeExtractor structureTreeExtractor;
    private final FigureExtractor figureExtractor;

    public PDFInspectionService() {
        this(new StructureTreeExtractor(), new FigureExtractor());
    }

    PDFInspectionService(StructureTreeExtractor structureTreeExtractor) {
        this(structureTreeExtractor, new FigureExtractor());
    }

    PDFInspectionService(StructureTreeExtractor structureTreeExtractor, FigureExtractor figureExtractor) {
        this.structureTreeExtractor = Objects.requireNonNull(structureTreeExtractor, "structureTreeExtractor must not be null");
        this.figureExtractor = Objects.requireNonNull(figureExtractor, "figureExtractor must not be null");
    }

    /**
     * Inspects one PDF file.
     *
     * @param pdfPath path to the src PDF
     * @return immutable inspection result
     * @throws IOException if the src cannot be read or parsed as a PDF
     */
    public InspectionResult inspect(Path pdfPath) throws IOException {
        Objects.requireNonNull(pdfPath, "pdfPath must not be null");

        Path normalizedPath = pdfPath.toAbsolutePath().normalize();
        validateSourcePath(normalizedPath);

        SourceDocument sourceDocument = inspectSourceDocument(normalizedPath);

        try (PDDocument document = Loader.loadPDF(normalizedPath.toFile())) {
            List<StructureTag> structureTree = structureTreeExtractor.extract(document);
            List<FigureInspection> figures = figureExtractor.extract(structureTree);
            List<PageInspection> pages = inspectPages(document, figures);

            return new InspectionResult(
                    InspectionResult.SUPPORTED_SCHEMA_VERSION,
                    sourceDocument,
                    pages.size(),
                    InspectionResult.SUPPORTED_COORDINATE_SYSTEM,
                    pages,
                    structureTree,
                    List.of()
            );
        }
    }

    private static void validateSourcePath(Path sourcePath) throws IOException {
        if (Files.notExists(sourcePath)) throw new NoSuchFileException(sourcePath.toString());
        if (!Files.isRegularFile(sourcePath)) throw new IOException("PDF source must be a regular file: " + sourcePath);
        if (Files.size(sourcePath) == 0L) throw new IOException("PDF source must not be empty: " + sourcePath);
    }

    private static SourceDocument inspectSourceDocument(Path sourcePath) throws IOException {
        String fileName = sourcePath.getFileName().toString();
        long sizeBytes = Files.size(sourcePath);
        String sha256 = calculateSha256(sourcePath);

        return new SourceDocument(fileName, sizeBytes, sha256);
    }

    private static List<PageInspection> inspectPages(PDDocument document, List<FigureInspection> figures) throws IOException {
        int pageCount = document.getNumberOfPages();
        Map<Integer, List<FigureInspection>> figuresByPage = groupFiguresByPage(figures, pageCount);
        List<PageInspection> pages = new ArrayList<>(pageCount);

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int pageNumber = pageIndex + 1;
            List<FigureInspection> pageFigures = figuresByPage.getOrDefault(pageNumber, List.of());
            pages.add(inspectPage(document.getPage(pageIndex), pageNumber, pageFigures));
        }

        return List.copyOf(pages);
    }

    private static Map<Integer, List<FigureInspection>> groupFiguresByPage(List<FigureInspection> figures, int pageCount) throws IOException {
        Objects.requireNonNull(figures, "figures must not be null");

        for (FigureInspection figure : figures) {
            Objects.requireNonNull(figure, "figures must not contain null elements");
            if (figure.pageNumber() > pageCount) { throw new IOException("Figure " + figure.figureId() + " references page " + figure.pageNumber() + ", but the document page count is " + pageCount); }
        }

        return Map.copyOf(figures.stream().collect(Collectors.groupingBy(FigureInspection::pageNumber, Collectors.toUnmodifiableList())));
    }

    private static PageInspection inspectPage(PDPage page, int pageNumber, List<FigureInspection> figures) throws IOException {
        PDRectangle visibleBox = page.getCropBox();

        if (visibleBox == null) { throw new IOException("Page " + pageNumber + " does not define a usable crop box"); }

        double widthPoints = visibleBox.getWidth();
        double heightPoints = visibleBox.getHeight();
        int normalizedRotation = Math.floorMod(page.getRotation(), 360);

        if (normalizedRotation == 90 || normalizedRotation == 270) {
            double originalWidth = widthPoints;
            widthPoints = heightPoints;
            heightPoints = originalWidth;
        }

        try {
            return new PageInspection(pageNumber, widthPoints, heightPoints, figures);
        } 
        
        catch (IllegalArgumentException exception) {
            throw new IOException("Page " + pageNumber + " contains invalid page geometry", exception);
        }
    }

    private static String calculateSha256(Path sourcePath) throws IOException {
        MessageDigest digest = createSha256Digest();
        byte[] buffer = new byte[HASH_BUFFER_SIZE];

        try (InputStream input = new BufferedInputStream(Files.newInputStream(sourcePath))) {
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } 
        
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in the current Java runtime", exception);
        }
    }
}