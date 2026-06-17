package io.github.hmoobvajm.pdfinspector.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BoundingBoxTest {

    @Test
    void createsBoundingBoxWithValidValues() {
        BoundingBox boundingBox = new BoundingBox(12.5, 24.0, 100.25, 200.75);

        assertAll(
                () -> assertEquals(12.5, boundingBox.x()),
                () -> assertEquals(24.0, boundingBox.y()),
                () -> assertEquals(100.25, boundingBox.width()),
                () -> assertEquals(200.75, boundingBox.height())
        );
    }

    @Test
    void allowsNegativeCoordinates() {
        BoundingBox boundingBox = new BoundingBox(-12.5, -24.0, 100.0, 200.0);

        assertAll(
                () -> assertEquals(-12.5, boundingBox.x()),
                () -> assertEquals(-24.0, boundingBox.y())
        );
    }

    @Test
    void allowsZeroDimensions() {
        BoundingBox boundingBox = new BoundingBox(12.5, 24.0, 0.0, 0.0);

        assertAll(
                () -> assertEquals(0.0, boundingBox.width()),
                () -> assertEquals(0.0, boundingBox.height())
        );
    }

    @Test
    void rejectsNegativeWidth() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, -1.0, 200.0));
    }

    @Test
    void rejectsNegativeHeight() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, 100.0, -1.0));
    }

    @Test
    void rejectsNonFiniteXCoordinate() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(Double.NaN, 24.0, 100.0, 200.0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(Double.POSITIVE_INFINITY, 24.0, 100.0, 200.0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(Double.NEGATIVE_INFINITY, 24.0, 100.0, 200.0))
        );
    }

    @Test
    void rejectsNonFiniteYCoordinate() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, Double.NaN, 100.0, 200.0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, Double.POSITIVE_INFINITY, 100.0, 200.0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, Double.NEGATIVE_INFINITY, 100.0, 200.0))
        );
    }

    @Test
    void rejectsNonFiniteWidth() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, Double.NaN, 200.0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, Double.POSITIVE_INFINITY, 200.0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, Double.NEGATIVE_INFINITY, 200.0))
        );
    }

    @Test
    void rejectsNonFiniteHeight() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, 100.0, Double.NaN)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, 100.0, Double.POSITIVE_INFINITY)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BoundingBox(12.5, 24.0, 100.0, Double.NEGATIVE_INFINITY))
        );
    }
}