package io.github.hmoobvajm.pdfinspector.model;

import java.util.Objects;

public record SourceDocument(String fileName, long sizeBytes, String sha256) {
    public SourceDocument {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(sha256, "sha256");
        
        if(fileName.isBlank()) { throw new IllegalArgumentException("fileName must not be blank"); }
        if(sizeBytes <= 0 ) { throw new IllegalArgumentException("sizeBytes must be greater than zero"); }
        if(!sha256.matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("sha256 must contain exactly 64 lowercase hexadecimal chars"); }
    }
}