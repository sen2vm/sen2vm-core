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

import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import java.util.Arrays;

import java.util.Vector;
import java.util.HashMap;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.File;
import java.nio.file.Files;

import org.sxgeo.engine.SimpleLocEngine;
import org.sxgeo.input.datamodels.sensor.Sensor;
import org.sxgeo.input.datamodels.sensor.SensorViewingDirection;
import org.sxgeo.input.datamodels.sensor.SpaceCraftModelTransformation;
import org.sxgeo.input.dem.DemManager;
import org.sxgeo.input.dem.GeoidManager;
import org.sxgeo.rugged.RuggedManager;
import org.orekit.rugged.linesensor.LineDatation;
import org.sxgeo.exception.SXGeoException;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.Configuration;
import esa.sen2vm.input.DEM.GenericDemFileManager;
import esa.sen2vm.input.OptionManager;
import esa.sen2vm.input.Params;
import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import esa.sen2vm.input.granule.Granule;
import esa.sen2vm.input.gipp.GIPPManager;
import esa.sen2vm.input.SafeManager;
import esa.sen2vm.output.OutputFileManager;
import esa.sen2vm.utils.grids.DirectLocGrid;
import esa.sen2vm.utils.grids.InverseLocGrid;
import esa.sen2vm.utils.Sen2VMConstants;

/**
 * Main class
 */
public class Sen2VM
{
    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(Sen2VM.class.getName());

    /**
     * Main process
     * @param args first arg: input json file. second param (optional): parameter json file
     * @throws Sen2VMException
     */

    public static void main( String[] args ) throws Sen2VMException, Exception
    {
        // Get the logger configuration
        InputStream logProperties = Thread.currentThread().getContextClassLoader().getResourceAsStream("log.properties");
        try
        {
            LogManager.getLogManager().readConfiguration(logProperties);
        }
        catch (SecurityException | IOException e)
        {
            throw new Sen2VMException(e);
        }

        // Run core
        LOGGER.info("Start Sen2VM");

        // Read the command line arguments
        // ===============================
        CommandLine commandLine = OptionManager.readCommandLineArguments(args);
        try
        {
            // Init all detectors and bands by default
            List<DetectorInfo> detectors = DetectorInfo.getAllDetectorInfo();
            List<BandInfo> bands = BandInfo.getAllBandInfo();

            Configuration config;
            Params params = null;

            // Initialize the configuration and the parameters
            // ------------------
            // Check whether the initialization shall be done from file or from command line arguments
            if (OptionManager.areFiles())
            {
                // Get the configuration file
                String configFilepath = commandLine.getOptionValue(OptionManager.OPT_CONFIG_SHORT);
                // Read configuration file
                config = new Configuration(configFilepath);

                // Verify if the parameter file is available in command line
                if (commandLine.hasOption(OptionManager.OPT_PARAM_SHORT))
                {
                    // Get the parameters file
                    String sensorParamsFile = commandLine.getOptionValue(OptionManager.OPT_PARAM_SHORT);

                    // Read parameter file (optional)
                    if (sensorParamsFile != null)
                    {
                        params = new Params(sensorParamsFile);
                    }
                }
            }
            else
            { // not areFiles
                // Initialize the configuration with the command line
                config = new Configuration(commandLine);

                // Initialize the parameters with the command line
                params = new Params(commandLine);

            } // end areFiles

            //Read the parameters to process
            if (params != null && params.getDetectorsList().size() > 0)
            {
                detectors = params.getDetectorsList();
            }
            else
            {
                // Information missing, by default we prosse all
                detectors = DetectorInfo.getAllDetectorInfo();
            }

            if (params != null && params.getBandsList().size() > 0)
            {
                bands = params.getBandsList();
            }
            else
            {
                // Information missing, by default we prosse all
                bands = BandInfo.getAllBandInfo();
            }

            LOGGER.info("Detectors list: " + detectors);
            LOGGER.info("Bands list: " + bands);

            // Read datastrip
            DataStripManager dataStripManager = new DataStripManager(config.getDatastripFilePath(), config.getIers(), !config.getDeactivateRefining());

            // Read GIPP
            GIPPManager gippManager = new GIPPManager(config.getGippFolder(), bands, dataStripManager, config.getGippVersionCheck());

            // Initialize SimpleLocEngine
            // ==========================
            // Init demManager
            // ---------------
            Boolean isOverlappingTiles = true; // geoid is a single file (not tiles) so set overlap to True by default

            //Using SXGEO FileManager
            /*SrtmFileManager demFileManager = new SrtmFileManager(config.getDem());
            if (!demFileManager.findRasterFile())
            {
                throw new Sen2VMException("Error when checking for DEM file");
            }*/

            //Using Sen2VM FileManager
            GenericDemFileManager demFileManager = new GenericDemFileManager(config.getDem());

            GeoidManager geoidManager = new GeoidManager(config.getGeoid(), isOverlappingTiles);
            DemManager demManager = new DemManager(
                demFileManager,
                geoidManager,
                isOverlappingTiles);

            // Build sensors list
            // ------------------
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
            // --------------------
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
            // --------------------
            SimpleLocEngine simpleLocEngine = new SimpleLocEngine(
                dataStripManager.getDataSensingInfos(),
                ruggedManager,
                demManager
            );
            // Safe Manager
            SafeManager safeManager = new SafeManager( config.getL1bProduct(), dataStripManager, config.getGridsOverwriting());
            Datastrip datastrip = safeManager.getDatastrip();
            //ds.checkNoVRT(detectors, bands);

            // GIPP
            double georefConventionOffsetPixel = +0.5f;
            double georefConventionOffsetLine = +0.5f;

            OutputFileManager outputFileManager = new OutputFileManager();

            if (config.getOperation().equals(Sen2VMConstants.INVERSE))
            {
                LOGGER.info("EGSG read from configuration file (for inverse location): " + config.getInverseLocReferential());
            }

            LOGGER.info("");
            LOGGER.info("Starting grids generation");

            // Test if no grids exists already
            if (config.getOperation().equals(Sen2VMConstants.DIRECT))
            {
                safeManager.testifDirectGridsToComputeAlreadyExist(detectors, bands);
            }
            else
            {
                safeManager.testifInverseGridsToComputeAlreadyExist(detectors, bands, config.getInverseLocOutputFolder());
            }

            for (BandInfo bandInfo: bands)
            {
                LOGGER.info("");
                LOGGER.info("###############");
                LOGGER.info("### BAND " + bandInfo.getName() + " ###");
                LOGGER.info("###############");

                double res = bandInfo.getPixelHeight();
                double step = config.getStepFromBandInfo(bandInfo);

                LOGGER.info("Grid resolution: " + String.valueOf(config.getStepFromBandInfo(bandInfo)));
                LOGGER.info("Band resolution: " + String.valueOf(res));

                for (DetectorInfo detectorInfo: detectors)
                {
                    LOGGER.info("");
                    LOGGER.info("### DET " + detectorInfo.getName() + " (BAND " + bandInfo.getName() + ") ###");

                    // Direct Loc case
                    // --------------------
                    if (config.getOperation().equals(Sen2VMConstants.DIRECT))
                    {
                        int[] bbox = safeManager.getFullSize(dataStripManager, bandInfo, detectorInfo);
                        int startLine = bbox[0];
                        int startPixel = bbox[1];
                        int sizeLine = bbox[2];
                        int sizePixel = bbox[3];

                        // Load Granule Info
                        ArrayList<Granule> granulesToCompute = safeManager.getGranulesToCompute(detectorInfo, bandInfo);

                        // Get Full Sensor Grid
                        DirectLocGrid dirGrid = new DirectLocGrid(georefConventionOffsetLine, georefConventionOffsetPixel,
                            step, startLine, startPixel, sizeLine, sizePixel);
                        double[][] sensorGridForDirectLoc = dirGrid.get2Dgrid(step/2 - georefConventionOffsetPixel, step/2 + georefConventionOffsetLine);

                        // Direct Loc
                        double[][] directLocGrid = simpleLocEngine.computeDirectLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()), sensorGridForDirectLoc);

                        Vector<String> inputTIFs = new Vector<String>();
                        double pixelOffset = dirGrid.getPixelOffsetGranule();

                        for (int g = 0; g < granulesToCompute.size(); g++)
                        {
                            Granule gr = granulesToCompute.get(g);

                            int startGranule = gr.getFirstLine(res);
                            int sizeGranule = gr.getSizeLines(res);

                            double[][][] subDirectLocGrid = dirGrid.extractPointsDirectLoc(directLocGrid, startGranule, sizeGranule, config.getExportAlt());

                            double subLineOffset = dirGrid.getLineOffsetGranule(startGranule);

                            // Save in TIF
                            String gridFileName = gr.getCorrespondingGeoFileName(bandInfo);

                            // Save with originY = - originY and stepY = -stepY for VRT construction
                            outputFileManager.createGeoTiff(gridFileName, pixelOffset, -(startGranule + subLineOffset) ,
                            step, -step, subDirectLocGrid, "", "EPSG:4326", subLineOffset, pixelOffset, true);

                            // Add TIF to the future VRT
                            inputTIFs.add(gridFileName);
                        }

                        // Create VRT
                        double lineOffset = dirGrid.getLineOffsetGranule(0);
                        String vrtFileName = datastrip.getCorrespondingVRTFileName(detectorInfo, bandInfo);
                        outputFileManager.createVRT(vrtFileName, inputTIFs, step, lineOffset, pixelOffset, config.getExportAlt());

                        // Correction post build VRT
                        outputFileManager.correctGeoGrid(inputTIFs);
                        outputFileManager.correctVRT(vrtFileName);
                    }

                    // Inverse Loc case
                    // --------------------
                    else
                    {
                        double[] bb =  config.getInverseLocBound();

                        InverseLocGrid invGrid = new InverseLocGrid(bb[0], bb[1], bb[2], bb[3], config.getInverseLocReferential(), step);
                        double[][] groundGrid = invGrid.get2DgridLatLon();

                        double[][] inverseLocGrid = simpleLocEngine.computeInverseLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()),  groundGrid, "EPSG:4326");

                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");

                        // DD 06
                        // double[][] coords = new double[4][3];
                        // coords[0][1] = 32.99235766783721; //lat
                        // coords[0][0] = -18.210695132287352; //lon
                        // coords[1][1] = 32.99456809413312; //lat  
                        // coords[1][0] = -17.740052645288863; //lon
                        // coords[2][1] = 33.01; //lat
                        // coords[2][0] = -17.740052645288863; //lon
                        // coords[3][1] = 32.98456809413312; //lat  
                        // coords[3][0] = -17.740052645288863; //lon

                        // double[][] inverseLocGrid_2 = simpleLocEngine.computeInverseLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()),  coords, "EPSG:4326");
                        // LOGGER.info(String.valueOf(coords[0][0]) + "," + String.valueOf(coords[0][1]));
                        // LOGGER.info(String.valueOf(inverseLocGrid_2[0][0]) + "," + String.valueOf(inverseLocGrid_2[0][1]));
                        // LOGGER.info(String.valueOf(coords[1][0]) + "," + String.valueOf(coords[1][1]));
                        // LOGGER.info(String.valueOf(inverseLocGrid_2[1][0]) + "," + String.valueOf(inverseLocGrid_2[1][1]));
                        // LOGGER.info(String.valueOf(coords[2][0]) + "," + String.valueOf(coords[2][1]));
                        // LOGGER.info(String.valueOf(inverseLocGrid_2[2][0]) + "," + String.valueOf(inverseLocGrid_2[2][1]));
                        // LOGGER.info(String.valueOf(coords[3][0]) + "," + String.valueOf(coords[3][1]));
                        // LOGGER.info(String.valueOf(inverseLocGrid_2[3][0]) + "," + String.valueOf(inverseLocGrid_2[3][1]));

                        // double[][] directs = new double[2][2];
                        // directs[0][0] = 1041; directs[0][1] = 75; 
                        // directs[1][0] = 1014; directs[1][1] = 68;
                        // double[][] directLocGrid = simpleLocEngine.computeDirectLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()), directs);
                        // LOGGER.info(String.valueOf(directs[0][0]) + "," + String.valueOf(directs[0][1]));
                        // LOGGER.info(String.valueOf(directLocGrid[0][0]) + "," + String.valueOf(directLocGrid[0][1])+ "," + String.valueOf(directLocGrid[0][2]));
                        // LOGGER.info(String.valueOf(directs[1][0]) + "," + String.valueOf(directs[1][1]));
                        // LOGGER.info(String.valueOf(directLocGrid[1][0]) + "," + String.valueOf(directLocGrid[1][1])+ "," + String.valueOf(directLocGrid[1][2]));





                        int size = 5;
                        // DD 08
                        double[][] coords = new double[size][3];
                        coords[0][1] = 33.277; //lat
                        coords[0][0] = -17.146; //lon
                        coords[1][1] = 33.342; //lat  
                        coords[1][0] = -16.931; //lon
                        coords[2][1] = 32.941; //lat
                        coords[2][0] = -17.158; //lon
                        coords[3][1] = 32.746; //lat  
                        coords[3][0] = -17.083; //lon
                        coords[4][1] = 32.345; //lat  
                        coords[4][0] = -17.273; //lon

                        double[][] inverseLocGrid_2 = simpleLocEngine.computeInverseLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()),  coords, "EPSG:4326");

                        for (int i = 0; i<size; i++)
                        {
                            LOGGER.info(String.valueOf(coords[i][0]) + "," + String.valueOf(coords[i][1]));
                            LOGGER.info(String.valueOf(inverseLocGrid_2[i][0]) + "," + String.valueOf(inverseLocGrid_2[i][1]));
                        }

                        double[][] directs = new double[5][2];
                        directs[0][0] = 352; directs[0][1] = 54; 
                        directs[1][0] = 162; directs[1][1] = 350;
                        directs[2][0] = 926; directs[2][1] = 183;
                        directs[3][0] = 1232; directs[3][1] = 383;
                        directs[4][0] = 1984; directs[4][1] = 270;
                        double[][] directLocGrid = simpleLocEngine.computeDirectLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()), directs);

                        for (int i = 0; i<size; i++)
                        {
                            LOGGER.info(String.valueOf(directs[i][0]) + "," + String.valueOf(directs[i][1]));
                            LOGGER.info(String.valueOf(directLocGrid[i][0]) + "," + String.valueOf(directLocGrid[i][1])+ "," + String.valueOf(directLocGrid[i][2]));
                        }
                        
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
                        LOGGER.info("DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");

                        double[][][] grid3D = invGrid.get3Dgrid(inverseLocGrid, georefConventionOffsetPixel, -georefConventionOffsetLine);

                        String invFileName = datastrip.getCorrespondingInverseLocGrid(detectorInfo, bandInfo, config.getInverseLocOutputFolder());
                        outputFileManager.createGeoTiff(invFileName, invGrid.getUlX(), invGrid.getUlY(), invGrid.getStepX(), invGrid.getStepY(), grid3D, config.getInverseLocReferential(), "", 0.0f, 0.0f, false);
                    }
                }
            }

            // Export parameters used in a json file
            String outputConfigPath ;
            if (config.getOperation().equals(Sen2VMConstants.DIRECT))
            {
                outputConfigPath = datastrip.getPath() + File.separator + Sen2VMConstants.GEO_DATA_DS;
            }
            else
            {
                outputConfigPath = config.getInverseLocOutputFolder();
            }
            outputFileManager.writeInfoJson(config, bands, detectors, outputConfigPath);
        }
        catch (IOException exception)
        {
            throw new Sen2VMException(exception);
        }
        catch (SXGeoException exception)
        {
            String newMessage = "";
            if(exception.toString().contains("Cant find bundle for base name S2GeoMessages"))
            {
                newMessage = "Please, check the GEOID data contains .gtx, DBL, HDR and .xml : ";
            }
            throw new Sen2VMException(newMessage, exception);
        }
    }


}


