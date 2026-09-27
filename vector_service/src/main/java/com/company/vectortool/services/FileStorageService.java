package com.company.vectortool.services;

import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootFolder = Paths.get("./file_vault");

    public FileStorageService() {
        try {
            Files.createDirectories(rootFolder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize local File Storage vault directory structure", e);
        }
    }

    public String commitToStorage(UUID documentId, String filename, byte[] fileBytes) {
        try {
            String sanitizedExt = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : ".dat";
            String storagePhysicalName = documentId.toString() + sanitizedExt;
            Path fileDestinationPath = this.rootFolder.resolve(storagePhysicalName);
            Files.write(fileDestinationPath, fileBytes);
            return fileDestinationPath.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed writing bytes to local file storage vault system: " + e.getMessage(), e);
        }
    }
}
