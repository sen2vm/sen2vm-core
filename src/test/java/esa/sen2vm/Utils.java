package esa.sen2vm;


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Float;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.Configuration;
import esa.sen2vm.utils.Sen2VMConstants;

import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import esa.sen2vm.input.granule.GranuleManager;
import esa.sen2vm.input.granule.Granule;
import esa.sen2vm.input.gipp.GIPPManager;
import esa.sen2vm.input.SafeManager;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.gdal.gdal.BuildVRTOptions;

import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.enums.BandInfo;

import org.orekit.rugged.linesensor.LineDatation;

import org.sxgeo.engine.SimpleLocEngine;
import org.sxgeo.input.datamodels.RefiningInfo;
import org.sxgeo.input.datamodels.sensor.Sensor;
import org.sxgeo.input.datamodels.sensor.SensorViewingDirection;
import org.sxgeo.input.datamodels.sensor.SpaceCraftModelTransformation;
import org.sxgeo.input.dem.DemManager;
import org.sxgeo.input.dem.DemFileManager;
import org.sxgeo.input.dem.SrtmFileManager;
import org.sxgeo.input.dem.GeoidManager;
import org.sxgeo.rugged.RuggedManager;
import org.sxgeo.exception.SXGeoException;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


public class Utils {


    public Utils() {
    }

    private static final double THRESHOLD_DIR = 1e-8;
    private static final double THRESHOLD_INV = 1e-6; // todo

    public static void verifyStepDirectLoc(String configFilepath, int step) throws Sen2VMException
    {

        Configuration configFile = new Configuration(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());
        SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);

        ArrayList<Granule> granules = sm.getGranules();

        for(int g = 0; g < granules.size(); g++) {
            File[] grids = granules.get(g).getGrids();

            int b = 0;
            for (File grid : grids) {
                if (grid != null) {
                    double res = BandInfo.getBandInfoFromIndex(b).getPixelHeight();
                    Dataset ds = gdal.Open(grid.getPath());
                    double[] transform = ds.GetGeoTransform();
                    assertEquals(transform[1] * res, step);
                    assertEquals(transform[5] * res, step);
                    ds.delete();
                }
                b = b + 1;
            }
        }
    }

    public static void verifyStepInverseLoc(String configFilepath, int step) throws Sen2VMException
    {

        Configuration configFile = new Configuration(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());
        SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);

        ArrayList<Granule> granules = sm.getGranules();

        for(int g = 0; g < granules.size(); g++) {
            File[] grids = granules.get(g).getGrids();

            int b = 0;
            for (File grid : grids) {
                if (grid != null) {
                    double res = BandInfo.getBandInfoFromIndex(b).getPixelHeight();
                    Dataset ds = gdal.Open(grid.getPath());
                    double[] transform = ds.GetGeoTransform();
                    assertEquals(transform[1] * res, step);
                    assertEquals(transform[5] * res, step);
                    ds.delete();
                }
                b = b + 1;
            }
        }
    }


    public static void verifyDirectLoc(String configFilepath, String outputRef) throws Sen2VMException, IOException
    {
        Configuration configFile = new Configuration(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());
        SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);

        ArrayList<Granule> granules = sm.getGranules();

        for(int g = 0; g < granules.size(); g++) {
            File[] grids = granules.get(g).getGrids();

            int b = 0;
            for (File grid : grids) {
                if (grid != null) {
                    int len = grid.toPath().getNameCount();
                    String refGrid = outputRef + File.separator + grid.toPath().subpath(len - 4, len);
                    assertEquals(imagesEqualDirect(grid.toString(), refGrid,THRESHOLD_DIR), true);
                }
                b = b + 1;
            }
        }
     }

    public static void verifyInverseLoc(String configFilepath, String outputRef) throws Sen2VMException, IOException
    {
        Configuration configFile = new Configuration(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());
        SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);
        File[][] outputGrids = sm.getInverseGrids(configFile.getInverseLocOutputFolder());
        File[][] refGrids = sm.getInverseGrids(outputRef);

        for(int d = 0; d < Sen2VMConstants.NB_DETS; d++)
        {
            for(int b = 0; b < Sen2VMConstants.NB_BANDS; b++)
            {
                BandInfo bandInfo = BandInfo.getBandInfoFromIndex(b);
                float res = (float) bandInfo.getPixelHeight();
                if (outputGrids[d][b] != null) {
                    File outputGrid = outputGrids[d][b];
                    File refGrid = refGrids[d][b];
                    assertEquals(imagesEqualInverse(outputGrid.toString(), refGrid.toString(), THRESHOLD_INV, res), true);
                }
            }
        }
     }



    public static boolean imagesEqualDirect(String img1Path, String img2Path, double threshold) throws IOException{

        Dataset ds1 = gdal.Open(img1Path, 0);
        Dataset ds2 = gdal.Open(img2Path, 0);
        if (ds1.GetRasterCount() == ds2.GetRasterCount() && ds1.getRasterXSize() == ds2.getRasterXSize() && ds1.getRasterYSize() == ds2.getRasterYSize()) {

            for(int b = 1; b <= ds1.GetRasterCount(); b++)
            {
                Band b1 = ds1.GetRasterBand(b);
                Band b2 = ds2.GetRasterBand(b);

                for(int r = 0; r < ds1.getRasterYSize(); r++) {
                    double[] data1 = new double[ds1.getRasterXSize()];
                    b1.ReadRaster(0, r, ds1.getRasterXSize(), 1, data1);
                    double[] data2 = new double[ds2.getRasterXSize()];
                    b2.ReadRaster(0, r, ds2.getRasterXSize(), 1, data2);

                    for(int c = 0; c < ds1.getRasterXSize(); c++) {
                        if (!(Double.isNaN(data1[c]) == Double.isNaN(data2[c])))
                        {
                            return false;
                        }

                        if (!(Double.isNaN(data1[c]))  && Math.abs(data1[c] - data2[c]) > threshold) {
                            System.out.println("Error in " + img1Path);
                            String error = String.valueOf(data1[c]) + " - " + String.valueOf(data1[c]) + " = " + String.valueOf(data1[c] - data2[c]);
                            System.out.println("Band "+ String.valueOf(b) + "/ coordinates (" + String.valueOf(r) + "," + String.valueOf(c) + "): " + error);
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

     public static boolean imagesEqualInverse(String img1Path, String img2Path, double threshold, Float res) throws IOException{

        Dataset ds1 = gdal.Open(img1Path, 0);
        Dataset ds2 = gdal.Open(img2Path, 0);
        if (ds1.GetRasterCount() == ds2.GetRasterCount() && ds1.getRasterXSize() == ds2.getRasterXSize() && ds1.getRasterYSize() == ds2.getRasterYSize()) {

            Band ds1b1 = ds1.GetRasterBand(1);
            Band ds1b2 = ds1.GetRasterBand(2);
            Band ds2b1 = ds2.GetRasterBand(1);
            Band ds2b2 = ds2.GetRasterBand(2);

            for(int r = 0; r < ds1.getRasterYSize(); r++) {

                double[] data1b1 = new double[ds1.getRasterXSize()];
                ds1b1.ReadRaster(0, r, ds1.getRasterXSize(), 1, data1b1);
                double[] data1b2 = new double[ds1.getRasterXSize()];
                ds1b2.ReadRaster(0, r, ds1.getRasterXSize(), 1, data1b2);

                double[] data2b1 = new double[ds1.getRasterXSize()];
                ds2b1.ReadRaster(0, r, ds1.getRasterXSize(), 1, data2b1);
                double[] data2b2 = new double[ds1.getRasterXSize()];
                ds2b2.ReadRaster(0, r, ds1.getRasterXSize(), 1, data2b2);

                for(int c = 0; c < ds1.getRasterXSize(); c++) {

                    // nan in one grid and value in other grid case
                    if (!(Double.isNaN(data1b1[c]) == Double.isNaN(data2b1[c])))
                    {
                        return false;
                    }

                    // values in both grids
                    if (!(Double.isNaN(data1b1[c]))) {

                        // Calculation planar error
                        double diff_column = data1b1[c] - data2b1[c];
                        double diff_column_2 = diff_column * diff_column;
                        double diff_line = data1b2[c] - data2b2[c];
                        double diff_line_2 = diff_line * diff_line;
                        double diff = Math.sqrt(diff_line_2 + diff_column_2);
                        diff = diff * res;

                        if (diff > threshold) {
                            System.out.println("Error in " + img1Path);
                            String error = "(" + String.valueOf(data1b2[c]) + ", " + String.valueOf(data1b1[c])  + ")";
                            error = error + " vs (" + String.valueOf(data2b2[c]) + ", " + String.valueOf(data2b1[c]) + ")";
                            error = error + " = " + String.valueOf(diff);
                            System.out.println("Coordinates (" + String.valueOf(r) + "," + String.valueOf(c) + "): " + error);
                            return false;
                        }
                    }

                }
            }

            return true;
        }
        return false;
    }



}
