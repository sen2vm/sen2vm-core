package esa.sen2vm;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.enums.BandInfo;

import esa.sen2vm.input.Configuration;
import esa.sen2vm.input.SafeManager;
import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;


import java.io.IOException;
import org.json.simple.parser.ParseException;
import esa.sen2vm.exception.Sen2VMException;


import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;


/**
 * Unit test for Sen2VM (inverse loc).
 */

public class Sen2VMInverseTest
{

    String configTmpInverse = "src/test/resources/tests/input/TDS1/configuration_TDS1_inverse.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/ref";

    @Test
    public void testStepInverseLoc()
    {

        String[] detectors = new String[]{"05"};
        String[] bands = new String[]{"B01", "B02", "B05"};
        int[] testsStep = new int[]{3000, 6000};


        for (int step : testsStep)
        {
            try
            {
                String nameTest = "testStepInverseLoc_" +  Integer.toString(step);
                String outputDir = Config.createTestDir(nameTest, "inverse");
                String config = Config.config(configTmpInverse, outputDir, step, "inverse", false);
                String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
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
                        System.out.println(invFileName);
                        Dataset ds = gdal.Open(invFileName);
                        double[] transform = ds.GetGeoTransform();

                        assertEquals(transform[1], step);
                        assertEquals(transform[5], -step);
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

    @Test
    public void testInverseLoc()
    {

        String[] detectors = new String[]{"01", "02","03","04","05","06","07","08","09","10","11","12"};
        String[] bands = new String[]{"B01", "B02","B03","B04","B05","B06","B07","B08","B8A", "B09","B10","B11","B12"};
        int step = 6000;
        try
        {
            String nameTest = "testInverseLoc";
            String outputDir = Config.createTestDir(nameTest, "inverse");
            String config = Config.config(configTmpInverse, outputDir, step, "inverse", false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
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

    @Test
    public void testInverseGipp()
    {
        String[] detectors = new String[]{"06"};
        String[] bands = new String[]{"B02"};
        String GIPP_2 = "src/test/resources/tests/data/GIPP/";

       try
       {
            String nameTest = "testInverseGipp";
            String outputDir = Config.createTestDir(nameTest, "inverse");
            String config = Config.configCheckGipp(configTmpInverse, GIPP_2, false, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
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

    @Test
    public void testInverseNoRefining()
    {
        boolean[] testsRef = new boolean[]{true, false};
        String[] detectors = new String[]{"06"};
        String[] bands = new String[]{"B01", "B02", "B05"};

        try
        {
            String nameTest = "testInverseNoRefining";
            String outputDir = Config.createTestDir(nameTest, "inverse");
            String config = Config.config(configTmpInverse, outputDir, 6000, "inverse", false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
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

    @Test
    public void testInverseIers()
    {

        String[] detectors = new String[]{"06"};
        String[] bands = new String[]{"B02"};

        try
        {
            String nameTest_ref = "testInverseIers_ref";
            String outputDir_ref = Config.createTestDir(nameTest_ref, "inverse");
            String iers_ref = "src/test/resources/tests/data/S2__OPER_AUX_UT1UTC_PDMC_20190725T000000_V20190726T000000_20200725T000000.txt";
            String config_ref = Config.configIERS(configTmpInverse, outputDir_ref, iers_ref);
            String param_ref = Config.changeParams(paramTmp, detectors, bands, outputDir_ref);
            String[] args_ref = {"-c", config_ref, "-p", param_ref};
            Sen2VM.main(args_ref);

            String nameTest = "testInverseIers_test";
            String outputDir = Config.createTestDir(nameTest, "inverse");
            String config = Config.configIERS(configTmpInverse, outputDir, null);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);

            Utils.verifyDirectLoc(config, outputDir_ref);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInverseParallelisation()
    {
        try
        {
            String outputDir1 = Config.createTestDir("testInverseParallelisation_1", "inverse");
            String[] detectors_order_1 = new String[]{"05", "06"};
            String[] bands_order_1 = new String[]{"B01", "B02"};
            String config_order_1 = Config.config(configTmpInverse, outputDir1, 6000, "inverse", false);
            String param_order_1 = Config.changeParams(paramTmp, detectors_order_1, bands_order_1, outputDir1);
            String[] args_order_1 = {"-c", config_order_1, "-p", param_order_1};
            Sen2VM.main(args_order_1);

            String outputDir2 = Config.createTestDir("testInverseParallelisation_2", "inverse");
            String[] detectors_order_2 = new String[]{"06", "05"};
            String[] bands_order_2 = new String[]{"B02", "B01"};
            String config_order_2 = Config.config(configTmpInverse, outputDir2, 6000, "inverse", false);
            String param_order_2 = Config.changeParams(paramTmp, detectors_order_2, bands_order_2, outputDir2);
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
    public void testInverseReferentialArea()
    {
        String[] detectors = new String[]{"02"};
        String[] bands = new String[]{"B02"};


        try
        {
            String nameTest = "testInverseReferentialArea";
            String outputDir = Config.createTestDir(nameTest, "inverse");

            // T27SYT
            Double ul_x = 699960.0;
            Double ul_y=  3800040.0;
            Double lr_x= 809760.0;
            Double lr_y= 3690240.0;
            String referential = "EPSG:32627";

            String config = Config.configInverseBB(configTmpInverse, ul_y, ul_x, lr_y, lr_x, referential, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
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

    @Test
    public void testInverseAreaHandling()
    {
        String[] detectors = new String[]{"05"};
        String[] bands = new String[]{"B02"};

        try
        {
            String nameTest = "testInverseAreaHandling";
            String outputDir = Config.createTestDir(nameTest, "inverse");

            // T28SBA
            Double ul_x = 199980.0;
            Double ul_y = 3600000.0;
            Double lr_x = 309780.0;
            Double lr_y = 3490200.0;
            String referential = "EPSG:32628";

            String config = Config.configInverseBB(configTmpInverse, ul_y, ul_x, lr_y, lr_x, referential, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
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

    @Test
    public void testInverseDem()
    {
        String[] detectors = new String[]{"06"};
        String[] bands = new String[]{"B01", "B02"};
        String[] testsDem = new String[]{"dem_1", "dem_2", "dem_3", "dem_4"};

        try
        {
            String nameTest_ref = "testDirectDem_ref";
            String outputDir_ref = Config.createTestDir(nameTest_ref, "inverse");
            String config_ref = Config.config(configTmpInverse, outputDir_ref, 6000, "direct", false);
            String params_ref = Config.changeParams(paramTmp, detectors, bands, outputDir_ref);
            String[] args_ref = {"-c", config_ref, "-p", params_ref};
            Sen2VM.main(args_ref);

            for (String testDem : testsDem) {
                String nameTest = "testDirectDem_" + testDem;
                String outputDir = Config.createTestDir(nameTest, "inverse");
                String config = Config.changeDem(configTmpInverse, "src/test/resources/tests/input/dem_tests/" + testDem, outputDir);
                String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyDirectLoc(config, outputDir_ref);
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
