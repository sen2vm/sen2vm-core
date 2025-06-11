package esa.sen2vm;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.enums.BandInfo;

import esa.sen2vm.input.Configuration;
import esa.sen2vm.input.SafeManager;
import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

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

    private static final double THRESHOLD_INV_HIGH = 6e-1; // TODO: investiguate why so high
    private static final double THRESHOLD_INV_LOW = 1e-6;

    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(Sen2VMInverseTest.class.getName());

    @Test
    public void testStepInverseLoc()
    {
        String[] detectors = new String[]{"05"};
        String[] bands = new String[]{"B01", "B02", "B05"};
        double[] testsStepBand10m = new double[]{3000, 6000};

        for (double stepBand10m : testsStepBand10m)
        {
            try
            {
                String nameTest = "testStepInverseLoc_" +  stepBand10m;
                String outputDir = Config.createTestDir(nameTest, "inverse");
                String config = Config.config(configTmpInverse, outputDir, stepBand10m, "inverse", false);
                String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);

                Configuration configFile = new Configuration(config);
                DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());
                SafeManager safeManager = new SafeManager(configFile.getL1bProduct(), dataStripManager);
                Datastrip datastrip = safeManager.getDatastrip();

                for (String band: bands)
                {
                    double res = BandInfo.getBandInfoFromNameWithB(band).getPixelHeight();
                    for (String detector: detectors)
                    {
                        String invFileName = datastrip.getCorrespondingInverseLocGrid(DetectorInfo.getDetectorInfoFromName(detector), BandInfo.getBandInfoFromNameWithB(band), configFile.getInverseLocOutputFolder());
                        LOGGER.info(invFileName);
                        Dataset ds = gdal.Open(invFileName);
                        double[] transform = ds.GetGeoTransform();
                        assertEquals(transform[1] * (res / 10), stepBand10m);
                        assertEquals(transform[5] * (res / 10), -stepBand10m);
                    }
                }
            } catch (Sen2VMException e) {
                LOGGER.warning(e.getMessage());
                e.printStackTrace();
                assert(false);
            } catch (ParseException e) {
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
    public void testInverseLoc()
    {
        String[] detectors = new String[]{"01", "02","03","04","05","06","07","08","09","10","11","12"};
        String[] bands = new String[]{"B01", "B02","B03","B04","B05","B06","B07","B08","B8A", "B09","B10","B11","B12"};
        int stepBand10m = 6000;
        try
        {
            String nameTest = "testInverseLoc";
            String outputDir = Config.createTestDir(nameTest, "inverse");
            String config = Config.config(configTmpInverse, outputDir, stepBand10m, "inverse", false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            LOGGER.warning("Threshold released at: " + THRESHOLD_INV_HIGH); // TODO
            Utils.verifyInverseLoc(config, refDir + "/" + nameTest, THRESHOLD_INV_HIGH);
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
            Utils.verifyInverseLoc(config, refDir + "/" + nameTest, THRESHOLD_INV_HIGH);
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
    public void testInverseNoRefining()
    {
        boolean[] testsRef = new boolean[]{true, false};
        String[] detectors = new String[]{"06"};
        String[] bands = new String[]{"B01", "B02", "B05"};
        int stepBand10m = 6000;

        try
        {
            String nameTest = "testInverseNoRefining";
            String outputDir = Config.createTestDir(nameTest, "inverse");
            String config = Config.config(configTmpInverse, outputDir, stepBand10m, "inverse", false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            LOGGER.warning("Threshold released at: " + THRESHOLD_INV_HIGH); // TODO
            Utils.verifyInverseLoc(config, refDir + "/" + nameTest, THRESHOLD_INV_HIGH);
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

            Utils.verifyInverseLoc(config, outputDir_ref, THRESHOLD_INV_LOW);
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
    public void testInverseParallelisation()
    {
        int stepBand10m = 6000;
        try
        {
            String outputDir1 = Config.createTestDir("testInverseParallelisation_1", "inverse");
            String[] detectors_order_1 = new String[]{"05", "06"};
            String[] bands_order_1 = new String[]{"B01", "B02"};
            String config_order_1 = Config.config(configTmpInverse, outputDir1, stepBand10m, "inverse", false);
            String param_order_1 = Config.changeParams(paramTmp, detectors_order_1, bands_order_1, outputDir1);
            String[] args_order_1 = {"-c", config_order_1, "-p", param_order_1};
            Sen2VM.main(args_order_1);

            String outputDir2 = Config.createTestDir("testInverseParallelisation_2", "inverse");
            String[] detectors_order_2 = new String[]{"06", "05"};
            String[] bands_order_2 = new String[]{"B02", "B01"};
            String config_order_2 = Config.config(configTmpInverse, outputDir2, stepBand10m, "inverse", false);
            String param_order_2 = Config.changeParams(paramTmp, detectors_order_2, bands_order_2, outputDir2);
            String[] args_order_2 = {"-c", config_order_2, "-p", param_order_2};
            Sen2VM.main(args_order_2);

            Utils.verifyInverseLoc(config_order_2, outputDir1, THRESHOLD_INV_LOW);
            Utils.verifyInverseLoc(config_order_1, outputDir2, THRESHOLD_INV_LOW);
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
    public void testInverseReferentialArea()
    {
        String[] detectors = new String[]{"02"};
        String[] bands = new String[]{"B02"};

        try
        {
            String nameTest = "testInverseReferentialArea";
            String outputDir = Config.createTestDir(nameTest, "inverse");

            // T27SYT
            double ul_x = 699960.0f;
            double ul_y = 3800040.0f;
            double lr_x = 809760.0f;
            double lr_y = 3690240.0f;
            String referential = "EPSG:32627";

            String config = Config.configInverseBB(configTmpInverse, ul_y, ul_x, lr_y, lr_x, referential, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyInverseLoc(config, refDir + "/" + nameTest, THRESHOLD_INV_LOW);
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
    public void testInverseAreaHandling()
    {
        String[] detectors = new String[]{"05"};
        String[] bands = new String[]{"B02"};

        try
        {
            String nameTest = "testInverseAreaHandling";
            String outputDir = Config.createTestDir(nameTest, "inverse");

            // T28SBA
            double ul_x = 199980.0f;
            double ul_y = 3600000.0f;
            double lr_x = 309780.0f;
            double lr_y = 3490200.0f;
            String referential = "EPSG:32628";

            String config = Config.configInverseBB(configTmpInverse, ul_y, ul_x, lr_y, lr_x, referential, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyInverseLoc(config, refDir + "/" + nameTest, THRESHOLD_INV_LOW);
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
    public void testInverseDem()
    {
        String[] detectors = new String[]{"06"};
        String[] bands = new String[]{"B01", "B02"};
        String[] testsDem = new String[]{"dem_1", "dem_2", "dem_3", "dem_4"};
        int stepBand10m = 6000;

        try
        {
            String nameTest_ref = "testInverseDem_ref";
            String outputDir_ref = Config.createTestDir(nameTest_ref, "inverse");
            String config_ref = Config.config(configTmpInverse, outputDir_ref, stepBand10m, "inverse", false);
            String params_ref = Config.changeParams(paramTmp, detectors, bands, outputDir_ref);
            String[] args_ref = {"-c", config_ref, "-p", params_ref};
            Sen2VM.main(args_ref);

            for (String testDem : testsDem) {
                String nameTest = "testInverseDem_" + testDem;
                String outputDir = Config.createTestDir(nameTest, "inverse");
                String config = Config.changeDem(configTmpInverse, "src/test/resources/tests/data/dem_tests/" + testDem, outputDir);
                String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
                String[] args = {"-c", config, "-p", param};
                Sen2VM.main(args);
                Utils.verifyInverseLoc(config, outputDir_ref, THRESHOLD_INV_LOW);
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

    @Test
    public void testInverseLatLonAreaBand10m()
	{
        String[] detectors = new String[]{"07", "08", "09", "10"};
		String[] bands = new String[]{"B02"};
        double unitLatLon = 9.00901E-5;
        double stepBand10m = 6000 ;

        double ul_x = 199980.0;
		double ul_y = 3700020.0;
		double lr_x = 309780.0;
		double lr_y = 3590220.0;
		String referential = "EPSG:32628";

		try
		{
			String nameTest = "testInverseLatLonArea";
			String outputDir = Config.createTestDir(nameTest, "inverse");

			// Init source/target SpatialReference and transformation
			SpatialReference sourceSRS = new SpatialReference();
			sourceSRS.ImportFromEPSG(32628);
			SpatialReference targetSRS = new SpatialReference();
			targetSRS.ImportFromEPSG(4326);
			CoordinateTransformation transformer = new CoordinateTransformation(sourceSRS, targetSRS);

			double[] ul = transformer.TransformPoint(ul_x, ul_y);
			double[] lr = transformer.TransformPoint(lr_x, lr_y);

			String config = Config.configInverseBBwithStepBand10m(configTmpInverse,
			    ul[0], ul[1], lr[0], lr[1], unitLatLon * stepBand10m / 10, "EPSG:4326", outputDir);

			String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
			String[] args = {"-c", config, "-p", param};
			Sen2VM.main(args);
			Utils.verifyInverseLoc(config, refDir + "/" + nameTest, THRESHOLD_INV_LOW);
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