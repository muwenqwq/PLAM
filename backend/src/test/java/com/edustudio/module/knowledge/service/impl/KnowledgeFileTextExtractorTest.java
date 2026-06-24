package com.edustudio.module.knowledge.service.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeFileTextExtractorTest {

    private final KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor();

    @Test
    void shouldExtractDocxBeforeTrustingTextContentType() throws Exception {
        byte[] docx = docx("""
                <?xml version="1.0" encoding="UTF-8"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>\u8f6f\u4ef6\u9700\u6c42\u5206\u6790</w:t></w:r></w:p>
                    <w:p><w:r><w:t>\u7528\u4f8b\u548c\u529f\u80fd\u9700\u6c42</w:t></w:r></w:p>
                  </w:body>
                </w:document>
                """);

        String text = extractor.extract("review-guide.docx", "text/plain", docx);

        assertThat(text)
                .contains("\u8f6f\u4ef6\u9700\u6c42\u5206\u6790")
                .contains("\u7528\u4f8b\u548c\u529f\u80fd\u9700\u6c42")
                .doesNotContain("PK")
                .doesNotContain("docProps");
    }

    private byte[] docx(String documentXml) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("docProps/app.xml"));
            zip.write("<Properties/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
