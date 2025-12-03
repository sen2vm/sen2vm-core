package esa.sen2vm.utils;

import java.io.File;
import java.util.logging.Logger;

import esa.sen2vm.exception.Sen2VMException;

public class PathUtils
{
    private static final Logger LOGGER = Logger.getLogger(PathUtils.class.getName());

    /**
     * Check that the path exist
     * @param path the path we want to check if it does exist
     * @return the path
     * @throws Sen2VMException
     */
    public static String checkPath(String path) throws Sen2VMException
    {
        File file = new File(path);
        if (!file.exists())
        {
            throw new Sen2VMException("Path " + file + " does not exist");
        }
        return path;
     }

    /**
     * Get the datastrip files path
     * @param l1bProduct the LIB product
     * @return the datastrio file path
     * @throws Sen2VMException
     */
    public static String getDatastripFilePath(String l1bProduct) throws Sen2VMException
    {
        File datastripFolder = new File(l1bProduct + "/" + Sen2VMConstants.DATASTRIP_MAIN_FOLDER);
        if (!datastripFolder.exists())
        {
            throw new Sen2VMException("Datastrip folder " + datastripFolder + " does not exist");
        }

        File[] directories = datastripFolder.listFiles();
        String datastripFilePath = null;
        for (File dir: directories)
        {
            if (!dir.isDirectory())
            {
                continue;
            }
            String filename = dir.getName().replaceAll("_N.*", "").replace(Sen2VMConstants.DATASTRIP_MSI_TAG, Sen2VMConstants.DATASTRIP_METADATA_TAG);
            datastripFilePath = dir + "/" + filename + Sen2VMConstants.xml_extention_small;
        }
        if(datastripFilePath==null)
        {
            throw new Sen2VMException("No datastrip metadata file found inside folder: " + datastripFolder);
        }
        File datastripFile = new File(datastripFilePath);
        if (datastripFile.exists())
        {
            LOGGER.info("Find the following datastrip metadata file: " + datastripFilePath);
            return datastripFilePath;
        }
        else
        {
            throw new Sen2VMException("No datastrip metadata file found inside folder: " + datastripFolder);
        }
    }
}