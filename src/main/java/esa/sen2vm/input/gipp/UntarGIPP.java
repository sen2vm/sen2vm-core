/** Copyright 2024-2025, CS GROUP, https://www.cs-soprasteria.com/
*
* This file is part of the Sen2VM Core project
*     https://gitlab.acri-cwa.fr/opt-mpc/s2_tools/sen2vm/sen2vm-core
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.*/

package esa.sen2vm.input.gipp;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.io.*;
import java.nio.file.*;

public class UntarGIPP
{
    private static final Logger LOGGER = Logger.getLogger(GIPPFileManager.class.getName());
    /**
     * Extracts a .tar file into targetDir.
     * Existing files are replaced.
     */
    public static void untar(Path tarFile, Path targetDir) throws IOException
    {
        // Ensure target directory exists
        Files.createDirectories(targetDir);

        try (InputStream fis = Files.newInputStream(tarFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             TarArchiveInputStream tis = new TarArchiveInputStream(bis))
        {
            extractAllEntries(tis, targetDir);
        }
    }

    /**
     * Extracts a .tar.gz / .tgz file into targetDir.
     * Existing files are replaced.
     */
    public static List<Path> untarGz(Path tarGzFile, Path targetDir) throws IOException
    {
        // Ensure target directory exists
        Files.createDirectories(targetDir);
        List<Path> listPaths = new ArrayList<>();
        try (InputStream fis = Files.newInputStream(tarGzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gis))
        {

            listPaths = extractAllEntries(tis, targetDir);
        }
        return listPaths;
    }

    /**
     * Common extraction routine that:
     * - Creates directories if needed
     * - Replaces existing files
     * - Prevents Zip Slip (path traversal)
     */
    private static List<Path> extractAllEntries(TarArchiveInputStream tis, Path targetDir) throws IOException
    {
        ArchiveEntry entry;
        byte[] buffer = new byte[8192];
        List<Path> listPaths = new ArrayList<>();
        while ((entry = tis.getNextEntry()) != null)
        {
            // Normalize entry path and prevent writing outside targetDir (Zip Slip protection)
            Path entryPath = targetDir.resolve(entry.getName()).normalize();
            if (!entryPath.startsWith(targetDir))
            {
                throw new IOException("Blocked suspicious entry (Zip Slip): " + entry.getName());
            }

            if (entry.isDirectory())
            {
                // Create directory for this entry
                Files.createDirectories(entryPath);
                continue;
            }

            // Ensure parent directory exists
            Path parent = entryPath.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }

            // Overwrite the file if it already exists
            try (OutputStream os = Files.newOutputStream(entryPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE))
            {
                int len;
                while ((len = tis.read(buffer)) != -1)
                {
                    os.write(buffer, 0, len);
                }
                listPaths.add(entryPath);
            }

        }
        return listPaths;
    }

}