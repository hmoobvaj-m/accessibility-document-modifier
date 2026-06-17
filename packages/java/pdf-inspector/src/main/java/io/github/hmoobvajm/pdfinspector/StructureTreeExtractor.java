package io.github.hmoobvajm.pdfinspector;

import io.github.hmoobvajm.pdfinspector.model.StructureTag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;

/**
 * Converts a PDFBox logical structure tree into the project's immutable structure-tag model.
 */
public final class StructureTreeExtractor {

    /**
     * Extracts the semantic structure elems from PDF doc.
     *
     * @param document loaded PDF document
     * @return immutable root structure-tag list in document order
     * @throws IOException if the structure tree is malformed or cyclic
     */
    public List<StructureTag> extract(PDDocument document) throws IOException {
        Objects.requireNonNull(document, "document must not be null");

        try {
            PDStructureTreeRoot structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            if (structureTreeRoot == null) { return List.of(); }

            Set<COSDictionary> activePath = Collections.newSetFromMap(new IdentityHashMap<>());
            return extractStructureChildren(document, structureTreeRoot.getKids(), activePath);
        }

        catch (IOException exception) { throw exception; }

        catch (RuntimeException exception) { throw new IOException("Unable to extract the PDF logical structure tree", exception); }
    }

    private static List<StructureTag> extractStructureChildren(PDDocument document, List<Object> kids, Set<COSDictionary> activePath) throws IOException {
        List<StructureTag> children = new ArrayList<>();

        for (Object kid : kids) {
            if (kid instanceof PDStructureElement structureElement) { children.add(extractStructureTag(document, structureElement, activePath)); }
        }

        return List.copyOf(children);
    }

    private static StructureTag extractStructureTag(PDDocument document, PDStructureElement structureElement, Set<COSDictionary> activePath) throws IOException {
        COSDictionary structureDictionary = structureElement.getCOSObject();

        if (!activePath.add(structureDictionary)) { throw new IOException("Cycle detected in the PDF logical structure tree"); }

        try {
            List<StructureTag> children = extractStructureChildren(document, structureElement.getKids(), activePath);
            Integer pageNumber = resolvePageNumber(document, structureElement.getPage());

            return new StructureTag(structureElement.getStructureType(), structureElement.getStandardStructureType(), structureElement.getTitle(), structureElement.getLanguage(), structureElement.getAlternateDescription(), structureElement.getActualText(), pageNumber, children);
        }

        finally { activePath.remove(structureDictionary); }
    }

    private static Integer resolvePageNumber(PDDocument document, PDPage page) {
        if (page == null) { return null; }

        int pageIndex = document.getPages().indexOf(page);
        return pageIndex >= 0 ? pageIndex + 1 : null;
    }
}