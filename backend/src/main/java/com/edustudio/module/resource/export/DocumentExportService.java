package com.edustudio.module.resource.export;

import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentExportService {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*(?:[-*+]\\s+|\\d+[.)]\\s+)(.*)$");
    private static final int IMAGE_WIDTH = 1400;
    private static final int IMAGE_MARGIN = 90;
    private static final int IMAGE_MAX_HEIGHT = 30000;

    public ExportedDocument export(String title, String markdown, String requestedFormat) {
        String format = normalizeFormat(requestedFormat);
        String content = markdown == null ? "" : markdown;
        try {
            return switch (format) {
                case "md" -> new ExportedDocument(content.getBytes(StandardCharsets.UTF_8),
                        "text/markdown;charset=UTF-8", "md");
                case "docx" -> new ExportedDocument(toDocx(title, content),
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
                case "pdf" -> new ExportedDocument(toPdf(title, content), "application/pdf", "pdf");
                case "png" -> new ExportedDocument(toPng(title, content), "image/png", "png");
                default -> throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的导出格式");
            };
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文档导出失败: " + exception.getMessage());
        }
    }

    private String normalizeFormat(String requestedFormat) {
        String value = requestedFormat == null ? "docx" : requestedFormat.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "word" -> "docx";
            case "image", "long-image" -> "png";
            case "markdown" -> "md";
            case "md", "docx", "pdf", "png" -> value;
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "导出格式只能是 Word、PDF、PNG 或 Markdown");
        };
    }

    private byte[] toDocx(String title, String markdown) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(safeTitle(title));
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            setChineseFont(titleRun, "Microsoft YaHei");

            boolean codeBlock = false;
            for (String rawLine : normalizedLines(markdown)) {
                if (rawLine.trim().startsWith("```")) {
                    codeBlock = !codeBlock;
                    continue;
                }
                XWPFParagraph paragraph = document.createParagraph();
                String text = rawLine;
                Matcher heading = HEADING.matcher(rawLine);
                Matcher listItem = LIST_ITEM.matcher(rawLine);
                int fontSize = 11;
                boolean bold = false;
                if (heading.matches()) {
                    text = heading.group(2);
                    fontSize = Math.max(12, 18 - heading.group(1).length());
                    bold = true;
                } else if (listItem.matches()) {
                    text = "• " + listItem.group(1);
                    paragraph.setIndentationLeft(360);
                }
                XWPFRun run = paragraph.createRun();
                run.setText(cleanInlineMarkdown(text));
                run.setBold(bold);
                run.setFontSize(fontSize);
                setChineseFont(run, codeBlock ? "Consolas" : "Microsoft YaHei");
            }
            document.write(output);
            return output.toByteArray();
        }
    }

    private void setChineseFont(XWPFRun run, String family) {
        run.setFontFamily(family);
        run.setFontFamily(family, XWPFRun.FontCharRange.eastAsia);
    }

    private byte[] toPdf(String title, String markdown) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = PDType0Font.load(document, locatePdfFont());
            List<PdfLine> lines = pdfLines(title, markdown, font, PDRectangle.A4.getWidth() - 108);
            PDPage page = null;
            PDPageContentStream stream = null;
            float y = 0;
            try {
                for (PdfLine line : lines) {
                    if (page == null || y - line.leading() < 54) {
                        if (stream != null) stream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        y = page.getMediaBox().getHeight() - 54;
                    }
                    stream.beginText();
                    stream.setFont(font, line.fontSize());
                    stream.newLineAtOffset(54, y);
                    stream.showText(line.text());
                    stream.endText();
                    y -= line.leading();
                }
            } finally {
                if (stream != null) stream.close();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private List<PdfLine> pdfLines(String title, String markdown, PDType0Font font, float maxWidth) throws IOException {
        List<PdfLine> result = new ArrayList<>();
        result.addAll(wrapPdfLine(sanitizeForPdf(font, safeTitle(title)), font, 18, maxWidth, 28));
        result.add(new PdfLine("", 11, 10));
        boolean codeBlock = false;
        for (String raw : normalizedLines(markdown)) {
            if (raw.trim().startsWith("```")) {
                codeBlock = !codeBlock;
                continue;
            }
            Matcher heading = HEADING.matcher(raw);
            Matcher listItem = LIST_ITEM.matcher(raw);
            String text = raw;
            float size = codeBlock ? 9 : 11;
            float leading = codeBlock ? 14 : 18;
            if (heading.matches()) {
                text = heading.group(2);
                size = Math.max(12, 17 - heading.group(1).length());
                leading = size + 9;
            } else if (listItem.matches()) {
                text = "• " + listItem.group(1);
            }
            text = sanitizeForPdf(font, cleanInlineMarkdown(text));
            result.addAll(wrapPdfLine(text, font, size, maxWidth, leading));
        }
        return result;
    }

    private List<PdfLine> wrapPdfLine(String text, PDType0Font font, float size, float maxWidth, float leading) throws IOException {
        if (text.isBlank()) return List.of(new PdfLine("", size, leading));
        List<PdfLine> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String token = new String(Character.toChars(codePoint));
            String candidate = current + token;
            if (!current.isEmpty() && font.getStringWidth(candidate) / 1000f * size > maxWidth) {
                lines.add(new PdfLine(current.toString(), size, leading));
                current.setLength(0);
            }
            current.append(token);
            offset += Character.charCount(codePoint);
        }
        if (!current.isEmpty()) lines.add(new PdfLine(current.toString(), size, leading));
        return lines;
    }

    private String sanitizeForPdf(PDType0Font font, String text) {
        StringBuilder result = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            String token = new String(Character.toChars(codePoint));
            try {
                font.getStringWidth(token);
                result.append(token);
            } catch (IllegalArgumentException | IOException ignored) {
                result.append('?');
            }
        });
        return result.toString();
    }

    private File locatePdfFont() {
        List<Path> candidates = List.of(
                Path.of("C:/Windows/Fonts/simhei.ttf"),
                Path.of("C:/Windows/Fonts/simsunb.ttf"),
                Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttf"),
                Path.of("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"),
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
        );
        return candidates.stream().filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_ERROR, "系统未找到可用于 PDF 的中文字体"))
                .toFile();
    }

    private byte[] toPng(String title, String markdown) throws IOException {
        Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 42);
        Font bodyFont = new Font("Microsoft YaHei", Font.PLAIN, 28);
        BufferedImage probe = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D probeGraphics = probe.createGraphics();
        probeGraphics.setFont(bodyFont);
        List<String> lines = wrapImageText(toPlainText(markdown), probeGraphics.getFontMetrics(),
                IMAGE_WIDTH - IMAGE_MARGIN * 2);
        probeGraphics.dispose();
        int lineHeight = 46;
        int requestedHeight = 190 + Math.max(1, lines.size()) * lineHeight;
        int height = Math.min(IMAGE_MAX_HEIGHT, requestedHeight);
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, IMAGE_WIDTH, height);
        graphics.setColor(new Color(31, 41, 55));
        graphics.setFont(titleFont);
        graphics.drawString(safeTitle(title), IMAGE_MARGIN, 90);
        graphics.setColor(new Color(226, 232, 240));
        graphics.drawLine(IMAGE_MARGIN, 120, IMAGE_WIDTH - IMAGE_MARGIN, 120);
        graphics.setColor(new Color(55, 65, 81));
        graphics.setFont(bodyFont);
        int y = 170;
        for (String line : lines) {
            if (y > height - 55) break;
            graphics.drawString(line, IMAGE_MARGIN, y);
            y += lineHeight;
        }
        if (requestedHeight > IMAGE_MAX_HEIGHT) {
            graphics.setColor(new Color(107, 114, 128));
            graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 22));
            graphics.drawString("内容较长，完整版本请使用 Word 或 PDF 导出。", IMAGE_MARGIN, height - 24);
        }
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private List<String> wrapImageText(String text, FontMetrics metrics, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String paragraph : normalizedLines(text)) {
            if (paragraph.isBlank()) {
                result.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int offset = 0; offset < paragraph.length();) {
                int codePoint = paragraph.codePointAt(offset);
                String token = new String(Character.toChars(codePoint));
                if (!current.isEmpty() && metrics.stringWidth(current + token) > maxWidth) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                current.append(token);
                offset += Character.charCount(codePoint);
            }
            if (!current.isEmpty()) result.add(current.toString());
        }
        return result;
    }

    private String toPlainText(String markdown) {
        StringBuilder result = new StringBuilder();
        boolean codeBlock = false;
        for (String raw : normalizedLines(markdown)) {
            if (raw.trim().startsWith("```")) {
                codeBlock = !codeBlock;
                continue;
            }
            Matcher heading = HEADING.matcher(raw);
            Matcher listItem = LIST_ITEM.matcher(raw);
            String text = heading.matches() ? heading.group(2) : (listItem.matches() ? "• " + listItem.group(1) : raw);
            result.append(cleanInlineMarkdown(text)).append('\n');
        }
        return result.toString().trim();
    }

    private String cleanInlineMarkdown(String text) {
        return text
                .replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1")
                .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                .replaceAll("[*_~`]", "")
                .replace("|", "  ")
                .trim();
    }

    private List<String> normalizedLines(String text) {
        return List.of((text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));
    }

    private String safeTitle(String title) {
        return title == null || title.isBlank() ? "学习资源" : title.trim();
    }

    private record PdfLine(String text, float fontSize, float leading) {
    }
}
