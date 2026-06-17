package io.github.hmoobvajm.pdfinspector.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageInspectionTest {

    @Test
    void createsPageInspectionWithValidValues() {
        BoundingBox boundingBox = new BoundingBox(10.0, 20.0, 100.0, 200.0);
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(boundingBox), "Process diagram", null, "Figure");
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of(figure));

        assertAll(
                () -> assertEquals(1, page.pageNumber()),
                () -> assertEquals(612.0, page.widthPoints()),
                () -> assertEquals(792.0, page.heightPoints()),
                () -> assertEquals(List.of(figure), page.figures())
        );
    }

    @Test
    void allowsEmptyFigureList() {
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of());

        assertEquals(List.of(), page.figures());
    }

    @Test
    void rejectsNonPositivePageNumber() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(0, 612.0, 792.0, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(-1, 612.0, 792.0, List.of()))
        );
    }

    @Test
    void rejectsNonFiniteWidth() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, Double.NaN, 792.0, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, Double.POSITIVE_INFINITY, 792.0, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, Double.NEGATIVE_INFINITY, 792.0, List.of()))
        );
    }

    @Test
    void rejectsNonFiniteHeight() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, 612.0, Double.NaN, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, 612.0, Double.POSITIVE_INFINITY, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, 612.0, Double.NEGATIVE_INFINITY, List.of()))
        );
    }

    @Test
    void rejectsNonPositiveWidth() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, 0.0, 792.0, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, -1.0, 792.0, List.of()))
        );
    }

    @Test
    void rejectsNonPositiveHeight() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, 612.0, 0.0, List.of())),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, 612.0, -1.0, List.of()))
        );
    }

    @Test
    void rejectsNullFigureList() {
        assertThrows(NullPointerException.class, () -> new PageInspection(1, 612.0, 792.0, null));
    }

    @Test
    void rejectsNullFigureElement() {
        List<FigureInspection> figures = new ArrayList<>();
        figures.add(null);

        assertThrows(NullPointerException.class, () -> new PageInspection(1, 612.0, 792.0, figures));
    }

    @Test
    void rejectsFigureFromDifferentPage() {
        FigureInspection figure = new FigureInspection("figure-1", 2, List.of(), null, null, "Figure");

        assertThrows(IllegalArgumentException.class, () -> new PageInspection(1, 612.0, 792.0, List.of(figure)));
    }

    @Test
    void defensivelyCopiesFigureList() {
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(), null, null, "Figure");
        List<FigureInspection> mutableFigures = new ArrayList<>();
        mutableFigures.add(figure);

        PageInspection page = new PageInspection(1, 612.0, 792.0, mutableFigures);
        mutableFigures.clear();

        assertEquals(List.of(figure), page.figures());
    }

    @Test
    void exposesUnmodifiableFigureList() {
        FigureInspection figure = new FigureInspection("figure-1", 1, List.of(), null, null, "Figure");
        PageInspection page = new PageInspection(1, 612.0, 792.0, List.of(figure));

        assertThrows(UnsupportedOperationException.class, () -> page.figures().add(figure));
    }
}