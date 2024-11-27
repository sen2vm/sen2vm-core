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
import esa.sen2vm.input.ConfigurationFile;
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

import esa.sen2vm.utils.DetectorInfo;
import esa.sen2vm.utils.BandInfo;

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

    public static String config(String filePath, String l1b_product, int step, String operation, boolean refining) throws FileNotFoundException,
            IOException, ParseException {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        objJson.put("operation", operation);
        objJson.put("deactivate_available_refining", refining);

        JSONObject steps = (JSONObject) objJson.get("steps");
        steps.put("10m_bands", (int) step / 10);
        steps.put("20m_bands", (int) step / 20);
        steps.put("60m_bands", (int) step / 60);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig); //overwrites the content of file
        writer.write(objJson.toString());
        writer.flush();
        writer.close();

        return outputConfig;

    }



    public static String configSuppIERS(String filePath, String l1b_product) throws FileNotFoundException,
            IOException, ParseException {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        objJson.remove("iers");

        String outputConfig = l1b_product + "/configuration.json";
        System.out.println(outputConfig);
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;

    }

    public static String createTestDir(String nameTest) throws IOException {
        String inputRef = "src/test/resources/tests/input/TDS1/L1B_min";
        String outputRef = "src/test/resources/tests/output/" + nameTest;
        copyFolder(new File(inputRef), new File(outputRef), true);
        return outputRef;
    }

    public static void verifyStepDirectLoc(String configFilepath, int step) throws Sen2VMException {

        ConfigurationFile configFile = new ConfigurationFile(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), configFile.getBooleanRefining());
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


    public static void verifyDirectLoc(String configFilepath, String outputRef) throws Sen2VMException, IOException {

        ConfigurationFile configFile = new ConfigurationFile(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), configFile.getBooleanRefining());
        SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);

        ArrayList<Granule> granules = sm.getGranules();

        for(int g = 0; g < granules.size(); g++) {
            File[] grids = granules.get(g).getGrids();

            int b = 0;
            for (File grid : grids) {
                if (grid != null) {
                    int len = grid.toPath().getNameCount();
                    String refGrid = outputRef + File.separator + grid.toPath().subpath(len - 4, len);
                    System.out.println(refGrid);
                    System.out.println(grid.toString());
                    assertEquals(imagesEqual(grid.toString(), refGrid), true);
                }
                b = b + 1;
            }
        }
     }



    public static boolean imagesEqual(String img1Path, String img2Path) throws IOException{
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
                    if (data1[d] != data2[d]) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

     public static String changeParams(String filePath, String[] detectors, String[] bands, String outputDir) throws FileNotFoundException,
            IOException, ParseException {

        // String[] detectors, String[] bands

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONArray detectorsJsonArray = new JSONArray();
        for (String det : detectors) {
          detectorsJsonArray.add(det);
        }

        JSONArray bandsJsonArray = new JSONArray();
        for (String band : bands) {
          bandsJsonArray.add(band);
        }

        JSONObject objJson = (JSONObject) obj;
        objJson.put("detectors",detectorsJsonArray);
        objJson.put("bands", bandsJsonArray);

        String outputParam = outputDir + "/param.json";
        FileWriter writer = new FileWriter(outputParam, false);
        writer.write(obj.toString());
        writer.close();

        return outputParam;

    }


    public static void copyFolder(File src, File dest, boolean copy) throws IOException{
        if(src.isDirectory()){
            if(!dest.exists()){
                dest.mkdir();
            }

            String files[] = src.list();

            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                copyFolder(srcFile,destFile, copy);
            }

        } else {

            if (copy) {
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0){
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            } else {
                Path records = src.toPath();
                Path recordsLink = dest.toPath();

            }
        }
    }


}
