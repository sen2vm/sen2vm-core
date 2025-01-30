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

public class Sen2VMInverseTest
{

    String configTmp = "src/test/resources/tests/input/TDS1/configuration_TDS1_inverse.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/input/ref";

    // @Test
    public void step_inverse()
    {

        String[] detectors = new String[]{"01", "02"};
        String[] bands = new String[]{"B01", "B02", "B05"};
        int[] testsStep = new int[]{3000, 6000};


        for (int step : testsStep) {
            try {
                String nameTest = "inverse_step_" +  Integer.toString(step) ;
                String outputDir = Utils.createTestDir(nameTest, "inverse");
                String config = Utils.config(configTmp, outputDir, step, "inverse", false);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                System.out.println(config);
                Sen2VM.main(args);

                Configuration configFile = new Configuration(config);
                DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());
                SafeManager safeManager = new SafeManager(configFile.getL1bProduct(), dataStripManager);
                Datastrip datastrip = safeManager.getDatastrip();

                for (String band: bands)
                {
                    for (String detector: detectors)
                    {
                        String invFileName = datastrip.getCorrespondingInverseLocGrid(DetectorInfo.getDetectorInfoFromName(detector), BandInfo.getBandInfoFromNameWithB(band), configFile.getInverseLocOutputFolder());
                        Dataset ds = gdal.Open(invFileName);
                        double[] transform = ds.GetGeoTransform();
                        assertEquals(transform[1], -step);
                        assertEquals(transform[5], step);
                    }

                }
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
    public void inverseLocD01B01_fromAsgard()
    {
        double delta = 1e-9;
        try {
            double[][] sensor = inverseLocD01B01(configTmp, "01", "B01", new double[]{0.0, 0.0, 0.0});
            assertEquals(sensor[0][1], Double.NaN);
            assertEquals(sensor[0][0], Double.NaN);

            sensor = inverseLocD01B01(configTmp, "01", "B01", new double[]{-18.919175317847085, 33.79427774463745, 42.539127849734236});
            assertEquals(sensor[0][0], 9.94165389e-01, delta);
            assertEquals(sensor[0][1], 4.68505600e-06, delta);

            sensor = inverseLocD01B01(configTmp, "01", "B01", new double[]{-18.919175317847085, 33.79427774463745});
            assertEquals(sensor[0][0], 9.94165389e-01, delta);
            assertEquals(sensor[0][1], 4.68505600e-06, delta);

            sensor = inverseLocD01B01(configTmp, "01", "B01", new double[]{-18.821137292435186, 33.635377815436044, 42.87227050779195});
            assertEquals(sensor[0][0], 250.50004268066434, delta);
            assertEquals(sensor[0][1], 200.50000152164057, delta);
        } catch (Sen2VMException e) {
            e.printStackTrace();
        }

    }

    public double[][] inverseLocD01B01(String config, String det, String band, double[] ground) throws Sen2VMException
    {
        double[][] sensorCoordinates = {{0., 0.}};
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

            double[][] grounds = {ground};
            sensorCoordinates = simpleLocEngine.computeInverseLoc(sensorList.get("B01/D01"), grounds, "EPSG:4326");
        } catch ( SXGeoException e ) {
            e.printStackTrace();
        }  catch (Sen2VMException e) {
            e.printStackTrace();
        }

        return sensorCoordinates;

    }



    // @Test
    public void geoLocInverse()
    {

        String[] detectors = new String[]{"01","02","03","04","05","06","07","08","09","10","11","12"};
        String[] bands = new String[]{"B01","B02","B03","B04","B05","B06","B07","B08","B8A", "B09","B10","B11","B12"};
        try {
            int step = 6000;
            String nameTest = "inverse_" + Integer.toString(step) + "m";
            String outputDir = Utils.createTestDir(nameTest, "inverse");
            String config = Utils.config(configTmp, outputDir, step, "inverse", false);
            String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyInverseLoc(config, refDir + "/" + nameTest);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void inverse_testGipp()
    {

        boolean checkGipp = false;
        String gipp_2 = "/Sen2vm/sen2vm-core/src/test/resources/tests/input/TDS1/inputs/GIPP/"; // Todo

        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};

        try {
            String nameTest = "inverse_checkGipp_" + checkGipp;
            String outputDir = Utils.createTestDir(nameTest, "inverse");
            String config = Utils.configCheckGipp(configTmp, gipp_2, checkGipp, outputDir);
            String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void geoRefining()
    {
        boolean[] testsRef = new boolean[]{true, false};
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01","B02","B05"};

        for (boolean ref : testsRef) {
            try {
                String nameTest = "inverse_refining_" + ref ;
                String outputDir = Utils.createTestDir(nameTest, "inverse");
                String config = Utils.config(configTmp, outputDir, 6000, "inverse", ref);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyInverseLoc(config, refDir + "/" + nameTest);
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
            String nameTest = "inverse_no_iers" ;
            String outputDir = Utils.createTestDir("inverse_no_iers", "inverse");
            String config = Utils.configSuppIERS(configTmp, outputDir);
            String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyInverseLoc(config, refDir + "/" + nameTest);
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
            String outputDir1 = Utils.createTestDir("inverse_order_1", "inverse");
            String[] detectors_order_1 = new String[]{"01", "02"};
            String[] bands_order_1 = new String[]{"B01", "B02"};
            String config_order_1 = Utils.config(configTmp, outputDir1, 6000, "inverse", false);
            String param_order_1 = Utils.changeParams(paramTmp, detectors_order_1, bands_order_1, outputDir1);
            String[] args_order_1 = {"-c", config_order_1, "-p", param_order_1};
            Sen2VM.main(args_order_1);

            String outputDir2 = Utils.createTestDir("inverse_order_2", "inverse");
            String[] detectors_order_2 = new String[]{"02", "01"};
            String[] bands_order_2 = new String[]{"B02", "B01"};
            String config_order_2 = Utils.config(configTmp, outputDir2, 6000, "inverse", false);
            String param_order_2 = Utils.changeParams(paramTmp, detectors_order_2, bands_order_2, outputDir2);
            String[] args_order_2 = {"-c", config_order_2, "-p", param_order_2};
            Sen2VM.main(args_order_2);

            Utils.verifyInverseLoc(config_order_2, outputDir1);
            Utils.verifyInverseLoc(config_order_1, outputDir2);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void referential_handling()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01","B02"};

        try {
            String nameTest = "referential_handling" ;
            String outputDir = Utils.createTestDir(nameTest, "inverse");
            Double ul_y = 699960.00 ;
            Double ul_x = 3700020.00;
            Double lr_y = 809760.000;
            Double lr_x = 3590220.000;
            String referential = "EPSG:32627";
            String config = Utils.configInverseBB(configTmp, ul_y, ul_x, lr_y, lr_x, referential, outputDir);
            String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyInverseLoc(config, "src/test/resources/tests/output/referential_handling_zero/");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void area_handling()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01","B02"};

        try {
            String nameTest = "area_handling" ;
            String outputDir = Utils.createTestDir(nameTest, "inverse");
            Double ul_y = 199980.000;
            Double ul_x = 3700020.000;
            Double lr_y = 309780.000;
            Double lr_x = 3590220.000;
            String referential = "EPSG:32628";
            String config = Utils.configInverseBB(configTmp, ul_y, ul_x, lr_y, lr_x, referential, outputDir);
            String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyInverseLoc(config, "src/test/resources/tests/output/referential_handling_zero"); // refDir + "/" + nameTest);
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
                String nameTest = "inverse_" + testDem;
                String outputDir = Utils.createTestDir(nameTest, "direct");
                String config = Utils.changeDem(configTmp, "src/test/resources/tests/input/dem_tests/" + testDem, outputDir);
                String param = Utils.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyDirectLoc(config, "src/test/resources/tests/input/ref/inverse_dem_D01_B01/");
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
