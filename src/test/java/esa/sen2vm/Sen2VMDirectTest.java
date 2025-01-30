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
import org.orekit.time.TimeScalesFactory;
import org.orekit.rugged.linesensor.LineSensor;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


/**
 * Unit test for Sen2VM.
 */

public class Sen2VMDirectTest
{

    String configTmp = "src/test/resources/tests/input/TDS1/configuration_TDS1_direct.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/input/ref";

    // @Test
    public void geoLocD01B01firstPixel()
    {

        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        int[] testsStep = new int[]{3000, 6000};

        String granulePath = "src/test/resources/tests/6km_ref/GRANULE/S2A_OPER_MSI_L1B_GR_DPRM_20140630T140000_S20200816T120226_D01_N05.00/GEO_DATA/S2A_OPER_GEO_L1B_GR_DPRM_20140630T140000_S20200816T120226_D01_B01.tif";

        for (int step : testsStep) {
            try {
                String nameTest = "direct_loc_" + Integer.toString(step);
                String outputDir = Utils.createTestDir(nameTest, "direct");
                String config = Utils.config(configTmp, outputDir, step, "direct", false);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);

                Dataset granule = gdal.Open(outputDir + "/GRANULE/S2A_OPER_MSI_L1B_GR_DPRM_20140630T140000_S20200816T120226_D01_N05.00/GEO_DATA/S2A_OPER_GEO_L1B_GR_DPRM_20140630T140000_S20200816T120226_D01_B01.tif", 0);
                Band b1 = granule.GetRasterBand(1);
                Band b2 = granule.GetRasterBand(2);
                Band b3 = granule.GetRasterBand(3);
                double[] geoGrid = {0.0,0.0,0.0};
                for(int i = 0; i < 1; i++)
                {
                    double[] data1 = new double[granule.getRasterXSize()];
                    b1.ReadRaster(0, i, granule.getRasterXSize(), 1, data1);
                    geoGrid[0] = data1[0];
                    b2.ReadRaster(0, i, granule.getRasterXSize(), 1, data1);
                    geoGrid[1] = data1[0];
                    b3.ReadRaster(0, i, granule.getRasterXSize(), 1, data1);
                    geoGrid[2] = data1[0];
                }

                double[][] grounds = geoLocD01B01(configTmp, "01", "B01", 1.0f, 0.0f);
                System.out.println("pixels 1.0, 0.0 in sensor = "+grounds[0][0]+" "+grounds[0][1]+" "+grounds[0][2]);
                System.out.println(" first pixels in geogrid = "+geoGrid[0]+" "+geoGrid[1]+" "+geoGrid[2]);

                assertEquals(grounds[0][0], geoGrid[0]);
                assertEquals(grounds[0][0], -18.919175317847085);
                assertEquals(grounds[0][1], geoGrid[1]);
                assertEquals(grounds[0][1], 33.79427774463745);
                assertEquals(grounds[0][2], geoGrid[2]);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Sen2VMException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // @Test
    public void geoTimeFirstLine()
    {
        int step = 6000;

        try {
            String nameTest = "direct_first_line_" + Integer.toString(step);
            String outputDir = Utils.createTestDir(nameTest, "direct");
            String config = Utils.config(configTmp, outputDir, step, "direct", false);
            String param = Utils.changeParams(paramTmp, new String[]{"01"}, new String[]{"B01"}, outputDir);

            Configuration configFile = new Configuration(config);
            List<DetectorInfo> detectors = new ArrayList<DetectorInfo>();
            detectors.add(DetectorInfo.getDetectorInfoFromName("01"));
            List<BandInfo> bands = new ArrayList<BandInfo>();
            bands.add(BandInfo.getBandInfoFromNameWithB("B01"));
            DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());
            GIPPManager gippManager = new GIPPManager(configFile.getGippFolder(), bands, dataStripManager, configFile.getGippVersionCheck());

            // Build sensor list
            // Save sensors for each focal plane
            HashMap<String, Sensor> sensorList = new HashMap<String, Sensor>();
            for (DetectorInfo detectorInfo: detectors) {
                for (BandInfo bandInfo: bands) {
                    SensorViewingDirection viewing = gippManager.getSensorViewingDirections(bandInfo, detectorInfo);
                    LineDatation lineDatation = dataStripManager.getLineDatation(bandInfo, detectorInfo);
                    SpaceCraftModelTransformation pilotingToMsi = gippManager.getPilotingToMsiTransformation();
                    SpaceCraftModelTransformation msiToFocalplane = gippManager.getMsiToFocalPlaneTransformation(bandInfo);
                    SpaceCraftModelTransformation focalplaneToSensor = gippManager.getFocalPlaneToDetectorTransformation(bandInfo, detectorInfo);

                    // Save sensor information
                    Sensor sensor = new Sensor(
                        bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD(),
                        viewing,
                        lineDatation,
                        bandInfo.getPixelHeight(),
                        focalplaneToSensor,
                        msiToFocalplane,
                        pilotingToMsi
                    );
                    System.out.println(sensor.getName());
                    sensorList.put(sensor.getName(), sensor);
                }
            }

            // Init demManager
            Boolean isOverlappingTiles = true; // geoid is a single file (not tiles) so set overlap to True by default
            SrtmFileManager demFileManager = new SrtmFileManager(configFile.getDem());

            GeoidManager geoidManager = new GeoidManager(configFile.getGeoid(), isOverlappingTiles);
            DemManager demManager = new DemManager(
                demFileManager,
                geoidManager,
                isOverlappingTiles);

            // Init rugged instance
            RuggedManager ruggedManager = RuggedManager.initRuggedManagerDefaultValues(
                demManager,
                dataStripManager.getDataSensingInfos(),
                Sen2VMConstants.MINMAX_LINES_INTERVAL_QUARTER,
                Sen2VMConstants.RESOLUTION_10M_DOUBLE,
                new ArrayList(sensorList.values()),
                Sen2VMConstants.MARGIN,
                dataStripManager.getRefiningInfo()
            );
            ruggedManager.setLightTimeCorrection(false);
            ruggedManager.setAberrationOfLightCorrection(false);

            // Init simpleLocEngine
            SimpleLocEngine simpleLocEngine = new SimpleLocEngine(
                dataStripManager.getDataSensingInfos(),
                ruggedManager,
                demManager
            );


            double[][] pixels = {{0., 0.}};
            double[][] grounds = simpleLocEngine.computeDirectLoc(sensorList.get("B01/D01"), pixels);

            LineSensor lineSensor = ruggedManager.getLineSensor("B01/D01");
            String date = lineSensor.getDate(0.5).toString(TimeScalesFactory.getGPS());
            System.out.println("date line 0.5:" + date);

            assertEquals(date, "2020-08-16T12:02:45.812731");

        } catch (IOException e) {
            e.printStackTrace();
        } catch ( SXGeoException e ) {
            e.printStackTrace();
        }  catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    // @Test
    public void geoLocD01B01_fromAsgard()
    {
        double delta = 1e-9;
        try {
            double[][] grounds = geoLocD01B01(configTmp, "01", "B01", 0.0, 0.0);
            System.out.println("pixels = 0.0 0.0 grounds = "+grounds[0][0]+" "+grounds[0][1]+" "+grounds[0][2]);
            assertEquals(grounds[0][0], -18.919024167218094, delta);
            assertEquals(grounds[0][1], 33.79483143151926, delta);
            assertEquals(grounds[0][2], 42.538715533140156, delta);

            grounds = geoLocD01B01(configTmp, "01", "B01", 250.5, 700.5);
            System.out.println("pixels = 0.0 0.0 grounds = "+grounds[0][0]+" "+grounds[0][1]+" "+grounds[0][2]);
            assertEquals(grounds[0][0], -18.490305707482214, delta);
            assertEquals(grounds[0][1], 33.58277655913304, delta);
            assertEquals(grounds[0][2], 43.448338191393816, delta);

        } catch (Sen2VMException e) {
            e.printStackTrace();
        }

    }

    public double[][] geoLocD01B01(String config, String det, String band, double line, double pixel) throws Sen2VMException
    {
        double[][] grounds = {{0., 0.}};
        try {

            // Read configuration file
            Configuration configFile = new Configuration(config);

            List<DetectorInfo> detectors = new ArrayList<DetectorInfo>();
            detectors.add(DetectorInfo.getDetectorInfoFromName(det));

            List<BandInfo> bands = new ArrayList<BandInfo>();
            bands.add(BandInfo.getBandInfoFromNameWithB(band));

            // Read datastrip
            DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());

            // Read GIPP
            GIPPManager gippManager = new GIPPManager(configFile.getGippFolder(), bands, dataStripManager, configFile.getGippVersionCheck());

            // Initialize SimpleLocEngine

            // Init demManager
            Boolean isOverlappingTiles = true; // geoid is a single file (not tiles) so set overlap to True by default
            SrtmFileManager demFileManager = new SrtmFileManager(configFile.getDem());
            if(!demFileManager.findRasterFile()) {
                throw new Sen2VMException("Error when checking for DEM file");
            }

            GeoidManager geoidManager = new GeoidManager(configFile.getGeoid(), isOverlappingTiles);
            DemManager demManager = new DemManager(
                demFileManager,
                geoidManager,
                isOverlappingTiles);

            // Build sensor list
            // Save sensors for each focal plane
            HashMap<String, Sensor> sensorList = new HashMap<String, Sensor>();
            for (DetectorInfo detectorInfo: detectors) {
                for (BandInfo bandInfo: bands) {
                    SensorViewingDirection viewing = gippManager.getSensorViewingDirections(bandInfo, detectorInfo);
                    LineDatation lineDatation = dataStripManager.getLineDatation(bandInfo, detectorInfo);
                    SpaceCraftModelTransformation pilotingToMsi = gippManager.getPilotingToMsiTransformation();
                    SpaceCraftModelTransformation msiToFocalplane = gippManager.getMsiToFocalPlaneTransformation(bandInfo);
                    SpaceCraftModelTransformation focalplaneToSensor = gippManager.getFocalPlaneToDetectorTransformation(bandInfo, detectorInfo);

                    // Save sensor information
                    Sensor sensor = new Sensor(
                        bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD(),
                        viewing,
                        lineDatation,
                        bandInfo.getPixelHeight(),
                        focalplaneToSensor,
                        msiToFocalplane,
                        pilotingToMsi
                    );
                    sensorList.put(sensor.getName(), sensor);
                }
            }

            // Init rugged instance
            RuggedManager ruggedManager = RuggedManager.initRuggedManagerDefaultValues(
                demManager,
                dataStripManager.getDataSensingInfos(),
                Sen2VMConstants.MINMAX_LINES_INTERVAL_QUARTER,
                Sen2VMConstants.RESOLUTION_10M_DOUBLE,
                new ArrayList(sensorList.values()),
                Sen2VMConstants.MARGIN,
                dataStripManager.getRefiningInfo()
            );
            ruggedManager.setLightTimeCorrection(false);
            ruggedManager.setAberrationOfLightCorrection(false);

            // Init simpleLocEngine
            SimpleLocEngine simpleLocEngine = new SimpleLocEngine(
                dataStripManager.getDataSensingInfos(),
                ruggedManager,
                demManager
            );

            double[][] pixels = {{line, pixel}};
            grounds = simpleLocEngine.computeDirectLoc(sensorList.get("B01/D01"), pixels);

        } catch ( SXGeoException e ) {
            e.printStackTrace();
        }  catch (Sen2VMException e) {
            e.printStackTrace();
        }

        return grounds;

    }

     public static HashMap<String, Sensor> getSensorHashMap(List<DetectorInfo> detectors, List<BandInfo> bands,
         DataStripManager dataStripManager, GIPPManager gippManager) throws Sen2VMException {

        // Save sensors for each focal plane
        HashMap<String, Sensor> sensorList = new HashMap<String, Sensor>();
        for (DetectorInfo detectorInfo: detectors) {
            for (BandInfo bandInfo: bands) {
                SensorViewingDirection viewing = gippManager.getSensorViewingDirections(bandInfo, detectorInfo);
                LineDatation lineDatation = dataStripManager.getLineDatation(bandInfo, detectorInfo);
                SpaceCraftModelTransformation pilotingToMsi = gippManager.getPilotingToMsiTransformation();
                SpaceCraftModelTransformation msiToFocalplane = gippManager.getMsiToFocalPlaneTransformation(bandInfo);
                SpaceCraftModelTransformation focalplaneToSensor = gippManager.getFocalPlaneToDetectorTransformation(bandInfo, detectorInfo);

                // Save sensor information
                Sensor sensor = new Sensor(
                    bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD(),
                    viewing,
                    lineDatation,
                    bandInfo.getPixelHeight(),
                    focalplaneToSensor,
                    msiToFocalplane,
                    pilotingToMsi
                );
                sensorList.put(sensor.getName(), sensor);
            }

        }
        return sensorList;
    }

    // @Test
    public void step()
    {
        String[] detectors = new String[]{"01", "02"};
        String[] bands = new String[]{"B01", "B02", "B05"};
        int[] testsStep = new int[]{3000}; // , 6000};

        for (int step : testsStep) {
            try {
                String nameTest = "direct_step_" +  Integer.toString(step);
                String outputDir = Utils.createTestDir(nameTest, "direct");
                String config = Utils.config(configTmp, outputDir, step, "direct", false);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyStepDirectLoc(config, step);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Sen2VMException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // @Test
    public void geoLoc()
    {
        String[] detectors = new String[]{"01","02","03","04","05","06","07","08","09","10","11","12"};
        String[] bands = new String[]{"B01","B02","B03","B04","B05","B06","B07","B08","B8A", "B09","B10","B11","B12"};
        try {
            int step = 6000;
            String nameTest = "direct_" +  Integer.toString(step);
            String outputDir = Utils.createTestDir(nameTest, "direct");
            String config = Utils.config(configTmp, outputDir, step, "direct", false);
            String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /* // @Test
    public void testGipp()
    {

        boolean[] checksGipp = new boolean[]{true, false};
        String gipp_2 = "/Sen2vm/sen2vm-core/src/test/resources/tests/input/TDS1/inputs/GIPP/"; // Todo

        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};

        for (boolean checkGipp : checksGipp) {
            boolean thrown = false;

            try {
                String nameTest = "direct_checkGipp_" + checkGipp;
                String outputDir = Utils.createTestDir(nameTest, "direct");
                String config = Utils.configCheckGipp(configTmp, gipp_2, checkGipp, outputDir);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Sen2VMException e) {
                thrown = true;
            } catch (ParseException e) {
                e.printStackTrace();
            }

            assertEquals(thrown, !checkGipp);
        }
    }*/

    // @Test
    public void geoRefining()
    {

        boolean[] testsRef = new boolean[]{true, false};
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01","B02","B05"};

        for (boolean ref : testsRef) {
            try {
                String nameTest = "direct_refining_" + ref;
                String outputDir = Utils.createTestDir(nameTest, "direct");
                String config = Utils.config(configTmp, outputDir, 6000, "direct", ref);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                System.out.println(config);
                System.out.println(refDir + "/" + nameTest);
                Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Sen2VMException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // @Test
    public void iersHandling()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01","B02","B05"};

        try {
            String nameTest = "direct_no_iers";
            String outputDir = Utils.createTestDir(nameTest, "direct");
            String config = Utils.configSuppIERS(configTmp, outputDir);
            String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void parallelisationRobustness()
    {

        try {
            String outputDir1 = Utils.createTestDir("direct_order_1", "direct");
            String[] detectors_order_1 = new String[]{"01", "02"};
            String[] bands_order_1 = new String[]{"B01", "B02"};
            String config_order_1 = Utils.config(configTmp, outputDir1, 6000, "direct", false);
            String param_order_1 = Utils.changeParams(paramTmp, detectors_order_1, bands_order_1, outputDir1);
            String[] args_order_1 = {"-c", config_order_1, "-p", param_order_1};
            Sen2VM.main(args_order_1);

            String outputDir2 = Utils.createTestDir("direct_order_2", "direct");
            String[] detectors_order_2 = new String[]{"02", "01"};
            String[] bands_order_2 = new String[]{"B02", "B01"};
            String config_order_2 = Utils.config(configTmp, outputDir2, 6000, "direct", false);
            String param_order_2 = Utils.changeParams(paramTmp, detectors_order_2, bands_order_2, outputDir2);
            String[] args_order_2 = {"-c", config_order_2, "-p", param_order_2};
            Sen2VM.main(args_order_2);

            Utils.verifyDirectLoc(config_order_2, outputDir1);
            Utils.verifyDirectLoc(config_order_1, outputDir2);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void dem()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01","B02"};
        String[] testsDem = new String[]{"dem_1", "dem_2", "dem_3", "dem_4"};
        String refDir = "src/test/resources/tests/input/ref/direct_dem_D01_B01/";
        for (String testDem : testsDem) {

            try {
                String nameTest = "direct_" + testDem;
                String outputDir = Utils.createTestDir(nameTest, "direct");
                String config = Utils.changeDem(configTmp, "src/test/resources/tests/input/dem_tests/" + testDem, outputDir);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyDirectLoc(config, "src/test/resources/tests/input/ref/direct_dem_D01_B01/");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Sen2VMException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }



}
