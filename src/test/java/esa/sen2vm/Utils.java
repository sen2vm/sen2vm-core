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

    private static final double THRESHOLD_DIR = 1e-9;
    private static final double THRESHOLD_INV = 1e-3;

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
                    assertEquals(imagesEqual(grid.toString(), refGrid,THRESHOLD_DIR), true);
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
                if (outputGrids[d][b] != null) {
                    File outputGrid = outputGrids[d][b] ;
                    File refGrid = refGrids[d][b] ;
                    assertEquals(imagesEqual(outputGrid.toString(), refGrid.toString(), THRESHOLD_INV), true);
                }
            }
        }
     }



    public static boolean imagesEqual(String img1Path, String img2Path, double threshold) throws IOException{

        Dataset ds1 = gdal.Open(img1Path, 0);
        Dataset ds2 = gdal.Open(img2Path, 0);
        if (ds1.GetRasterCount() == ds2.GetRasterCount() && ds1.getRasterXSize() == ds2.getRasterXSize() && ds1.getRasterYSize() == ds2.getRasterYSize()) {
            Band b1 = ds1.GetRasterBand(1);
            Band b2 = ds2.GetRasterBand(1);

            for(int i = 0; i < ds1.getRasterYSize(); i++)
            {
                double[] data1 = new double[ds1.getRasterXSize()];
                b1.ReadRaster(0, i, ds1.getRasterXSize(), 1, data1);
                double[] data2 = new double[ds2.getRasterXSize()];
                b2.ReadRaster(0, i, ds2.getRasterXSize(), 1, data2);
                for(int d = 0; d < ds1.getRasterXSize(); d++) {
                    if (!(Double.isNaN(data1[d]) == Double.isNaN(data2[d])))
                    {
                        return false;
                    }
                    if (!(Double.isNaN(data1[d]))  && Math.abs(data1[d] - data2[d]) < threshold) {
                        System.out.println("[DEBUG] " + img1Path + " " + img2Path);
                        String error = String.valueOf(data1[d]) + " - " + String.valueOf(data1[d]) + " = " + String.valueOf(data1[d] - data2[d]);
                        System.out.println("[DEBUG] " + String.valueOf(i) + "/" + String.valueOf(d) + ": " + error);

                    }

                    if (!(Double.isNaN(data1[d]))  && Math.abs(data1[d] - data2[d]) > threshold)
                    {
                        System.out.println("[DEBUG] " + img1Path + " " + img2Path);
                        String error = String.valueOf(data1[d]) + " - " + String.valueOf(data1[d]) + " = " + String.valueOf(data1[d] - data2[d]);
                        System.out.println("[DEBUG] " + String.valueOf(i) + "/" + String.valueOf(d) + ": " + error);
                        return false;
                    }


                }
            }
            return true;
        }
        return false;
    }



}
