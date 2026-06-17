package io.github.hmoobvajm.pdfinspector.model;

import java.util.List;
import java.util.Objects;

/**
 * Inspection data for one figure or image discovered in a PDF.
 *
 * The model preserves source metadata even when that metadata is missing or blank. 
 * Accessibility rules are responsible for determining whether the figure has an acceptable alternate description.
 *
 * @param figureId stable identifier assigned by inspection process
 * @param pageNumber one-based page on which the figure was discovered
 * @param boundingBoxes visual regions occupied by the figure on the page
 * @param alternateDescription alternate description obtained from the PDF
 * @param actualText replacement text obtained from the PDF structure element
 * @param structureType raw structure type associated with the figure
 */
public record FigureInspection(String figureId, int pageNumber, List<BoundingBox> boundingBoxes, String alternateDescription, String actualText, String structureType) {

    /**
     * Validates representation-level invariants and creates an immutable copy
     * of the bounding-box list.
     */
    public FigureInspection {
        Objects.requireNonNull(figureId, "figureId must not be null");
        Objects.requireNonNull(boundingBoxes, "boundingBoxes must not be null");

        if (figureId.isBlank()) throw new IllegalArgumentException("figureId must not be blank");
        if (pageNumber < 1) throw new IllegalArgumentException("pageNumber must be greater than or equal to 1");

        for (BoundingBox boundingBox : boundingBoxes) {
            Objects.requireNonNull(boundingBox, "boundingBoxes must not contain null elements");
        }

        boundingBoxes = List.copyOf(boundingBoxes);
    }
}