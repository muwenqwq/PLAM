package com.edustudio.module.learningspace.support;

import com.edustudio.module.knowledge.entity.KnowledgeFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeStorageCleanerTest {

    @TempDir
    Path storageRoot;

    @Test
    void deletesStoredFileAndEmptySpaceDirectories() throws Exception {
        Path storedFile = storageRoot.resolve("knowledge/3/42/20260717/notes.txt");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "test content");

        KnowledgeFile file = new KnowledgeFile();
        file.setStoragePath("knowledge/3/42/20260717/notes.txt");
        KnowledgeStorageCleaner cleaner = new KnowledgeStorageCleaner();
        ReflectionTestUtils.setField(cleaner, "storageDir", storageRoot.toString());

        cleaner.deleteStoredFiles(List.of(file));

        assertThat(storedFile).doesNotExist();
        assertThat(storageRoot.resolve("knowledge/3/42")).doesNotExist();
    }

    @Test
    void ignoresPathsOutsideStorageRoot() throws Exception {
        Path outside = storageRoot.getParent().resolve("outside.txt");
        Files.writeString(outside, "keep");
        try {
            KnowledgeFile file = new KnowledgeFile();
            file.setStoragePath("../../outside.txt");
            KnowledgeStorageCleaner cleaner = new KnowledgeStorageCleaner();
            ReflectionTestUtils.setField(cleaner, "storageDir", storageRoot.toString());

            cleaner.deleteStoredFiles(List.of(file));

            assertThat(outside).exists();
        } finally {
            Files.deleteIfExists(outside);
        }
    }
}
