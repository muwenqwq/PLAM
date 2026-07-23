package com.edustudio.module.learningspace.support;

import com.edustudio.module.knowledge.entity.KnowledgeFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class KnowledgeStorageCleaner {

    @Value("${eduagent.knowledge.storage-dir:${user.dir}/data/knowledge}")
    private String storageDir;

    public void deleteStoredFiles(List<KnowledgeFile> files) {
        for (KnowledgeFile file : files) {
            deleteStoredFile(file.getStoragePath());
        }
    }

    public void deleteStoredFile(KnowledgeFile file) {
        if (file != null) {
            deleteStoredFile(file.getStoragePath());
        }
    }

    private void deleteStoredFile(String storagePath) {
        if (!StringUtils.hasText(storagePath) || storagePath.startsWith("generated-resource://")) {
            return;
        }
        Path root = Paths.get(storageDir).toAbsolutePath().normalize();
        Path target = root.resolve(storagePath).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
            deleteEmptyParents(target.getParent(), root);
        } catch (IOException ignored) {
            // Database cleanup remains authoritative; inaccessible files can be removed later.
        }
    }

    private void deleteEmptyParents(Path directory, Path root) {
        Path current = directory;
        while (current != null && current.startsWith(root) && !current.equals(root)) {
            try {
                Files.delete(current);
            } catch (IOException exception) {
                return;
            }
            current = current.getParent();
        }
    }
}
