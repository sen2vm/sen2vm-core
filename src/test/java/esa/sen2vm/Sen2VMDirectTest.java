/** Copyright 2024-2025, CS GROUP, https://www.cs-soprasteria.com/
*
* This file is part of the Sen2VM Core project
*     https://gitlab.acri-cwa.fr/opt-mpc/s2_tools/sen2vm/sen2vm-core
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.*/

package esa.sen2vm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

import esa.sen2vm.exception.Sen2VMException;

import org.orekit.data.DataContext;
import org.orekit.data.LazyLoadedDataContext;


/**
 * Unit test for Sen2VM (direct loc).
 */
public class Sen2VMDirectTest
{
    private static final Logger LOGGER = Logger.getLogger(Sen2VMDirectTest.class.getName());

    String configTmpDirectTDS1 = "src/test/resources/tests/input/TDS1/configuration_TDS1_direct.json";
    String configTmpDirectTDS1ShiftRaw = "src/test/resources/tests/input/TDS1/configuration_TDS1_direct_shift_raw.json";
    String configTmpDirectTDS2 = "src/test/resources/tests/input/TDS2-INS-RAW/configuration_TDS2_direct.json";
    String configTmpDirectTDS2NoShift = "src/test/resources/tests/input/TDS2-INS-RAW/configuration_TDS2_direct_no_shift.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/ref";

    @AfterEach
    void resetGlobalState(){
        // Orekit
        DataContext.getDefault()
                   .getDataProvidersManager()
                   .clearProviders();

        DataContext.setDefault(new LazyLoadedDataContext());
    }

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
                String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
                String config = Config.config(configTmpDirectTDS1, outputDir, stepBand10m, "direct", false);
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
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.config(configTmpDirectTDS1, outputDir, stepBand10m, "direct", false);
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
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configAutoGippSelection(configTmpDirectTDS1, GIPP_2, false, outputDir);
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
    public void testAutoSelectTarGipp()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        String GIPP_archive = "src/test/resources/tests/data/archive_GIPP/";
        String GIPP_2 = "src/test/resources/tests/data/test_GIPP/";
        File gippDir= new File(GIPP_2);
        File sourceArchive= new File(GIPP_archive);
        if(Files.exists(gippDir.toPath()))
        {

            Config.deleteDirectory(gippDir);
        }
        gippDir.mkdir();
        try
        {
            Config.copyFolder(sourceArchive,gippDir,true);
            String nameTest = "testDirectLoc";
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configAutoGippSelection(configTmpDirectTDS1, GIPP_2, true, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            LOGGER.info("config: "+config);
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
    public void testNoAutoSelectTarGipp()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        String GIPP_archive = "src/test/resources/tests/data/archive_GIPP/";
        String GIPP_2 = "src/test/resources/tests/data/test_GIPP/";
        File gippDir= new File(GIPP_2);
        File sourceArchive= new File(GIPP_archive);
        if(Files.exists(gippDir.toPath()))
        {
            Config.deleteDirectory(gippDir);
        }
        gippDir.mkdir();
        try
        {
            Config.copyFolder(sourceArchive,gippDir,true);

            File fileToRemove = new File(GIPP_2 + "S2A_OPER_GIP_SPAMOD_MPC__20210419T000024_V20210421T233000_21000101T000000_B00.xml");
            LOGGER.info("File to remove: " + fileToRemove.toString());
            fileToRemove.delete();

            fileToRemove = new File(GIPP_2 + "S2A_OPER_GIP_SPAMOD_MPC__20210419T000024_V20210421T233000_21000101T000000_B00.tar.gz");
            LOGGER.info("File to remove: " + fileToRemove.toString());
            fileToRemove.delete();

            fileToRemove = new File(GIPP_2 + "S2A_OPER_GIP_BLINDP_MPC__20150605T094736_V20150622T000000_21000101T000000_B00/S2A_OPER_GIP_BLINDP_MPC__20150605T094736_V20150622T000000_21000101T000000_B00.DBL");
            LOGGER.info("File to remove: " + fileToRemove.toString());
            fileToRemove.delete();

            String nameTest = "testDirectLoc";
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configAutoGippSelection(configTmpDirectTDS1, GIPP_2, false, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            LOGGER.info("config: "+config);
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
    public void testAutoSelectWithMissingGipp()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        String GIPP_archive = "src/test/resources/tests/data/archive_GIPP/";
        String GIPP_2 = "src/test/resources/tests/data/test_GIPP/";
        File gippDir= new File(GIPP_2);
        File sourceArchive= new File(GIPP_archive);
        if(Files.exists(gippDir.toPath()))
        {

            Config.deleteDirectory(gippDir);
        }
        gippDir.mkdir();

        try
        {
            Config.copyFolder(sourceArchive,gippDir,true);
            // remove a listed GIPP to check a test failure
            File fileToRemove = new File(GIPP_2 + "S2A_OPER_GIP_VIEDIR_SPS__20150731T092207_V20150703T000000_21000101T000000_B01.tar.gz");
            LOGGER.info("File to remove: "+fileToRemove.toString());
            fileToRemove.delete();
            String nameTest = "testDirectLoc";
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configAutoGippSelection(configTmpDirectTDS1, GIPP_2, true, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            LOGGER.info("config: "+config);
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
            LOGGER.warning("Expecting an error.");
            assert(false);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(true);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testAutoSelectWithMissingUntarGipp()
    {
        String[] detectors = new String[]{"01"};
        String[] bands = new String[]{"B01"};
        String GIPP_archive = "src/test/resources/tests/data/archive_GIPP/";
        String GIPP_2 = "src/test/resources/tests/data/test_GIPP/";
        File gippDir= new File(GIPP_2);
        File sourceArchive= new File(GIPP_archive);
        if(Files.exists(gippDir.toPath()))
        {

            Config.deleteDirectory(gippDir);
        }
        gippDir.mkdir();

        try
        {
            Config.copyFolder(sourceArchive,gippDir,true);

            // remove a listed GIPP to check a test failure
            File fileToRemove = new File(GIPP_2 + "S2A_OPER_GIP_SPAMOD_MPC__20210419T000024_V20210421T233000_21000101T000000_B00.xml");
            LOGGER.info("File to remove: "+fileToRemove.toString());
            fileToRemove.delete();

            fileToRemove = new File(GIPP_2 + "S2A_OPER_GIP_SPAMOD_MPC__20210419T000024_V20210421T233000_21000101T000000_B00.tar.gz");
            LOGGER.info("File to remove: "+fileToRemove.toString());
            fileToRemove.delete();

            fileToRemove = new File(GIPP_2 + "S2A_OPER_GIP_SPAMOD_MPC__20220120T000025_V20220125T022000_21000101T000000_B00.tar.gz");
            LOGGER.info("File to remove: "+fileToRemove.toString());
            fileToRemove.delete();


            String nameTest = "testDirectLoc";
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configAutoGippSelection(configTmpDirectTDS1, GIPP_2, true, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            LOGGER.info("config: "+config);
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
            LOGGER.warning("Expecting an error.");
            assert(false);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(true);
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
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configAutoGippSelection(configTmpDirectTDS1, GIPP_2, true, outputDir);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            LOGGER.warning("Expecting an error.");
            assert(false);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            LOGGER.info("An error is expected due to the GIPP");
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
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.config(configTmpDirectTDS1, outputDir, stepBand10m, "direct", false);
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
            String outputDir_ref = Config.createTestDir(Config.TDS.TDS1, nameTest_ref, "direct");
            String iers_ref = "src/test/resources/tests/data/S2__OPER_AUX_UT1UTC_PDMC_20190725T000000_V20190726T000000_20200725T000000.txt";
            String config_ref = Config.configIERS(configTmpDirectTDS1, outputDir_ref, iers_ref);
            String param_ref = Config.changeParams(paramTmp, detectors, bands, outputDir_ref);
            String[] args_ref = {"-c", config_ref, "-p", param_ref};
            Sen2VM.main(args_ref);

            String nameTest = "testDirectIers_test";
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configIERS(configTmpDirectTDS1, outputDir, null);
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
            String outputDir1 = Config.createTestDir(Config.TDS.TDS1, "testDirectParallelisation_1", "direct");
            String[] detectors_order_1 = new String[]{"01", "02"};
            String[] bands_order_1 = new String[]{"B01", "B02"};
            String config_order_1 = Config.config(configTmpDirectTDS1, outputDir1, stepBand10m, "direct", false);
            String param_order_1 = Config.changeParams(paramTmp, detectors_order_1, bands_order_1, outputDir1);
            String[] args_order_1 = {"-c", config_order_1, "-p", param_order_1};
            Sen2VM.main(args_order_1);

            String outputDir2 = Config.createTestDir(Config.TDS.TDS1, "testDirectParallelisation_2", "direct");
            String[] detectors_order_2 = new String[]{"02", "01"};
            String[] bands_order_2 = new String[]{"B02", "B01"};
            String config_order_2 = Config.config(configTmpDirectTDS1, outputDir2, stepBand10m, "direct", false);
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
        String[] detectors = new String[]{"08"};
        String[] bands = new String[]{"B01"};
        String[] testsDem = new String[]{"dem_1", "dem_2", "dem_3", "dem_4", "dem_5", "dem_6"};
        int stepBand10m = 600; // corresponding to 6 kms

        try
        {
            String nameTest_ref = "testDirectDem_ref";
            String outputDir_ref = Config.createTestDir(Config.TDS.TDS1, nameTest_ref, "direct");
            String config_ref = Config.config(configTmpDirectTDS1, outputDir_ref, stepBand10m, "direct", false);
            String params_ref = Config.changeParams(paramTmp, detectors, bands, outputDir_ref);
            String[] args_ref = {"-c", config_ref, "-p", params_ref};
            Sen2VM.main(args_ref);

            for (String testDem : testsDem)
            {
                String nameTest = "testDirectDem_" + testDem;
                String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
                String config = Config.changeDem(configTmpDirectTDS1, "src/test/resources/tests/data/dem_tests/" + testDem, outputDir);
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

    @Test
    public void testDirectLocWithNominalRawShiftIgnored()
    {
        // String[] detectors = new String[]{"01", "02","03","04","05","06","07","08","09","10","11","12"};
        // String[] bands = new String[]{"B01", "B02","B03","B04","B05","B06","B07","B08","B8A", "B09","B10","B11","B12"};
        String[] detectors = new String[]{"06","12"};
        String[] bands = new String[]{"B04", "B12"};
        int stepBand10m = 600; // corresponding to 6 kms

        try
        {
            String nameTest = "testDirectLocWithNominalRawShiftIgnored";
            String outputDir = Config.createTestDir(Config.TDS.TDS1, nameTest, "direct");
            String config = Config.configRawShifts(configTmpDirectTDS1ShiftRaw, outputDir, stepBand10m, "direct", false, false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            // Utils.verifyDirectLoc(config, refDir + "/" + nameTest);
            // Comparison with nominal run
            Utils.verifyDirectLoc(config, refDir + "/" + "testDirectLoc");
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
    public void testDirectGippPrdlocError()
    {
        String[] detectors = new String[]{"05"};
        String[] bands = new String[]{"B01"};
        String GIPP_2 = "src/test/resources/tests/data/GIPP_noPRDLOC/";

        try
        {
            String nameTest = "testDirectGippNoPrdlocError";
            String outputDir = Config.createTestDir(Config.TDS.TDS2, nameTest, "direct");
            String config = Config.configAutoGippSelectionWithRawShift(configTmpDirectTDS2, GIPP_2, true, outputDir, false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            LOGGER.warning("Expecting an error.");
            assert(false);
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            LOGGER.info("An error is expected due to missing PRDLOC GIPP");
            assert(true); //An error is expected due to the GIPP
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectGippPrdlocNoErrorWhenDeactivated()
    {
        String[] detectors = new String[]{"05"};
        String[] bands = new String[]{"B01"};
        String GIPP_2 = "src/test/resources/tests/data/GIPP_noPRDLOC/";

        try
        {
            String nameTest = "testDirectGippNoPrdlocNoError";
            String outputDir = Config.createTestDir(Config.TDS.TDS2, nameTest, "direct");
            String config = Config.configAutoGippSelectionWithRawShift(configTmpDirectTDS2, GIPP_2, true, outputDir, true);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            LOGGER.info("Not in Error as expected.");
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            LOGGER.info("An error is expected due to the GIPP");
            assert(true); //An error is expected due to the GIPP
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testDirectLocRawShifted()
    {
        // ref DATA generated duriectly with Sen2Vm 1.1.4 using Notebook with same list of bands/detectors. In Sen2VM 1.1.4, shift was not corrected yet
        String[] detectors = new String[]{"05","06","11","12"};
        String[] bands = new String[]{"B01","B02","B03","B04","B05","B06", "B07", "B08", "B8A","B09","B10","B11","B12"};
        int stepBand10m = 600; // corresponding to 6 kms

        try
        {
            String nameTest = "testDirectLocRawShiftedZero";
            String outputDir = Config.createTestDir(Config.TDS.TDS2, nameTest, "direct");
            String config = Config.configRawShifts(configTmpDirectTDS2, outputDir, stepBand10m, "direct", false, true);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            Utils.verifyDirectLoc(config, refDir + "/" + nameTest);

            String nameTest2 = "testDirectLocRawShifted";
            String outputDir2 = Config.createTestDir(Config.TDS.TDS2, nameTest2, "direct");
            String config2 = Config.configRawShifts(configTmpDirectTDS2, outputDir2, stepBand10m, "direct", false, false);
            String param2 = Config.changeParams(paramTmp, detectors, bands, outputDir2);
            String[] args2 = {"-c", config2, "-p", param2};
            Sen2VM.main(args2);
            // Utils.verifyDirectLoc(config2, refDir + "/" + nameTest);
            // Comparison with run not shifted
            // Rough conversion of pixel in Lat/Lon at equator
            //   48p * 10m / 110 000m => 0.00436
            //   32p * 20m / 110 000m => 0.00582
            //   18p * 60m / 110 000m => 0.00982
            // We expect roughly a maximum shift of 0.009
            Utils.verifyDirectLoc(config2, refDir + "/" + "testDirectLocRawShiftedZero", 0.0101); 
            // We are expecting a shift larger that 0.004 (We are expecting a failure of the comparison)
            Utils.verifyDirectLoc(config2, refDir + "/" + "testDirectLocRawShiftedZero", 0.004,false);
            LOGGER.info("We are expecting differences, as we are checking if the shift is applyied compared to the original run");
            // Comparison versus "reference" data
            // ref DATA generated by Sen2VM with shift applied, so no real ref, more a non-regression test
            // Orthorectification has yet been verified visually
            Utils.verifyDirectLoc(config2, refDir + "/" + nameTest2);
            
            String nameTest3 = "testDirectLocRawNoShiftInConf";
            String outputDir3 = Config.createTestDir(Config.TDS.TDS2, nameTest3, "direct");
            String config3 = Config.config(configTmpDirectTDS2NoShift, outputDir3, stepBand10m, "direct", false);
            String param3 = Config.changeParams(paramTmp, detectors, bands, outputDir3);
            String[] args3 = {"-c", config3, "-p", param3};
            Sen2VM.main(args3);
            // We expect it to be shifted
            Utils.verifyDirectLoc(config3, refDir + "/" + nameTest2);

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