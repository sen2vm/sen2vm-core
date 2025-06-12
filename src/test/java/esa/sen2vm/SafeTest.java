package esa.sen2vm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.Configuration;

import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import esa.sen2vm.input.granule.Granule;
import esa.sen2vm.input.SafeManager;

import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.enums.BandInfo;

/**
 * Unit test for SafeTest
 */
public class SafeTest
{
    private static final Logger LOGGER = Logger.getLogger(SafeTest.class.getName());

    String configTmp = "src/test/resources/tests/input/TDS1/configuration_TDS1_direct.json";

    /**
     * Functional test
     */
    @Test
    public void testGranuleInformation()
    {
        try
        {
            // Read configuration file
            Configuration config = new Configuration(configTmp);
            DataStripManager dataStripManager = new DataStripManager(config.getDatastripFilePath(), config.getIers(), !config.getDeactivateRefining());
            SafeManager safeManager = new SafeManager(config.getL1bProduct(), dataStripManager);

            // Test granule selection
            String granuleName = "S2A_OPER_MSI_L1B_GR_DPRM_20140630T140000_S20200816T120230_D01_N05.00";
            Granule gr = safeManager.getGranuleByName(granuleName);
            assertEquals(gr.getName(), granuleName);
            assertEquals(gr.getDetector(), "D01");

            // Test Bounding Box
            assertEquals(gr.getULpixel(10.0)[0], 2304); // Line
            assertEquals(gr.getULpixel(10.0)[1], 0); // Pixel
            assertEquals(gr.getULpixel(20.0)[0], 1152); // Line
            assertEquals(gr.getULpixel(20.0)[1], 0); // Pixel
            assertEquals(gr.getULpixel(60.0)[0], 384); // Line
            assertEquals(gr.getULpixel(60.0)[1], 0); // Pixel
            assertEquals(gr.getBRpixel(10.0)[0], 4608); // Line
            assertEquals(gr.getBRpixel(10.0)[1], 2552); // Pixel
            assertEquals(gr.getBRpixel(20.0)[0], 2304); // Line
            assertEquals(gr.getBRpixel(20.0)[1], 1276); // Pixel
            assertEquals(gr.getBRpixel(60.0)[0], 768); // Line
            assertEquals(gr.getBRpixel(60.0)[1], 425); // Pixel

            // Test Geo File Name computation / band
            Path p = Paths.get(gr.getCorrespondingGeoFileName(BandInfo.BAND_1));
            String fileName = p.getFileName().toString();
            assertEquals(fileName, "S2A_OPER_GEO_L1B_GR_DPRM_20140630T140000_S20200816T120230_D01_B01.tif");
        }
        catch (Sen2VMException e)
        {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }

    @Test
    public void testSafeInformation()
    {
        try
        {
            // Read configuration file
            Configuration config = new Configuration(configTmp);
            DataStripManager dataStripManager = new DataStripManager(config.getDatastripFilePath(), config.getIers(), !config.getDeactivateRefining());
            SafeManager safeManager = new SafeManager(config.getL1bProduct(), dataStripManager);

            // Test granules number
            assertEquals(safeManager.getGranules().size(), 72);

            LOGGER.info("   ");
            int[] BBox = safeManager.getFullSize(dataStripManager, BandInfo.BAND_1, DetectorInfo.DETECTOR_1);
            assertEquals(BBox[0], 0); // startLine
            assertEquals(BBox[1], 0); // startPixel
            assertEquals(BBox[2], 2304); // sizeLine
            assertEquals(BBox[3], 425); // sizePixel

            BBox = safeManager.getFullSize(dataStripManager, BandInfo.BAND_2, DetectorInfo.DETECTOR_3);
            assertEquals(BBox[0], 0); // startLine
            assertEquals(BBox[1], 0); // startPixel
            assertEquals(BBox[2], 13824); // sizeLine
            assertEquals(BBox[3], 2552); // sizePixel

            assertEquals(safeManager.getGranulesToCompute(DetectorInfo.DETECTOR_3, BandInfo.BAND_2).size(), 6);
        }  catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }
    }
}
