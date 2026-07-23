package com.edustudio.module.resource.export;

public record ExportedDocument(byte[] bytes, String mediaType, String extension) {
}
