package esa.sen2vm;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import esa.sen2vm.exception.Sen2VMException;

/**
 * Unit test for Sen2VM (direct loc).
 */
public class Sen2VMDirectTest
{
    private static final Logger LOGGER = Logger.getLogger(Sen2VMDirectTest.class.getName());

    String configTmpDirect = "src/test/resources/tests/input/TDS1/configuration_TDS1_direct.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/ref";

    @Test
    public void testStepDirectLoc()
    {
        String[] detectors = new String[]{"02"};
        String[] bands = new String[]{"B01", "B02", "B05"};
        int[] testsStepBand10m = new int[]{300, 600}; // corresponding to 3 and 6 kms


        for (int stepBand10m : testsStepBand10m)
        {
            try
            {
                String nameTest = "testStepDirectLoc_" +  Integer.toString(stepBand10m);
                String outputDir = Config.createTestDir(nameTest, "direct");
                String config = Config.config(configTmpDirect, outputDir, stepBand10m, "direct", false);
                String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyStepDirectLoc(config, stepBand10m);
            } catch (Sen2VMException e) {
                LOGGER.warning(e.getMessage());
                e.printStackTrace();
                assert(false);
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                e.printStackTrace();
                assert(false);
            }
        }
    }

    @Test
    public void testDirectLoc()
    {
        String[] detectors = new String[]{"01", "02","03","04","05","06","07","08","09","10","11","12"};
        String[] bands = new String[]{"B01", "B02","B03","B04","B05","B06","B07","B08","B8A", "B09","B10","B11","B12"};
        int stepBand10m = 600; // corresponding to 6 kms

        try
        {
            String nameTest = "testDirectLoc";
            String outputDir = Config.createTestDir(nameTest, "direct");
            String config = Config.config(configTmpDirect, outputDir, stepBand10m, "direct", false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectGipp()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        String GIPP_2 = "src/test/resources/tests/data/GIPP/";

        try
        {
            String nameTest = "testDirectGipp";
            String outputDir = Config.createTestDir(nameTest, "direct");
            String config = Config.configCheckGipp(configTmpDirect, GIPP_2, false, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectGippError()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        String GIPP_2 = "src/test/resources/tests/data/GIPP/";

        try
        {
            String nameTest = "testDirectGipp";
            String outputDir = Config.createTestDir(nameTest, "direct");
            String config = Config.configCheckGipp(configTmpDirect, GIPP_2, true, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(true); //An error is expected due to the GIPP
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectNoRefining()
    {

        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B02"};
        int stepBand10m = 600; // corresponding to 6 kms

        try
        {
            String nameTest = "testDirectNoRefining";
            String outputDir = Config.createTestDir(nameTest, "direct");
            String config = Config.config(configTmpDirect, outputDir, stepBand10m, "direct", false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectIers()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B02"};

        try
        {
            String nameTest_ref = "testDirectIers_ref";
            String outputDir_ref = Config.createTestDir(nameTest_ref, "direct");
            String iers_ref = "src/test/resources/tests/data/S2__OPER_AUX_UT1UTC_PDMC_20190725T000000_V20190726T000000_20200725T000000.txt";
            String config_ref = Config.configIERS(configTmpDirect, outputDir_ref, iers_ref);
            String param_ref = Config.changeParams(paramTmp, detectors, bands, outputDir_ref);
            String[] args_ref = {"-c", config_ref, "-p", param_ref};
            Sen2VM.main(args_ref);

            String nameTest = "testDirectIers_test";
            String outputDir = Config.createTestDir(nameTest, "direct");
            String config = Config.configIERS(configTmpDirect, outputDir, null);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);

            Utils.verifyDirectLoc(config, outputDir_ref);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectParallelisation()
    {
        int stepBand10m = 600; // corresponding to 6 kms

        try
        {
            String outputDir1 = Config.createTestDir("testDirectParallelisation_1", "direct");
            String[] detectors_order_1 = new String[]{"07", "10"};
            String[] bands_order_1 = new String[]{"B01", "B04"};
            String config_order_1 = Config.config(configTmpDirect, outputDir1, stepBand10m, "direct", false);
            String param_order_1 = Config.changeParams(paramTmp, detectors_order_1, bands_order_1, outputDir1);
            String[] args_order_1 = {"-c", config_order_1, "-p", param_order_1};
            Sen2VM.main(args_order_1);

            String outputDir2 = Config.createTestDir("testDirectParallelisation_2", "direct");
            String[] detectors_order_2 = new String[]{"10", "07"};
            String[] bands_order_2 = new String[]{"B04", "B01"};
            String config_order_2 = Config.config(configTmpDirect, outputDir2, stepBand10m, "direct", false);
            String param_order_2 = Config.changeParams(paramTmp, detectors_order_2, bands_order_2, outputDir2);
            String[] args_order_2 = {"-c", config_order_2, "-p", param_order_2};
            Sen2VM.main(args_order_2);

            Utils.verifyDirectLoc(config_order_2, outputDir1);
            Utils.verifyDirectLoc(config_order_1, outputDir2);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectDem()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        String[] testsDem = new String[]{"dem_1", "dem_2", "dem_3", "dem_4"};
        int stepBand10m = 600; // corresponding to 6 kms

        try
        {
            String nameTest_ref = "testDirectDem_ref";
            String outputDir_ref = Config.createTestDir(nameTest_ref, "direct");
            String config_ref = Config.config(configTmpDirect, outputDir_ref, stepBand10m, "direct", false);
            String params_ref = Config.changeParams(paramTmp, detectors, bands, outputDir_ref);
            String[] args_ref = {"-c", config_ref, "-p", params_ref};
            Sen2VM.main(args_ref);

            for (String testDem : testsDem)
            {
                String nameTest = "testDirectDem_" + testDem;
                String outputDir = Config.createTestDir(nameTest, "direct");
                String config = Config.changeDem(configTmpDirect, "src/test/resources/tests/data/dem_tests/" + testDem, outputDir);
                String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyDirectLoc(config, outputDir_ref);
            }
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }
}
