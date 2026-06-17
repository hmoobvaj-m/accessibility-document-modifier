package io.github.hmoobvajm.pdfinspector.model;

/*
    Immutable rectangular region expressed via coordinate system declared by inspection result
    @param x horizontal coordinate of rectangle origin
    @param y vertical coordinate of rectangle origin
    @param width rectangle width in coordinate system units
    @param ehight rectangle height in coordinate system units
*/
public record BoundingBox(double x, double y, double width, double height) {
    /*
        Validates invariants of bounding box
        Zero-sized dimensions are allowed in order for inspection results to report malformed source geometry
    */
   public BoundingBox {
        requireFinite("x", x);
        requireFinite("y", y);
        requireFinite("width", width);
        requireFinite("height", height);

        if(width < 0.0) { throw new IllegalArgumentException("width must be greater than or equal to 0"); }
        if(height <0.0) { throw new IllegalArgumentException("height must be greater than or equal to 0"); }    
   }

   private static void requireFinite(String fieldName, double value) {
        if(!Double.isFinite(value)) { throw new IllegalArgumentException(fieldName + " must be finite"); }
   }
}