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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.Configuration;
import esa.sen2vm.utils.Sen2VMConstants;

import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import esa.sen2vm.input.gipp.GIPPManager;

import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.enums.BandInfo;

import org.sxgeo.engine.SimpleLocEngine;
import org.sxgeo.input.datamodels.RefiningInfo;
import org.sxgeo.input.datamodels.sensor.Sensor;
import org.sxgeo.input.datamodels.sensor.SensorViewingDirection;
import org.sxgeo.input.datamodels.sensor.SpaceCraftModelTransformation;
import org.sxgeo.input.dem.DemManager;
import org.sxgeo.input.dem.SrtmFileManager;
import org.sxgeo.input.dem.GeoidManager;
import org.sxgeo.rugged.RuggedManager;
import org.sxgeo.exception.SXGeoException;
import org.orekit.time.TimeScalesFactory;
import org.orekit.rugged.linesensor.LineSensor;
import org.orekit.rugged.linesensor.LineDatation;

/**
 * Unit test for Sen2VM.
 */

public class Sen2VMCheckPointTest
{
    private static final Logger LOGGER = Logger.getLogger(Sen2VMCheckPointTest.class.getName());

    String configTmpDirect = "src/test/resources/tests/input/TDS1/configuration_TDS1_direct.json";
    String configTmpInverse = "src/test/resources/tests/input/TDS1/configuration_TDS1_inverse.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/ref";

    @Test
    public void geoTimeFirstLine()
    {
        int step = 6000;

        try
        {
            String nameTest = "direct_first_line_" + Integer.toString(step);
            String outputDir = Config.createTestDir(nameTest, "direct");
            String config = Config.config(configTmpDirect, outputDir, step, "direct", false);
            String param = Config.changeParams(paramTmp, new String[]{"01"}, new String[]{"B01"}, outputDir);

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
                    LOGGER.info(sensor.getName());
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
            LOGGER.info("date line 0.5:" + date);

            assertEquals(date, "2020-08-16T12:02:45.812731");
        } catch ( SXGeoException e ) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Sen2VMException e) {
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
    public void geoLocD01B01_fromAsgard()
    {
        double delta = 1e-9;
        try
        {
            double[][] grounds = geoLocD01B01(configTmpDirect, 0.0, 0.0);
            LOGGER.info("pixels = 0.0 0.0 grounds = "+grounds[0][0]+" "+grounds[0][1]+" "+grounds[0][2]);
            assertEquals(grounds[0][0], -18.919024167218094, delta);
            assertEquals(grounds[0][1], 33.79483143151926, delta);
            assertEquals(grounds[0][2], 42.538715533140156, delta);

            grounds = geoLocD01B01(configTmpDirect, 250.5, 700.5);
            LOGGER.info("pixels = 0.0 0.0 grounds = "+grounds[0][0]+" "+grounds[0][1]+" "+grounds[0][2]);
            assertEquals(grounds[0][0], -18.490305707482214, delta);
            assertEquals(grounds[0][1], 33.58277655913304, delta);
            assertEquals(grounds[0][2], 43.448338191393816, delta);
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

    public double[][] geoLocD01B01(String config, double line, double pixel) throws Sen2VMException
    {
        double[][] grounds = {{0., 0.}};
        try
        {
            // Read configuration file
            Configuration configFile = new Configuration(config);

            List<DetectorInfo> detectors = new ArrayList<DetectorInfo>();
            detectors.add(DetectorInfo.getDetectorInfoFromName("01"));

            List<BandInfo> bands = new ArrayList<BandInfo>();
            bands.add(BandInfo.getBandInfoFromNameWithB("B01"));

            // Read datastrip
            DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), !configFile.getDeactivateRefining());

            // Read GIPP
            GIPPManager gippManager = new GIPPManager(configFile.getGippFolder(), bands, dataStripManager, configFile.getGippVersionCheck());

            // Initialize SimpleLocEngine

            // Init demManager
            Boolean isOverlappingTiles = true; // geoid is a single file (not tiles) so set overlap to True by default
            SrtmFileManager demFileManager = new SrtmFileManager(configFile.getDem());
            if(!demFileManager.findRasterFile())
            {
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
            for (DetectorInfo detectorInfo: detectors)
            {
                for (BandInfo bandInfo: bands)
                {
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
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }

        return grounds;
    }

     public static HashMap<String, Sensor> getSensorHashMap(List<DetectorInfo> detectors, List<BandInfo> bands,
         DataStripManager dataStripManager, GIPPManager gippManager) throws Sen2VMException {

        // Save sensors for each focal plane
        HashMap<String, Sensor> sensorList = new HashMap<String, Sensor>();
        for (DetectorInfo detectorInfo: detectors)
        {
            for (BandInfo bandInfo: bands)
            {
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

    @Test
    public void inverseLocD01B01_fromAsgard()
    {
        double delta = 1e-9;
        try
        {
            double[][] sensor = inverseLocD01B01(configTmpInverse, "01", "B01", new double[]{0.0, 0.0, 0.0});
            assertEquals(sensor[0][1], Double.NaN);
            assertEquals(sensor[0][0], Double.NaN);

            sensor = inverseLocD01B01(configTmpInverse, "01", "B01", new double[]{-18.919175317847085, 33.79427774463745, 42.539127849734236});
            assertEquals(sensor[0][0], 9.94165389e-01, delta);
            assertEquals(sensor[0][1], 4.68505600e-06, delta);

            sensor = inverseLocD01B01(configTmpInverse, "01", "B01", new double[]{-18.919175317847085, 33.79427774463745});
            assertEquals(sensor[0][0], 9.94165389e-01, delta);
            assertEquals(sensor[0][1], 4.68505600e-06, delta);

            sensor = inverseLocD01B01(configTmpInverse, "01", "B01", new double[]{-18.821137292435186, 33.635377815436044, 42.87227050779195});
            assertEquals(sensor[0][0], 250.50004268066434, delta);
            assertEquals(sensor[0][1], 200.50000152164057, delta);
            LOGGER.info("OK");
        } catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }

    }

    public double[][] inverseLocD01B01(String config, String det, String band, double[] ground) throws Sen2VMException
    {
        double[][] sensorCoordinates = {{0., 0.}};
        try
        {
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
            if(!demFileManager.findRasterFile())
            {
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
            for (DetectorInfo detectorInfo: detectors)
            {
                for (BandInfo bandInfo: bands)
                {
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
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Sen2VMException e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }  catch (Exception e) {
            LOGGER.warning(e.getMessage());
            e.printStackTrace();
            assert(false);
        }

        return sensorCoordinates;
    }
}