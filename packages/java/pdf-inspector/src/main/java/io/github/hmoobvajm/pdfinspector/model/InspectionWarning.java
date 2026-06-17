package io.github.hmoobvajm.pdfinspector.model;

import java.util.Objects;

/**
 * Non-fatal issue encountered during inspection of PDF
 * Warnings preserve conditions that could not be represented as normal inspection data without stopping the entire process
 * 
 * @param code stable machine-readable warning code
 * @param message human-readable explanation of the warning
 * @param pageNumber optional one-based page associated with warning
 * @param relatedObjectId optional identifier of the figure or other extracted object associated with warning
 */
public record InspectionWarning(String code, String message, Integer pageNumber, String relatedObjectId) {
    public InspectionWarning {
        Objects.requireNonNull(code, "code mnust not be null");
        Objects.requireNonNull(message, "message must not be null");

        if(code.isBlank()) { throw new IllegalArgumentException("code must not be blank"); }
        if(message.isBlank()) { throw new IllegalArgumentException("message must not be blank"); }
        if(pageNumber != null && pageNumber < 1) { throw new IllegalArgumentException("pageNumber must be greater than or equal to 1"); }
        if(relatedObjectId != null && relatedObjectId.isBlank()) { throw new IllegalArgumentException("relatedObjectId must not be blank when provided"); }
    }
}