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

package esa.sen2vm.input.DEM;

import org.hipparchus.util.FastMath;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.logging.Logger;

import org.gdal.gdal.gdal;
import org.gdal.gdal.Dataset;
import org.gdal.gdalconst.gdalconstConstants;

import org.sxgeo.input.dem.SrtmFileManager;

// import com.sun.tools.javac.util.List;

import org.sxgeo.exception.SXGeoException;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.DEM.DemTile;

//Extend SrtmFileManager and not DemManager, as an check on the type of instanciation is made if isInstance of SrtmFileManager

/**
 * Read the DEM files
 */
public class GenericDemFileManager extends SrtmFileManager
{
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(GenericDemFileManager.class.getName());

    // Map dem filepath with a string that represents longitude/latitude
    // Example: with a SRTM tile on Madeira island located at longitude -16 and latitude 30
    // the correponding map entry will be ("-16/30"="/DEMDIR/DEM_SRTM/w016/n30.dt1")
    // Key corresponding to "longitude/latitude" and value corresponding to the dem filepath.
    Map<Long, List<DemTile>> demGridMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    public GenericDemFileManager(String demRootDir) throws Sen2VMException
    {
        super(demRootDir);
        buildMap(demRootDir);
    }


    private long mapKey(int x, int y)
    {
        return (((long) x) << 32) | (y & 0xffffffffL);
    }

    private void addDemTile(DemTile d)
    {
        int xMin = (int)FastMath.floor(d.minX);
        int xMax = (int)FastMath.floor(d.maxX);
        int yMin = (int)FastMath.floor(d.minY);
        int yMax = (int)FastMath.floor(d.maxY);

        for (int x = xMin; x <= xMax; x++)
        {
            for (int y = yMin; x <= yMax; y++)
            {
                long key = mapKey(x, y);
                demGridMap.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
            }
        }
    }

    /**
     * Build a map that contains dem files
     */
    private void buildMap(String directory) throws Sen2VMException
    {
        try
        {
            Path dir = FileSystems.getDefault().getPath(directory);
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            for (Path path : stream)
            {
                File currentFile = path.toFile();
                if (currentFile.isDirectory())
                {
                    buildMap(currentFile.getAbsolutePath());
                }
                else
                {
                    String filePath = currentFile.getAbsolutePath();
                    DemTile newDemTile = getDemTileFromFile(filePath);
                    if (newDemTile != null)
                    {
                        addDemTile(newDemTile);
                    }
                }
            }
            stream.close();
        }
        catch(IOException e)
        {
            throw new Sen2VMException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean findRasterFile(String directory) throws SXGeoException
    {
        try
        {
            return (demGridMap.size() != 0);
        }
        catch (Exception e)
        {
            throw new SXGeoException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRasterFilePath(double latitude, double longitude)
    {
        int latFloor = (int)FastMath.floor(FastMath.toDegrees(latitude));
        int lonFloor = (int)FastMath.floor(FastMath.toDegrees(longitude));

        // when close to the anti-meridian
        if (lonFloor >= 180)
        {
            lonFloor -= 360;
        }
        else if (lonFloor < -180)
        {
            lonFloor += 360;
        }

        long key = mapKey(lonFloor, latFloor);
        List<DemTile> candidates = demGridMap.get(key);

        if (candidates != null)
        {
            for (DemTile d: candidates)
            {
                if(d.containPoint(lonFloor, latFloor))
                {
                    return d.filePath;
                }
            }
        }

        return "";
    }

    /**
     * Get footprint information and create a DemTile object from file
     */
    public DemTile getDemTileFromFile(String filePath)
    {
        gdal.AllRegister();

        Dataset dataset = gdal.Open(filePath, gdalconstConstants.GA_ReadOnly);
        if (dataset == null)
        {
            LOGGER.severe("Error when reading  : " + gdal.GetLastErrorMsg());
            //System.err.println("Error when reading  : " + gdal.GetLastErrorMsg());
            return null;
        }

        double[] geoTransform = dataset.GetGeoTransform();

        double minX = geoTransform[0];
        double maxY = geoTransform[3];
        double pixelWidth = geoTransform[1];
        double pixelHeight = geoTransform[5];

        int imageWidth = dataset.getRasterXSize();
        int imageHeight = dataset.getRasterYSize();

        double maxX = minX + imageWidth * pixelWidth;
        double minY = maxY + imageHeight * pixelHeight;
        dataset.delete();

        String lonlat = Math.round(minX) + "/" + Math.round(minY);
        return new DemTile(minX, maxX, minY, maxY, filePath);
    }

}