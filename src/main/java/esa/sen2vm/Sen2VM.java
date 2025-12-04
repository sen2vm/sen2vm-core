package esa.sen2vm;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import esa.sen2vm.input.GenericDemFileManager;
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
            demFileManager.buildMap(config.getDem());

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
                safeManager.testifDirectGridsToComputeAlreadyExist(detectors, bands) ;
            }
            else
            {
                safeManager.testifInverseGridsToComputeAlreadyExist(detectors, bands, config.getInverseLocOutputFolder()) ;
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
            outputFileManager.writeInfoJson(config, params, outputConfigPath);
        }
        catch ( IOException exception )
        {
            throw new Sen2VMException(exception);
        }
        catch ( SXGeoException exception )
        {
            throw new Sen2VMException(exception);
        }
    }


}


