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
import esa.sen2vm.utils.grids.DirectLocGrid;

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
 * Unit test for DirectLocGridTest.
 */
public class DirectLocGridTest
{

    /**
     * Functional test
     */
    @Test
    public void readConfigurationFile()
    {
        float step = 4.5f ;
        int startLine = 0 ;
        int startPixel = 0 ;
        int sizeLine = 2304 ;
        int sizePixel = 425 ;

        DirectLocGrid dirGrid = new DirectLocGrid(0.5f, 0.5f,
                        step, startPixel, startLine, sizeLine, sizePixel);
        assertEquals(dirGrid.getPixelOffsetGranule(), -1.75);
        assertEquals(dirGrid.getLineOffsetGranule(0), -1.75);
        assertEquals(dirGrid.getLineOffsetGranule(200), -3.75);
        assertEquals(dirGrid.getLineOffsetGranule(210), -0.25);

        ArrayList<Double> pixels = dirGrid.getGridPixels();
        assertEquals(pixels.size(), 95);
        assertEquals(pixels.get(0), -1.75);
        assertEquals(pixels.get(1), pixels.get(0) + step);
        assertEquals(pixels.get(dirGrid.getGridPixels().size() - 1), 421.25);

        ArrayList<Double> lines = dirGrid.getGridLines();
        assertEquals(lines.size(), 513);
        assertEquals(lines.get(0), -1.75);
        assertEquals(lines.get(1), lines.get(0) + step);
        assertEquals(lines.get(dirGrid.getGridLines().size() - 1), 2302.25);


    }


}
