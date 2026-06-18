package io.github.hmoobvajm.pdfinspector;

import io.github.hmoobvajm.pdfinspector.model.FigureInspection;
import io.github.hmoobvajm.pdfinspector.model.StructureTag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FigureExtractor {
    private static final String FIGURE_STANDARD_STRUCTURE_TYPE = "Figure";

    public List<FigureInspection> extract(List<StructureTag> structureTree) {
        Objects.requireNonNull(structureTree, "structureTree must not be null");
        List<FigureInspection> figures = new ArrayList<>();
        extractFigures(structureTree, null, figures);

        return List.copyOf(figures);
    }

    public void extractFigures(List<StructureTag> tags, Integer inheritedPageNumber, List<FigureInspection> figures) {
        for(StructureTag tag : tags) {
            Objects.requireNonNull(tag, "structureTree must not ocntain null elems");
            Integer effectivePageNumber = tag.pageNumber() != null ? tag.pageNumber() : inheritedPageNumber;

            if(FIGURE_STANDARD_STRUCTURE_TYPE.equals(tag.standardStructureType())) {
                int figurePageNumber = resolveFigurePageNumber(tag, inheritedPageNumber);
                String figureId = "figure-" + (figures.size()+1);
                
                figures.add(new FigureInspection(figureId, figurePageNumber, List.of(), tag.alternateDescription(), tag.actualText(), tag.structureType()));
                effectivePageNumber = figurePageNumber;
            }
            extractFigures(tag.children(), effectivePageNumber, figures);
        }
    }

    private int resolveFigurePageNumber(StructureTag figureTag, Integer inheritedPageNumber) {
        if(figureTag.pageNumber() != null) { return figureTag.pageNumber(); }

        Set<Integer> descendantPageNumbers = new LinkedHashSet<>();
        collectPageNumbers(figureTag.children(), descendantPageNumbers);

        if(descendantPageNumbers.size() > 1) { throw new IllegalStateException("Figure structure tag spans multiple pages and cannot be represented by FigureInspection"); }
        if(descendantPageNumbers.size() == 1) { return descendantPageNumbers.iterator().next(); }
        if(inheritedPageNumber != null) { return inheritedPageNumber; }

        throw new IllegalStateException("Figure structure tag does not resolve to a page number");
    }

    private void collectPageNumbers(List<StructureTag> tags, Set<Integer> pageNumbers) {
        for(StructureTag tag : tags) {
            if(tag.pageNumber() != null) { pageNumbers.add(tag.pageNumber()); }
            collectPageNumbers(tag.children(), pageNumbers);
        }
    }
}