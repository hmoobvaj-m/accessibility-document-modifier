package io.github.hmoobvajm.pdfinspector;

import io.github.hmoobvajm.pdfinspector.model.StructureContentReference;
import io.github.hmoobvajm.pdfinspector.model.StructureTag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
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
            Integer pageNumber = resolvePageNumber(document, structureElement.getPage());
            List<StructureContentReference> contentReferences = extractContentReferences(document, structureElement.getKids(), pageNumber);
            List<StructureTag> children = extractStructureChildren(document, structureElement.getKids(), activePath);

            return new StructureTag(structureElement.getStructureType(), structureElement.getStandardStructureType(), structureElement.getTitle(), structureElement.getLanguage(), structureElement.getAlternateDescription(), structureElement.getActualText(), pageNumber, children, contentReferences);
        }

        finally { activePath.remove(structureDictionary); }
    }

    private static List<StructureContentReference> extractContentReferences(PDDocument document, List<Object> kids, Integer structurePageNumber) {
        List<StructureContentReference> contentReferences = new ArrayList<>();

        for (Object kid : kids) {
            if (kid instanceof PDMarkedContentReference markedContentReference) { addMarkedContentReference(document, contentReferences, markedContentReference, structurePageNumber); }

            if (kid instanceof COSInteger markedContentIdentifier && structurePageNumber != null) {
                contentReferences.add(new StructureContentReference(structurePageNumber, markedContentIdentifier.intValue()));
            }
        }

        return List.copyOf(contentReferences);
    }

    private static void addMarkedContentReference(PDDocument document, List<StructureContentReference> contentReferences, PDMarkedContentReference markedContentReference, Integer fallbackPageNumber) {
        Integer pageNumber = resolvePageNumber(document, markedContentReference.getPage());

        if (pageNumber == null) {
            pageNumber = fallbackPageNumber;
        }

        if (pageNumber == null) { return; }

        contentReferences.add(new StructureContentReference(pageNumber, markedContentReference.getMCID()));
    }

    private static Integer resolvePageNumber(PDDocument document, PDPage page) {
        if (page == null) { return null; }

        int pageIndex = document.getPages().indexOf(page);
        return pageIndex >= 0 ? pageIndex + 1 : null;
    }
}