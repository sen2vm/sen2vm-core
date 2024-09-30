package esa.sen2vm.input;

import org.hipparchus.util.FastMath;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

import org.gdal.gdal.gdal;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Band;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;

import org.sxgeo.input.dem.DemFileManager;
import org.sxgeo.exception.SXGeoException;
import org.sxgeo.properties.SXGeoResourceBundle;

import esa.sen2vm.exception.Sen2VMException;

public class SnapDemFileManager extends DemFileManager {

    // Map dem filepath with a string that represents longitude/latitude
    // Example: with a SRTM tile on Madeira island located at longitude -16 and latitude 30
    // the correponding map entry will be ("-16/30"="/DEMDIR/DEM_SRTM/w016/n30.dt1")
    // Key corresponding to "longitude/latitude" and value corresponding to the dem filepath.
    Map<String, String> demFilePathMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    public SnapDemFileManager(String demRootDir) {
        super(demRootDir);
    }

    /**
     * Build a map that contains dem files
     */
    public void buildMap(String directory) throws Sen2VMException {
        try {
            Path dir = FileSystems.getDefault().getPath(directory);
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            for (Path path : stream) {
                File currentFile = path.toFile();
                if (currentFile.isDirectory()) {
                    buildMap(currentFile.getAbsolutePath());
                } else {
                    String filePath = currentFile.getAbsolutePath();
                    String lonlat = getLonLatFromFile(filePath);
                    if (lonlat != null) {
                        demFilePathMap.put(getLonLatFromFile(filePath), filePath);
                    }
                }
            }
            stream.close();
        }
        catch(IOException e) {
            throw new Sen2VMException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean findRasterFile(String directory) throws SXGeoException {
        try {
            Path dir = FileSystems.getDefault().getPath(directory);
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            boolean found = false;
            for (Path path : stream) {
                if (!found) {
                    File currentFile = path.toFile();
                    if (currentFile.isDirectory()) {
                        found = findRasterFile(currentFile.getAbsolutePath());
                    } else {
                        String filePath = currentFile.getAbsolutePath();
                        if ( ( filePath.matches(".*.dt1") ) || ( filePath.matches(".*.dt2") )) {
                            found = true;
                        }
                    }
                    if (found) {
                        stream.close();
                        return true;
                    }
                }
            }
            stream.close();
            throw new SXGeoException("NO_RASTER_FILE_FOUND_IN_DEM");
        } catch (Exception e) {
            throw new SXGeoException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRasterFilePath(double latitude, double longitude) {
        double latFloor = FastMath.floor(FastMath.toDegrees(latitude));
        double lonFloor = FastMath.floor(FastMath.toDegrees(longitude));
        int lat = (int) latFloor;
        int lon = (int) lonFloor;

        // IPFSPR-800 : when close to the anti-meridian,
        // the input longitude is not necessarily within [-180, 180[
        if (lon >= 180) {
        	lon -= 360;
        } else if (lon < -180) {
        	lon += 360;
        }

        String str = "";
        String demDir = null;
        // Compute file path name :
        if (lon < 0) {
            str += "w";
        } else {
            str += "e";
        }
        str += String.format("%03d", FastMath.abs(lon));
        demDir = str;
        if (lat < 0) {
            str = "s";
        } else {
            str = "n";
        }
        String filePath = demRootDir + File.separator + demDir + File.separator + str + String.format("%02d", FastMath.abs(lat)) + ".dt1";

        File f = new File(filePath);

        if (!f.exists())
        {
		filePath = demRootDir + File.separator + demDir + File.separator + str + String.format("%02d", FastMath.abs(lat)) + ".dt2";
        }
        return filePath;
    }

    /**
     * Get footprint information from file
     */
    public String getLonLatFromFile(String filePath) {
        System.out.println("Read file: "+filePath);
        gdal.AllRegister();

        Dataset dataset = gdal.Open(filePath, gdalconstConstants.GA_ReadOnly);
        if (dataset == null) {
            System.err.println("Error when reading  : " + gdal.GetLastErrorMsg());
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

        System.out.printf("Emprise du fichier :\n");
        System.out.printf("Min Longitude (X): %f\n", minX);
        System.out.printf("Max Longitude (X): %f\n", maxX);
        System.out.printf("Min Latitude (Y): %f\n", minY);
        System.out.printf("Max Latitude (Y): %f\n", maxY);

        dataset.delete();

        String lonlat = (int) minX + "/" + (int) minY;
        System.out.println("lonlat: "+lonlat);

        return lonlat;
    }
}
