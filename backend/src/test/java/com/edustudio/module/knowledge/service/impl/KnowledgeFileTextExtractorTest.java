package com.edustudio.module.knowledge.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeFileTextExtractorTest {

    private final KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor();

    @Test
    void shouldExtractDocxBeforeTrustingTextContentType() throws Exception {
        byte[] docx = docx("""
                <?xml version="1.0" encoding="UTF-8"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>软件需求分析</w:t></w:r></w:p>
                    <w:p><w:r><w:t>用例和功能需求</w:t></w:r></w:p>
                  </w:body>
                </w:document>
                """);

        String text = extractor.extract("review-guide.docx", "text/plain", docx);

        assertThat(text)
                .contains("软件需求分析")
                .contains("用例和功能需求")
                .doesNotContain("PK")
                .doesNotContain("docProps");
    }

    @Test
    void shouldExtractTextFromPdf() throws Exception {
        byte[] pdf = pdf("database index review guide");

        String text = extractor.extract("database.pdf", "application/pdf", pdf);

        assertThat(text).contains("database index review guide");
    }

    @Test
    void shouldRejectScannedPdfWithoutText() throws Exception {
        byte[] pdf = blankPdf();

        assertThatThrownBy(() -> extractor.extract("scan.pdf", "application/pdf", pdf))
                .hasMessageContaining("文字版 PDF");
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

    private byte[] pdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText(text);
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] blankPdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}