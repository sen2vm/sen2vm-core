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