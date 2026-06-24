package io.github.hmoobvajm.pdfinspector;

import io.github.hmoobvajm.pdfinspector.model.BoundingBox;
import io.github.hmoobvajm.pdfinspector.model.StructureContentReference;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MarkedContentBoundingBoxExtractor {
    public List<BoundingBox> extract(PDDocument document, List<StructureContentReference> contentReferences) throws IOException {
        Objects.requireNonNull(document, "document must not be null");
        Objects.requireNonNull(contentReferences, "contentReferences must not be null");

        for (StructureContentReference contentReference : contentReferences) {
            Objects.requireNonNull(contentReference, "contentReferences must not contain null elements");
        }

        if (contentReferences.isEmpty()) {
            return List.of();
        }

        List<BoundingBox> boundingBoxes = new ArrayList<>();

        for (StructureContentReference contentReference : contentReferences) {
            TextMarkedContentCollector collector = new TextMarkedContentCollector(contentReference.markedContentId());
            collector.setStartPage(contentReference.pageNumber());
            collector.setEndPage(contentReference.pageNumber());
            collector.getText(document);

            collector.toBoundingBox().ifPresent(boundingBoxes::add);
        }

        return List.copyOf(boundingBoxes);
    }

    private static final class TextMarkedContentCollector extends PDFTextStripper {
        private final int targetMarkedContentId;
        private final ArrayDeque<Integer> activeMarkedContentIds = new ArrayDeque<>();
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        private TextMarkedContentCollector(int targetMarkedContentId) throws IOException {
            this.targetMarkedContentId = targetMarkedContentId;
            setSortByPosition(false);
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String operation = operator.getName();

            if ("BDC".equals(operation)) {
                activeMarkedContentIds.push(extractMarkedContentId(operands));
            }

            super.processOperator(operator, operands);

            if ("EMC".equals(operation) && !activeMarkedContentIds.isEmpty()) {
                activeMarkedContentIds.pop();
            }
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            if (!activeMarkedContentIds.contains(targetMarkedContentId)) {
                return;
            }

            double x = text.getXDirAdj();
            double y = text.getYDirAdj();
            double width = text.getWidthDirAdj();
            double height = text.getHeightDir();

            minX = Math.min(minX, x);
            minY = Math.min(minY, y - height);
            maxX = Math.max(maxX, x + width);
            maxY = Math.max(maxY, y);
        }

        private java.util.Optional<BoundingBox> toBoundingBox() {
            if (!hasTextBounds()) {
                return java.util.Optional.empty();
            }

            double x = minX;
            double y = minY;
            double width = maxX - minX;
            double height = maxY - minY;

            return java.util.Optional.of(new BoundingBox(x, y, width, height));
        }

        private boolean hasTextBounds() {
            return Double.isFinite(minX) && Double.isFinite(minY) && Double.isFinite(maxX) && Double.isFinite(maxY);
        }

        private static Integer extractMarkedContentId(List<COSBase> operands) {
            if (operands.size() < 2 || !(operands.get(1) instanceof COSDictionary properties)) {
                return null;
            }

            if (!properties.containsKey(COSName.MCID)) {
                return null;
            }

            return properties.getInt(COSName.MCID);
        }
    }
}