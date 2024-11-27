package esa.sen2vm.output;

import java.util.Vector;

import java.io.File;
import java.io.*;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdal.BuildVRTOptions;

import java.util.logging.Logger;

public class OutputFileManager
{
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(OutputFileManager.class.getName());
    protected Dataset dataset = null;
    protected Driver driver = null;
    protected Double noDataRasterValue = Double.NaN;

    /**
     * Constructor
     * @param filepath Path to the configuration file to parse
     */
    public OutputFileManager()
    {
        gdal.AllRegister();
    }

    /**
     * Save grid in 3D TIFF  (lon, lat, alt)
     * @param fileName path the file to save the grid in
     * @param startPixel pixel upper left
     * @param startLine line upper left
     * @param endLine line bottom left
     * @param step step of the grid (metadata)
     * @param bandVal 3d ground coordinate array as [[[lon00,lon01,...],[...]],[[lat00,lat01,,...], [...]],[[alt00,alt01,,...], [...]]] in deg, deg, m
     * @param src epsg
     * @param srs subLineOffset line offset of the grid (metadata)
     */
     public void createGeoTiff(String fileName, Float startPixel, Float startLine,
            Float stepX, Float stepY, double[][][] bandVal, String epsg, String epsgData, Float lineOffset, Float pixelOffset)
    {

        int nbBand = bandVal.length;
        double[][] band1val = bandVal[0];
        int nbPixels = band1val[0].length;
        int nbLines = band1val.length;

        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        // String[] options = {"COMPRESS=LZW", "PREDICTOR=2"};

        Dataset ds = driver.Create(fileName, nbPixels, nbLines, nbBand, gdalconst.GDT_Float64);
        GdalGridFileInfo fileInfo = new GdalGridFileInfo();
        fileInfo.setDs(ds);

        // Add metadata
        ds.SetMetadataItem("X_BAND", "1");
        ds.SetMetadataItem("Y_BAND", "2");
        if (nbBand == 3)
        {
            ds.SetMetadataItem("Z_BAND", "3");
        }
        ds.SetMetadataItem("PIXEL_OFFSET", String.valueOf(pixelOffset));
        ds.SetMetadataItem("PIXEL_STEP", String.valueOf(stepX));
        ds.SetMetadataItem("LINE_OFFSET", String.valueOf(lineOffset));
        ds.SetMetadataItem("LINE_STEP", String.valueOf(stepY));
        ds.SetMetadataItem("SRS", epsgData);
        ds.SetMetadataItem("GEOREFERENCING_CONVENTION", "TOP_LEFT_CORNER");
        // ds.SetDescription("Direct Location Grid");

        Vector<Band> bands = new Vector<Band>();
        for (int b = 0; b < nbBand; b++)
        {
            Band band = ds.GetRasterBand(b+1);
            fileInfo.setXBand(band);
            band.SetNoDataValue(noDataRasterValue);
            bands.add(band);
        }

        double[] gtInfo = getGeoTransformInfo(startPixel, stepX, startLine, stepY);
        ds.SetGeoTransform(gtInfo);
        ds.SetProjection(epsg);

        for (int b = 0; b < nbBand; b++)
        {
            for (int i = 0; i < nbLines; i++)
            {
                double[] band_online = new double[nbPixels];
                for (int j = 0; j < nbPixels; j++)
                {
                    band_online[j] = bandVal[b][i][j];
                }
                bands.get(b).WriteRaster(0, i, nbPixels, 1, band_online);
            }
            ds.GetRasterBand(b+1).FlushCache();
        }

        ds.FlushCache();

        for (int b = 0; b < nbBand; b++)
        {
            bands.get(b).delete();
        }
        ds.delete();

        LOGGER.info("Granule grid saved in: " + fileName);
    }


    /**
     * Create geotransform for the image
     * @param originX x-coordinate of the upper-left corner of the upper-left pixel.
     * @param gridXStep w-e pixel resolution / pixel width.
     * @param originY y-coordinate of the upper-left corner of the upper-left pixel.
     * @param gridYStep n-s pixel resolution / pixel height (negative value for a north-up image).
     */
    public double[] getGeoTransformInfo(Float originX, Float gridXStep, Float originY, Float gridYStep)
    {
        double[] gtInfo = new double[6];
        int idx = 0;
        gtInfo[idx++] = originX;
        gtInfo[idx++] = gridXStep;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = originY;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = gridYStep;
        return gtInfo;
    }

    /**
     * Build VRT from a list of input TIFFs and transform absolute to relative path from DS directory
     * @param vrtFilePath path of the VRT file to save to create # TODO tmp
     * @param inputTIFs list of TIFFs
     */
    public void createVRT(String vrtFilePath, Vector<String> inputTIFs,  Float step,
                            Float lineOffset, Float pixelOffset, boolean exportAlt)
    {
        // Create file tmp
        String vrtFilePath_tmp = vrtFilePath.substring(0, vrtFilePath.length() -4) + "_tmp.vrt";

        // Option code here
        final Vector<String> buildVRTOptions = new Vector<String>();
        // buildVRTOptions.add("-non_strict");

        // Start build VRT process
        gdal.AllRegister();
        final Dataset ds = gdal.BuildVRT(vrtFilePath_tmp, inputTIFs, new BuildVRTOptions(buildVRTOptions));

        // Add metadata
        ds.SetMetadataItem("X_BAND", "1"); // Longitude
        ds.SetMetadataItem("Y_BAND", "2"); // Latitude
        if (exportAlt)
        {
            ds.SetMetadataItem("Z_BAND", "3"); // Altitude
        }
        ds.SetMetadataItem("PIXEL_OFFSET", String.valueOf(pixelOffset));
        ds.SetMetadataItem("PIXEL_STEP", String.valueOf(step));
        ds.SetMetadataItem("LINE_OFFSET", String.valueOf(lineOffset));
        ds.SetMetadataItem("LINE_STEP", String.valueOf(step));
        ds.SetMetadataItem("SRS", "EPSG:4326");
        ds.SetMetadataItem("GEOREFERENCING_CONVENTION", "TOP_LEFT_CORNER");
        ds.delete();
    }

    /**
     * Change GeoTransform stepY to -stepY and originY to -originY
     * @param inputTIFs list of TIFFs to change
     */
     public void correctGeoGrid(Vector<String> inputTIFs)
     {
        for(int g = 0; g < inputTIFs.size(); g++ )
        {
            Dataset ds = gdal.Open(inputTIFs.get(g), 0);
            double[] transform = ds.GetGeoTransform();
            double[] gtInfo = getGeoTransformInfo((float) transform[0], (float) transform[1], (float) -transform[3], (float)  -transform[5]);
            ds.SetGeoTransform(gtInfo);
            ds.FlushCache();
            ds.delete();
        }
    }

    /**
     * Change absolute paths to relative paths from GRANULE dir
     * and change stepY to -stepY and originY to -originY
     * @param vrtFilePath path of the VRT file to correct # TODO tmp
     */
    public void correctVRT(String vrtFilePath) throws Exception
    {
        String vrtFilePath_tmp = vrtFilePath.substring(0,vrtFilePath.length() -4) + "_tmp.vrt";

        // File reader
        File file = new File(vrtFilePath_tmp);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String s = br.readLine();

        // File writer
        File file_relative_path = new File(vrtFilePath);
        FileWriter fw = new FileWriter(file_relative_path);
        BufferedWriter bw = new BufferedWriter(fw);

        // Read file
        while(s != null)
        {
            // change the reference  of the path if SourceFilename
            if (s.contains("SourceFilename"))
            {
                int origin = s.lastIndexOf("GRANULE");
                s = "      <SourceFilename relativeToVRT=\"1\">../../../" + s.substring(origin);
            }
            if (s.contains("GeoTransform"))
            {
                String[] parts = s.split(",");
                float originYf = - Float.parseFloat(parts[3]);
                float stepYf = - Float.parseFloat(parts[5].split("<")[0]);
                s = parts[0] + "," + parts[1] + "," + parts[2];
                s = s + "," + String.valueOf(originYf) + "," + parts[4] + "," + stepYf + " </GeoTransform>";
            }
            bw.write(s);
            bw.newLine();
            s = br.readLine();
        }
        bw.flush();
        bw.close();
        br.close();
        file.delete();

        LOGGER.info("VRT saved in: " + vrtFilePath);
    }
}
