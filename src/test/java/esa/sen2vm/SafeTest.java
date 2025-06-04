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
import java.util.logging.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.io.IOException;
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


    // @Test
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

    // @Test
    public void testRunAndxistingFile()
    {
        try
        {
            // Before Test, create environnement
            String paramTmp = "src/test/resources/params_all.json";
            String[] detectors = new String[]{"10"};
            String[] bands = new String[]{"B01", "B02"};
            String configFile = "";
            String paramFile = "";
            String outputDir = "";

            outputDir = Config.createTestDir("run_D10", "direct");
            configFile = Config.config(configTmp, outputDir, 6000, "direct", false);
            paramFile = Config.changeParams(paramTmp, detectors, bands, outputDir);

            // Test if no vrt and geogrid found
            LOGGER.info(configFile);

            Configuration config = new Configuration(configFile);
            DataStripManager dataStripManager = new DataStripManager(config.getDatastripFilePath(), config.getIers(), !config.getDeactivateRefining());
            SafeManager safeManager = new SafeManager(config.getL1bProduct(), dataStripManager);
            Datastrip datastrip = safeManager.getDatastrip();

            ArrayList<Granule> listGranules = safeManager.getGranules();
            for(int g = 0; g < listGranules.size(); g++)
            {
                for (BandInfo bandInfo: BandInfo.getAllBandInfo())
                {
                    assertEquals(listGranules.get(g).getGrids()[bandInfo.getIndex()], null);
                }
            }

            File[][] vrt = datastrip.getVRTs();
            for(int d = 0; d < vrt.length; d++)
            {
                for(int b = 0; b < vrt[d].length; b++)
                {
                    assertEquals(vrt[d][b], null);
                }
            }
            LOGGER.info("   ");

            String[] args = {"-c", configFile, "-p", paramFile};
            Sen2VM.main(args);

            config = new Configuration(configFile);
            dataStripManager = new DataStripManager(config.getDatastripFilePath(), config.getIers(), !config.getDeactivateRefining());
            safeManager = new SafeManager(config.getL1bProduct(), dataStripManager);
            datastrip = safeManager.getDatastrip();
            listGranules = safeManager.getGranules();
            int nbPresent = 0;
            for(int g = 0; g < listGranules.size(); g++)
            {
                for (BandInfo bandInfo: BandInfo.getAllBandInfo())
                {
                    if (listGranules.get(g).getGrids()[bandInfo.getIndex()] != null)
                    {
                        nbPresent++;
                    }
                }
            }
            assertEquals(nbPresent, 12);

            nbPresent = 0;
            vrt = datastrip.getVRTs();
            for(int d = 0; d < vrt.length; d++)
            {
                for(int b = 0; b < vrt[d].length; b++)
                {
                    if(vrt[d][b] != null)
                    {
                        nbPresent++;
                    }
                }
            }
            assertEquals(nbPresent, 2);
            String ok = "false";
            // configChangeOverwrite(String filePath, Boolean overwrite) throws FileNotFoundException,

            String[] args2 = {"-c", configFile, "-p", paramFile};
            Sen2VM.main(args2);
            ok = "true";
            LOGGER.info(ok);

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

}
