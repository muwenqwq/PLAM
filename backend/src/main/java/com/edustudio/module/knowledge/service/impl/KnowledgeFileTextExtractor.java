package com.edustudio.module.knowledge.service.impl;

import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class KnowledgeFileTextExtractor {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "tsv", "json", "xml", "html", "htm",
            "java", "py", "js", "ts", "vue", "css", "sql", "yaml", "yml", "properties", "log"
    );

    public String extract(String originalName, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        String extension = extensionOf(originalName);
        if ("docx".equals(extension)) {
            return normalize(extractOfficeText(bytes, name -> "word/document.xml".equals(name)));
        }
        if ("pptx".equals(extension)) {
            return normalize(extractOfficeText(bytes, name -> name.startsWith("ppt/slides/slide") && name.endsWith(".xml")));
        }
        if (looksLikeZip(bytes)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Office file must be uploaded as docx or pptx, not decoded as plain text");
        }
        if (isText(extension, contentType)) {
            return normalize(decodeText(bytes));
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "暂不支持该文件类型自动解析，请上传 txt、md、csv、json、docx 或 pptx 文件");
    }

    private boolean isText(String extension, String contentType) {
        if (TEXT_EXTENSIONS.contains(extension)) {
            return true;
        }
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String lower = contentType.toLowerCase();
        return lower.startsWith("text/")
                || lower.contains("json")
                || lower.contains("xml")
                || lower.contains("csv")
                || lower.contains("yaml");
    }

    private String decodeText(byte[] bytes) {
        byte[] content = bytes;
        if (startsWith(bytes, new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF})) {
            content = java.util.Arrays.copyOfRange(bytes, 3, bytes.length);
        }
        try {
            return strictDecode(StandardCharsets.UTF_8, content);
        } catch (CharacterCodingException ignored) {
            return Charset.forName("GB18030").decode(ByteBuffer.wrap(content)).toString();
        }
    }

    private String strictDecode(Charset charset, byte[] bytes) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private String extractOfficeText(byte[] bytes, Predicate<String> entryMatcher) {
        Map<String, byte[]> entries = new TreeMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory() && entryMatcher.test(name)) {
                    entries.put(name, zipInputStream.readAllBytes());
                }
            }
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件解析失败，请确认文件未损坏");
        }
        if (entries.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件中没有可解析的正文内容");
        }
        StringBuilder builder = new StringBuilder();
        entries.values().forEach(entryBytes -> builder.append(extractXmlText(entryBytes)).append("\n"));
        return builder.toString();
    }

    private String extractXmlText(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(decodeText(xmlBytes))));
            StringBuilder builder = new StringBuilder();
            appendText(document, builder);
            return builder.toString();
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Office 文件正文解析失败");
        }
    }

    private void appendText(Node node, StringBuilder builder) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String text = node.getTextContent();
            if (StringUtils.hasText(text)) {
                builder.append(text.trim()).append(' ');
            }
            return;
        }
        Node child = node.getFirstChild();
        while (child != null) {
            appendText(child, builder);
            child = child.getNextSibling();
        }
    }

    private String normalize(String text) {
        String normalized = text == null ? "" : text
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件中没有可用于生成知识片段的文本");
        }
        return normalized;
    }

    private String extensionOf(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeZip(byte[] bytes) {
        return startsWith(bytes, new byte[]{0x50, 0x4B, 0x03, 0x04})
                || startsWith(bytes, new byte[]{0x50, 0x4B, 0x05, 0x06})
                || startsWith(bytes, new byte[]{0x50, 0x4B, 0x07, 0x08});
    }
}
