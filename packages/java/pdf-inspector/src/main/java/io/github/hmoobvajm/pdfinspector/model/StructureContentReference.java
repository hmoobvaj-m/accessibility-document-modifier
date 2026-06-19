package io.github.hmoobvajm.pdfinspector.model;

import java.util.Objects;

/**
 * One direct content reference associated with pdf structure elem
 * 
 * Reference keeps logical structure tree connected to marked page content without forcing geometry extraction
 * 
 * @param pageNumber one-based page number where reference content is declared
 * @param markedContentId marked-content identifier used by pdf content stream
 */
public record StructureContentReference(Integer pageNumber, Integer markedContentId) {
    public StructureContentReference {
        Objects.requireNonNull(pageNumber, "pageNumber must not be null");
        Objects.requireNonNull(markedContentId, "markedContentId must not be null");

        if(pageNumber < 1) { throw new IllegalArgumentException("pageNumber must be greater than or equal to 1"); }
        if(markedContentId < 0) { throw new IllegalArgumentException("markedContentId must be greater than or equal to 0"); }
    }
}