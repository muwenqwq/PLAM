package com.edustudio.module.resource.export;

import com.edustudio.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentExportServiceTest {

    private final DocumentExportService service = new DocumentExportService();
    private final String markdown = "# 数据库复习\n\n- 索引\n- 事务\n\n请结合例题复习。";

    @Test
    void shouldExportUserFriendlyFormats() {
        ExportedDocument word = service.export("数据库复习", markdown, "docx");
        ExportedDocument pdf = service.export("数据库复习", markdown, "pdf");
        ExportedDocument image = service.export("数据库复习", markdown, "png");
        ExportedDocument source = service.export("数据库复习", markdown, "md");

        assertThat(word.extension()).isEqualTo("docx");
        assertThat(word.mediaType()).contains("officedocument.wordprocessingml.document");
        assertThat(word.bytes()).startsWith((byte) 'P', (byte) 'K');

        assertThat(pdf.extension()).isEqualTo("pdf");
        assertThat(pdf.mediaType()).isEqualTo("application/pdf");
        assertThat(new String(pdf.bytes(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        assertThat(image.extension()).isEqualTo("png");
        assertThat(image.mediaType()).isEqualTo("image/png");
        assertThat(image.bytes()).startsWith(
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
        );

        assertThat(source.extension()).isEqualTo("md");
        assertThat(new String(source.bytes(), StandardCharsets.UTF_8)).isEqualTo(markdown);
    }

    @Test
    void shouldSupportFriendlyAliasesAndRejectUnknownFormats() {
        assertThat(service.export("复习资料", markdown, "word").extension()).isEqualTo("docx");
        assertThat(service.export("复习资料", markdown, "image").extension()).isEqualTo("png");
        assertThatThrownBy(() -> service.export("复习资料", markdown, "exe"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Word");
    }
}
