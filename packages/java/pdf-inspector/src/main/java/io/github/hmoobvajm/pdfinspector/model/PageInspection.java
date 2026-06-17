package io.github.hmoobvajm.pdfinspector.model;

import java.util.List;
import java.util.Objects;

/**
 * Inspection data collected for one PDF page
 * 
 * @param pageNumber one-based page number
 * @param widthPoints page width in PDF points
 * @param heightPoints page height in PDF points
 * @param figures figures discoverd on a page
 */
public record PageInspection(int pageNumber, double widthPoints, double heightPoints, List<FigureInspection> figures) {
    public PageInspection {
        Objects.requireNonNull(figures, "figures must not be null");
        
        if(pageNumber < 1) { throw new IllegalArgumentException("pageNumber must be greater than or equal to 1"); }

        requireFinite("widthPoints", widthPoints);
        requireFinite("heightPoints", heightPoints);

        if(widthPoints <= 0.0) { throw new IllegalArgumentException("widthPoints must be greater than or equal to 0.0"); }
        if(heightPoints <= 0.0) { throw new IllegalArgumentException("heightPoints must be greater than or equal to 0.0"); }

        for(FigureInspection figure : figures) {
            Objects.requireNonNull(figure, "figures must not contain null elems");
            if(figure.pageNumber() != pageNumber) { throw new IllegalArgumentException("figure pageNumber must match the containing pageNumber"); }
        }

        figures = List.copyOf(figures);
    }

    private static void requireFinite(String fieldName, double value) {
        if(!Double.isFinite(value)) { throw new IllegalArgumentException(fieldName + " must be finite"); }
    }
}