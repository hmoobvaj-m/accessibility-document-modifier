package io.github.hmoobvajm.pdfinspector.model;

import java.util.List;
import java.util.Objects;

/**
 * One element in a PDF's structure tree
 * 
 * Record preserves the structure type stored directly within PDF and standard structure type obtained after applying doc's role map.
 * Metadata vals are allowed to be null/blank due to missing or malformed metadata needing to be reported by inspection rules if needed.
 * 
 * @param structureType raw structure type from PDF like {@code Figure}, {@code P}, or custome role; may be null
 * @param standardStructureType standard role after role-map resolution; may be null
 * @param title optional human-readable structure elem title
 * @param language optional language associated with elem
 * @param alternateDescription optional alternate description, commonly used by figures
 * @param actualText optional replacement text for structure elem
 * @param pageNumber optional one-based page association declared by structure elem
 * @param children immediate child structure elems in doc order
 */
public record StructureTag(String structureType, String standardStructureType, String title, String language, String alternateDescription, String actualText, Integer pageNumber, List<StructureTag> children) {
    public StructureTag {
        if(pageNumber != null && pageNumber < 1 ) { throw new IllegalArgumentException("pageNumber must be greater than or equal to 1"); }
        
        Objects.requireNonNull(children, "children must not be null");

        for(StructureTag child : children) {
            Objects.requireNonNull(child, "cihldren must not contain null elements");
        }

        children = List.copyOf(children);
    }
}