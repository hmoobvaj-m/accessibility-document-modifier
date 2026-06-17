package io.github.hmoobvajm.pdfinspector.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Complete immutable result produced from inspecting a single PDF doc
 * 
 * @param schemaVersion version of serialized inspection contract
 * @param sourceDocument metadata identifying inspected src doc
 * @param pageCount number of pages in inspected document
 * @param coordinateSystem coordinated convention used by all extracted geometry
 * @param pages page inspection results in document order 
 * @param structureTree root elements from PDF logical structure tree
 * @param warnings non-fatal issues encountered during inspection
 */
public record InspectionResult(String schemaVersion, SourceDocument sourceDocument, int pageCount, String coordinateSystem, List<PageInspection> pages, List<StructureTag> structureTree, List<InspectionWarning> warnings) {
    public static final String SUPPORTED_SCHEMA_VERSION = "1.0";
    public static final String SUPPORTED_COORDINATE_SYSTEM = "pdf_points_top_left";

    public InspectionResult {
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(sourceDocument, "sourceDocument must not be null");
        Objects.requireNonNull(coordinateSystem, "coordinateSystem must not be null");
        Objects.requireNonNull(pages, "pages must not be null");
        Objects.requireNonNull(structureTree, "structureTree must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");

        if (!SUPPORTED_SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("schemaVersion must equal " + SUPPORTED_SCHEMA_VERSION);
        if (pageCount < 0) throw new IllegalArgumentException("pageCount must be greater than or equal to 0");
        if (!SUPPORTED_COORDINATE_SYSTEM.equals(coordinateSystem)) throw new IllegalArgumentException("coordinateSystem must equal " + SUPPORTED_COORDINATE_SYSTEM);
        if (pageCount != pages.size()) throw new IllegalArgumentException("pageCount must equal the number of pages");

        Set<String> figureIds = new HashSet<>();

        for (int index = 0; index < pages.size(); index++) {
            PageInspection page = Objects.requireNonNull(pages.get(index), "pages must not contain null elements");
            int expectedPageNumber = index + 1;

            if (page.pageNumber() != expectedPageNumber) throw new IllegalArgumentException("page numbers must be sequential starting at 1");

            for (FigureInspection figure : page.figures()) {
                if (!figureIds.add(figure.figureId())) throw new IllegalArgumentException("figureId values must be unique across the document");
            }
        }

        for (StructureTag structureTag : structureTree) {
            Objects.requireNonNull(structureTag, "structureTree must not contain null elements");
            validateStructureTagPageNumbers(structureTag, pageCount);
        }

        for (InspectionWarning warning : warnings) {
            Objects.requireNonNull(warning, "warnings must not contain null elements");
            if (warning.pageNumber() != null && warning.pageNumber() > pageCount) throw new IllegalArgumentException("warning pageNumber must not exceed pageCount");
        }

        pages = List.copyOf(pages);
        structureTree = List.copyOf(structureTree);
        warnings = List.copyOf(warnings);
    }

    private static void validateStructureTagPageNumbers(StructureTag structureTag, int pageCount) {
        if (structureTag.pageNumber() != null && structureTag.pageNumber() > pageCount) throw new IllegalArgumentException("structure tag pageNumber must not exceed pageCount");
        for (StructureTag child : structureTag.children()) {
            validateStructureTagPageNumbers(child, pageCount);
        }
    }
}