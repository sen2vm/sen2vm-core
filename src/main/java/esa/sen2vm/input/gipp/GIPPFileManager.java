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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.Sen2VMConstants;

/**
 * Manager for GIPP files
 */
public class GIPPFileManager
{

    /*
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(GIPPFileManager.class.getName());

    /**
     * List of GIPP Viewing Direction XML files
     */
    protected List<File> viewingDirectionFileList = new ArrayList<>();

    /**
     * GIPP Blind pixel XML files
     */
    protected File blindPixelFile = null;

    /**
     * GIPP spa mod XML files
     */
    protected File spaModFile = null;

    /**
     * Default constructor
     */
    public GIPPFileManager()
    {

    }

    /**
     * Get GIPP files of a specific type by passing a regex
     * @param root contains the GIPP xml files
     * @param dirNameRegex regex that match a specific folder of GIPP
     * @param fileNameRegex regex that match a specific type of GIPP
     * @param list of all valid extensions
     * @return a list that contains all GIPP files that correspond to the input regex
     * @throws Sen2VMException
     */
    public static List<File> findGippFiles(Path root, String dirNameRegex, List<String> gippList, String fileNameRegex, List<String> validExtensions) throws IOException {
        if(gippList.isEmpty())
        {
            return searchGIPFilesFromRegex(root, dirNameRegex, fileNameRegex, validExtensions);
        }else{
            return searchGIPPFilesFromList(root, dirNameRegex, gippList, validExtensions);
        }
    }

    public static List<File> searchGIPFilesFromRegex(Path root, String dirNameRegex, String fileNameRegex, List<String> validExtensions) throws IOException {
                final Pattern dirPattern = Pattern.compile(dirNameRegex);
        final Pattern filePattern = Pattern.compile(fileNameRegex);
        final List<File> results = new ArrayList<>();
        // Stack indicating whether we are currently in a qualified subtree
        final Deque<Boolean> qualifiedStack = new ArrayDeque<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Checks that the parent is already in a qualified subtree
                boolean parentQualified = !qualifiedStack.isEmpty() && qualifiedStack.peek();

                // Does this folder match the folder regex?
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : dir.toString();
                boolean thisDirMatches = dirPattern.matcher(dirName).matches();
                
                // If the parent is qualified or this folder matches, then this subtree is qualified
                boolean qualified = parentQualified || thisDirMatches;

                qualifiedStack.push(qualified);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                boolean inQualifiedSubtree = !qualifiedStack.isEmpty() && qualifiedStack.peek();
                String fileName = filePath.getFileName() != null ? filePath.getFileName().toString() : filePath.toString();
                if (inQualifiedSubtree || filePattern.matcher(fileName).matches()) {
                    File file = filePath.toFile();
                    String extension = getFileExtension(file);
                    if(validExtensions.stream().anyMatch(item -> item.contains(extension)))
                    {
                        results.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                // Exiting the directory: we remove the qualification state
                qualifiedStack.pop();
                return FileVisitResult.CONTINUE;
            }
        });
        return results;
    }

    public static List<File> searchGIPPFilesFromList(Path root, String dirNameRegex, List<String> gippList, List<String> validExtensions) throws IOException {
        final List<File> results = new ArrayList<>();
        final List<String> fileNames = new ArrayList<>();
        final List<String> fileNamesWithoutExtension = new ArrayList<>();
        final List<String> tarExtension = Arrays.asList("TGZ", "tar.gz","tgz");
        // first loop to find the GIPP from untar files and the GIPP list
        for(String gippName:gippList)
        {
            final Pattern filePattern = Pattern.compile(gippName);
            // Stack indicating whether we are currently in a qualified subtree
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                    String fileName = filePath.getFileName() != null ? filePath.getFileName().toString() : filePath.toString();
                    String fileNameWithoutExtension = fileName;
                    if(!Files.isDirectory(filePath.toAbsolutePath())){
                        fileNameWithoutExtension = getFilePathWithoutExtension(new File(fileName));
                    }
                    if (filePattern.matcher(fileNameWithoutExtension).matches())
                    {
                        File file = filePath.toFile();
                        String extension = getFileExtension(file);
                        if(validExtensions.stream().anyMatch(item -> item.contains(extension)))
                        {
                            results.add(file);
                            fileNames.add(file.getName());
                            fileNamesWithoutExtension.add(fileNameWithoutExtension);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        // second loop to find the GIPP unfound from tar files and the GIPP list
        if(results.size()!=gippList.size())
        {
            for(String gippName:gippList)
            {
                final Pattern filePattern = Pattern.compile(gippName);
                // Stack indicating whether we are currently in a qualified subtree
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                        // boolean inQualifiedSubtree = !qualifiedStack.isEmpty() && qualifiedStack.peek();
                        String fileName = filePath.getFileName() != null ? filePath.getFileName().toString() : filePath.toString();
                        String fileNameWithoutExtension = fileName;
                        if(!Files.isDirectory(filePath.toAbsolutePath())){
                            fileNameWithoutExtension = getFilePathWithoutExtension(new File(fileName));
                        }

                        if (filePattern.matcher(fileNameWithoutExtension).matches() && !fileNamesWithoutExtension.contains(fileNameWithoutExtension))
                        {
                            File file = filePath.toFile();
                            String extension = getFileExtension(file);
                            if(tarExtension.stream().anyMatch(item -> item.contains(extension)))
                            {
                                try{
                                    List<Path> listPath = UntarGIPP.untarGz(file.toPath(), Paths.get(file.getParent()));
                                    for(Path untarPath:listPath){
                                        File untarFile = untarPath.toFile();
                                        String untarFileExtension = getFileExtension(untarFile);
                                        if(validExtensions.stream().anyMatch(item -> item.contains(untarFileExtension)))
                                        {
                                            if(!results.contains(untarFile) || !fileNames.contains(untarFile.getName())){
                                                results.add(untarFile);
                                                fileNames.add(untarFile.getName());
                                            }
                                        }
                                    }
                                    LOGGER.info("Untar GIPP: "+file.toString());
                                }catch(IOException e)
                                {
                                    LOGGER.warning("The targz extraction of GIPP has failed:"+file.toString());
                                    e.printStackTrace();
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        return results;
    }

    public static List<String> typedGIPPList(List<String> gippList, String gippType)
    {
        Pattern pattern = Pattern.compile(".*"+gippType+".*");
        List<String> filteredGIPList = new ArrayList<>();
        for (String item : gippList) {
            Matcher matcher = pattern.matcher(item);
            if (matcher.matches()) {
                filteredGIPList.add(item);
            }
        }
        return filteredGIPList;
    }

     /**
     * Get GIPP files of a specific type by passing a regex
     * @param root contains the GIPP xml files
     * @param dirNameRegex regex that match a specific folder of GIPP
     * @param fileNameRegex regex that match a specific type of GIPP
     * @param list of all valid extensions
     * @return a GIPP file that correspond to the input regex
     * @throws Sen2VMException
     */
    public static File findGippFile(Path root, String dirNameRegex, List<String> gippList, String fileNameRegex, List<String> validExtensions) throws IOException, Sen2VMException {
        final List<File> results = findGippFiles(root, dirNameRegex, gippList, fileNameRegex, validExtensions);
        if(results.size()==0)
        {
            throw new Sen2VMException("The directory must be contains keyword:"+fileNameRegex); 
        }
        else if(results.size()>1)
        {
            String message = results.stream()
                .filter(f -> f != null)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(", "));

            LOGGER.info("GIPP "+dirNameRegex+" : "+message);
            throw new Sen2VMException("Duplicate GIPP file type found"); 
        }
        else
        {
            return results.get(0);
        }
    }

    /**
     * Untar and remove all the GIPP files TGZ in folder
     * @param parentFolder the GIPP folder
     * @throws IOException
     */
    public static void untarAllGIPP(Path parentFolder) throws IOException
    {

        Files.walkFileTree(parentFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".TGZ") || name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                    UntarGIPP.untarGz(file, file.getParent());
                    Files.deleteIfExists(file);
                    LOGGER.info("Untar GIPP: "+file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Return the filepath extension file
     * @param the filepath to parse
     * @return a string that represents the extension file
     */
    private static String getFileExtension(File file)
    {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if(fileName.contains(".tar.gz"))
        {
            lastDotIndex = fileName.lastIndexOf(".tar");
        }
        return (lastDotIndex == -1) ? "": fileName.substring(lastDotIndex + 1);
    }

    /**
     * Remove the extension file from the filepath
     * @param the filepath to parse
     * @return a string that represents the absolute filepath without the extension file
     */
    private static String getFilePathWithoutExtension(File file)
    {
        String fileName = file.getPath();
        int lastDotIndex = fileName.lastIndexOf('.');
        if(fileName.contains(".tar.gz"))
        {
            lastDotIndex = fileName.lastIndexOf(".tar");
        }
        return (lastDotIndex == -1) ? fileName: fileName.substring(0, lastDotIndex);
    }

    /**
     * Constructor
     * @param folder contains the GIPP xml files
     * @throws Sen2VMException
     */
    public GIPPFileManager(String folder, List<String> gippList) throws Sen2VMException
    {
        LOGGER.info("Get through GIPP folder: "+ folder);
        List<String> validExtensions = Arrays.asList(Sen2VMConstants.xml_extention_small,
                                                     Sen2VMConstants.xml_extention_big,
                                                     Sen2VMConstants.dbl_extention_small,
                                                     Sen2VMConstants.dbl_extention_big);

        Path gippFolder = new File(folder).toPath();
        String blindPixelGIPType = "GIP_BLINDP";
        String spamodGIPType = "GIP_SPAMOD";
        String viewingDirGIPType = "GIP_VIEDIR";
        List<String> blindPixelGIPList = new ArrayList();
        List<String> spamodGIPList = new ArrayList();
        List<String> viewingDirGIPList = new ArrayList();
        if(!gippList.isEmpty())
        {
            // get blind pixel file
            blindPixelGIPList =typedGIPPList(gippList, blindPixelGIPType);

            // get spacecraft model file
            spamodGIPList =typedGIPPList(gippList, spamodGIPType);

            // get viewing direction file
            viewingDirGIPList =typedGIPPList(gippList, viewingDirGIPType);
        }
        try{
            // get blind pixel file
            blindPixelFile = findGippFile(gippFolder, blindPixelGIPType, blindPixelGIPList, Sen2VMConstants.GIPP_BLINDP_PAT, validExtensions);
            
            // get spacecraft model file
            spaModFile = findGippFile(gippFolder, spamodGIPType, spamodGIPList, Sen2VMConstants.GIPP_SPAMOD_PAT, validExtensions);
            
            // get viewing direction file
            viewingDirectionFileList = findGippFiles(gippFolder, viewingDirGIPType, viewingDirGIPList, Sen2VMConstants.GIPP_VIEWDIR_PAT, validExtensions);
        }catch(IOException e)
        {
            throw new Sen2VMException(e);
        }
    }

    /**
     * @return the viewingDirectionFileList
     */
    public List<File> getViewingDirectionFileList()
    {
        return viewingDirectionFileList;
    }

    /**
     * Set the viewing direction file path list
     * @param viewDirFilePathList
     */
    public void setViewDirFilePathList(List<String> viewDirFilePathList)
    {
        for (int i = 0; i < viewDirFilePathList.size(); i++)
        {
            String pathName = viewDirFilePathList.get(i);

            File file = new File(pathName);
            viewingDirectionFileList.set(i, file);
        }
    }

    /**
     * @return the blindPixelFile
     */
    public File getBlindPixelFile()
    {
        return blindPixelFile;
    }

    /**
     * @param blindPixelFile the blindPixelFile to set
     */
    public void setBlindPixelFile(File blindPixelFile)
    {
        this.blindPixelFile = blindPixelFile;
    }

    /**
     * @return the spaModFile
     */
    public File getSpaModFile()
    {
        return spaModFile;
    }

    /**
     * @param spaModFile the spaModFile to set
     */
    public void setSpaModFile(File spaModFile)
    {
        this.spaModFile = spaModFile;
    }
}