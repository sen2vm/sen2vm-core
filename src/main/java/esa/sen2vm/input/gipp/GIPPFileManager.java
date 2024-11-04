package esa.sen2vm.input.gipp;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.Sen2VMConstants;

import java.util.Set;

/**
 * Manager for gipp files
 *
 */
public class GIPPFileManager {

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
    public GIPPFileManager() {

    }

    /**
     * Get GIPP files of a specific type by passing a regex
     * @param folder contains the GIPP xml files
     * @param a regex that match a specific type of GIPP
     * @param list of all valid extensions
     * @return a list that contains all files that correspond to the input regex
     * @throws Sen2VMException
     */
    public static List<File> findGippFiles(File[] directories, String regexPattern, List<String> validExtensions) throws Sen2VMException {
        Pattern pattern = Pattern.compile(regexPattern);
        Map<String, Set<String>> fileMap = new HashMap<>();
        List<File> foundFiles = new ArrayList<File>();

        for (File dir: directories) {
            File[] matchedFiles = null;

            if (!dir.isDirectory()) {
                // if non-directory files
                if (pattern.matcher(dir.getName()).matches() &&
                            validExtensions.stream().anyMatch(dir.getName()::endsWith)) {
                    matchedFiles = new File[1];
                    matchedFiles[0] = dir;
                }
            }
            else {
                matchedFiles = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return pattern.matcher(name).matches() &&
                                validExtensions.stream().anyMatch(name::endsWith);
                    }
                });
            }

            if (matchedFiles != null) {
                for (File file: matchedFiles) {
                    String filepathWithoutExtension = getFilepathWithoutExtension(file);
                    System.out.println(filepathWithoutExtension);
                    String extension = getFileExtension(file);

                    // Check for duplicate file names with different extensions
                    if (fileMap.containsKey(filepathWithoutExtension)) {
                        if (fileMap.get(filepathWithoutExtension).contains(extension)) {
                            throw new Sen2VMException("Duplicate GIPP file type found: " + filepathWithoutExtension);
                        } else {
                            throw new Sen2VMException("File with same name but different extension found: " + file.getName());
                        }
                    } else {
                        Set<String> extensions = new HashSet<>();
                        extensions.add(extension);


                        fileMap.put(filepathWithoutExtension, extensions);
                    }
                }
            }
        }

        fileMap.forEach((key, value) -> {
            String filename = key + "." + value.iterator().next();
            File file = new File(filename);
            foundFiles.add(file);
        });

        return foundFiles;
    }

    /**
     * Remove the extension file from the filepath
     * @param the filepath to parse
     * @return a string that represents the absolute filepath without the extension file
     */
    private static String getFilepathWithoutExtension(File file) {
        String fileName = file.getPath();
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? fileName: fileName.substring(0, lastDotIndex);
    }

    /**
     * Return the filepath extension file
     * @param the filepath to parse
     * @return a string that represents the extension file
     */
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "": fileName.substring(lastDotIndex + 1);
    }

    /**
     * Get GIPP file of a specific type by passing a regex
     * @param folder contains the GIPP xml files
     * @param a regex that match a specific type of GIPP
     * @param list of all valid extensions
     * @return the first occurrence that correspond to the input regex
     * @throws Sen2VMException
     */
    public File findFile(File[] directories, String regexPattern, List<String> validExtensions) throws Sen2VMException {
        List<File> foundFiles = findGippFiles(directories, regexPattern, validExtensions);

        if (foundFiles != null && foundFiles.size() == 1) {
            return foundFiles.get(0);
        }
        else if (foundFiles != null && foundFiles.size() > 1) {
            throw new Sen2VMException("Duplicate GIPP file of type " + regexPattern + " find");
        }
        else {
            throw new Sen2VMException("No GIPP file of type " + regexPattern + " were find");
        }
    }

    /**
     * Constructor
     * @param folder contains the GIPP xml files
     * @throws Sen2VMException
     */
    public GIPPFileManager(String folder) throws Sen2VMException {
        LOGGER.info("Get through GIPP folder: "+ folder);
        List<String> validExtensions = Arrays.asList(Sen2VMConstants.xml_extention_small,
                                                     Sen2VMConstants.xml_extention_big,
                                                     Sen2VMConstants.dbl_extention_small,
                                                     Sen2VMConstants.dbl_extention_big);

        File gippFolder = new File(folder);
        File[] directories = gippFolder.listFiles();

        // get viewing direction file
        viewingDirectionFileList = findGippFiles(directories, Sen2VMConstants.GIPP_VIEWDIR_PAT, validExtensions);

        // get blind pixel file
        blindPixelFile = findFile(directories, Sen2VMConstants.GIPP_BLINDP_PAT, validExtensions);

        // get spacecraft model file
        spaModFile = findFile(directories, Sen2VMConstants.GIPP_SPAMOD_PAT, validExtensions);
    }

    /**
     * @return the viewingDirectionFileList
     */
    public List<File> getViewingDirectionFileList() {
        return viewingDirectionFileList;
    }

    /**
     * Set the viewing direction file path list
     * @param viewDirFilePathList
     */
    public void setViewDirFilePathList(List<String> viewDirFilePathList) {
        for (int i = 0; i < viewDirFilePathList.size(); i++) {
            String pathName = viewDirFilePathList.get(i);

            File file = new File(pathName);
            viewingDirectionFileList.set(i, file);
        }
    }

    /**
     * @return the blindPixelFile
     */
    public File getBlindPixelFile() {
        return blindPixelFile;
    }

    /**
     * @param blindPixelFile the blindPixelFile to set
     */
    public void setBlindPixelFile(File blindPixelFile) {
        this.blindPixelFile = blindPixelFile;
    }

    /**
     * @return the spaModFile
     */
    public File getSpaModFile() {
        return spaModFile;
    }

    /**
     * @param spaModFile the spaModFile to set
     */
    public void setSpaModFile(File spaModFile) {
        this.spaModFile = spaModFile;
    }
}
