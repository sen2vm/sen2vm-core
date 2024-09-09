package esa.sen2vm.input;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.Arrays;

import java.io.File;
import java.io.*;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.gdal.gdal.BuildVRTOptions;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
    public OutputFileManager() {
        gdal.AllRegister();
    }

    public void create_archi() {
        // File dossier = new File(this.filepath + File.separator + "dir");
        // boolean res = dossier.mkdir();
    }


    public void createGeoTiff(String fileName, int startPixel, int startLine,
            int step, int nbBand, String srs, double[][][] bandVal) {

        double[][] band1val = bandVal[0];
        double[][] band2val = bandVal[1];
        int nbPixels = band1val[0].length ;
        int nbLines = band1val.length ;

        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        Dataset ds;
        Band band1;
        Band band2;

        ds = driver.Create(fileName, nbPixels, nbLines, nbBand, gdalconst.GDT_Float64);

        GdalGridFileInfo fileInfo = new GdalGridFileInfo();
        fileInfo.setDs(ds);

        band1 = ds.GetRasterBand(1);
        fileInfo.setXBand(band1);
        band1.SetNoDataValue(noDataRasterValue);

        band2 = ds.GetRasterBand(2);
        fileInfo.setYBand(band2);
        band2.SetNoDataValue(noDataRasterValue);


        double[] gtInfo = getGeoTransformInfo(startPixel, step, startLine, step) ;
        ds.SetGeoTransform(gtInfo);
        ds.SetProjection(srs);

        for (int i = 0; i < nbLines; i++) {
            double[] band1_online = new double[nbPixels];
            double[] band2_online = new double[nbPixels];

            for (int j = 0; j < nbPixels; j++) {
                band1_online[j] = band1val[i][j];
                band2_online[j] = band2val[i][j];
            }

            band1.WriteRaster(0, i, nbPixels, 1, band1_online);
            band2.WriteRaster(0, i, nbPixels, 1, band2_online);
        }

        ds.GetRasterBand(1).FlushCache();
        ds.GetRasterBand(2).FlushCache();
        close(ds, band1, band2);

        LOGGER.info("Tiff saved in: " + fileName);
    }


    protected void close(Dataset ds, Band pixelBand, Band lineBand) {
        pixelBand.delete();
        lineBand.delete();
        ds.delete();
    }



    public double[] getGeoTransformInfo(int originX, int gridXStep, int originY, int gridYStep)  {

        double[] gtInfo = new double[6];
        int idx = 0;
        gtInfo[idx++] = originX;
        gtInfo[idx++] = gridXStep;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = originY;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = -gridYStep;
        return gtInfo;
    }

    public void createVRT(String vrtFilePath, Vector<String> inputTIFs)  throws Exception{

        final Vector<String> buildVRTOptions = new Vector<String>();

        // Option code here
        // buildVRTOptions.add("-te");
        // buildVRTOptions.add("start granule");

        gdal.AllRegister();
        final Dataset dataset = gdal.BuildVRT(vrtFilePath, inputTIFs, new BuildVRTOptions(buildVRTOptions));
        dataset.delete();

        // File reader
        File file = new File(vrtFilePath);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        String s = br.readLine();

        // File writer
        File file_relative_path = new File(vrtFilePath);
        FileWriter fw = new FileWriter(file_relative_path);
        BufferedWriter bw = new BufferedWriter(fw);

        // Read file
        while(s != null)
        {
            // change the reference  of the path
            if (s.contains("SourceFilename")) {
                int origin = s.indexOf("GRANULE");
                s = "      <SourceFilename relativeToVRT=\"0\">./" + s.substring(origin) ;
            }
            bw.write(s);
            bw.newLine();
            s = br.readLine();

        }
        bw.flush();
        bw.close();
        LOGGER.info("VRT saved in: " + vrtFilePath);

    }


}
