package esa.sen2vm.input.gipp;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.file.*;

public class UntarGIPP {
    
    /**
     * Extracts a .tar file into targetDir.
     * Existing files are replaced.
     */
    public static void untar(Path tarFile, Path targetDir) throws IOException {
        // Ensure target directory exists
        Files.createDirectories(targetDir);

        try (InputStream fis = Files.newInputStream(tarFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             TarArchiveInputStream tis = new TarArchiveInputStream(bis)) {

            extractAllEntries(tis, targetDir);
        }
    }

    /**
     * Extracts a .tar.gz / .tgz file into targetDir.
     * Existing files are replaced.
     */
    public static void untarGz(Path tarGzFile, Path targetDir) throws IOException {
        // Ensure target directory exists
        Files.createDirectories(targetDir);

        try (InputStream fis = Files.newInputStream(tarGzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {

            extractAllEntries(tis, targetDir);
        }
    }

    /**
     * Common extraction routine that:
     * - Creates directories if needed
     * - Replaces existing files
     * - Prevents Zip Slip (path traversal)
     */
    private static void extractAllEntries(TarArchiveInputStream tis, Path targetDir) throws IOException {
        ArchiveEntry entry;
        byte[] buffer = new byte[8192];

        while ((entry = tis.getNextEntry()) != null) {
            // Normalize entry path and prevent writing outside targetDir (Zip Slip protection)
            Path entryPath = targetDir.resolve(entry.getName()).normalize();
            if (!entryPath.startsWith(targetDir)) {
                throw new IOException("Blocked suspicious entry (Zip Slip): " + entry.getName());
            }

            if (entry.isDirectory()) {
                // Create directory for this entry
                Files.createDirectories(entryPath);
                continue;
            }

            // Ensure parent directory exists
            Path parent = entryPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Overwrite the file if it already exists
            try (OutputStream os = Files.newOutputStream(entryPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                int len;
                while ((len = tis.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }

        }
    }

}