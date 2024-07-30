package esa.sen2vm;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Get GIPP file of a specific type by passing a regex
     * @param folders that may
     * @param a regex that match a specific type of GIPP
     * @return an array that contains all files that correspond to the input regex
     */
    public List<File> findFiles(File[] directories, String regexMatch) {
        List<File> foundFiles = new ArrayList<>();
        Pattern pattern = Pattern.compile(regexMatch);

        if (directories != null) {
            for (File dir : directories) {
                if (dir.isDirectory()) {
                    File[] files = dir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            Matcher matcher = pattern.matcher(name);
                            return matcher.matches();
                        }
                    });

                    if (files != null) {
                        for (File file : files) {
                            foundFiles.add(file);
                        }
                    }
                }
            }
        }

        if (foundFiles != null && foundFiles.size() > 0) {
            return foundFiles;
        }
        else {
            return null;
        }
    }

    /**
     * Get GIPP file of a specific type by passing a regex
     * @param folder contains the GIPP xml files
     * @param a regex that match a specific type of GIPP
     * @return the first occurrence that correspond to the input regex
     */
    public File findFile(File[] directories, String regexMatch) {
        List<File> foundFiles = findFiles(directories, regexMatch);

        if (foundFiles != null && foundFiles.size() > 0) {
            return foundFiles.get(0);
        }
        else {
            return null;
        }
    }

    /**
     * Constructor
     * @param folder contains the GIPP xml files
     */
    public GIPPFileManager(String folder) {
        File gippFolder = new File(folder);

        if (!gippFolder.exists() || !gippFolder.isDirectory()) {
            LOGGER.severe("The gipp folder " + gippFolder + " doesn't exist or is not a directory");
        }

        File[] directories = gippFolder.listFiles();

        // get viewing direction files
        viewingDirectionFileList = findFiles(directories, ".*GIP_VIEDIR.*\\.DBL");

        // get blind pixel file
        blindPixelFile = findFile(directories, ".*GIP_BLINDP_MPC.*\\.DBL");

        // get spa mod file
        spaModFile = findFile(directories, ".*GIP_SPAMOD.*\\.DBL");
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
