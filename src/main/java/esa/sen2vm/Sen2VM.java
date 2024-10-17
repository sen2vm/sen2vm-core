package esa.sen2vm;

import org.apache.commons.cli.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Float;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;



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

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.ConfigurationFile;
import esa.sen2vm.input.ParamFile;
import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.granule.GranuleManager;
import esa.sen2vm.input.gipp.GIPPManager;
import esa.sen2vm.input.SafeManager;
import esa.sen2vm.input.Granule;
import esa.sen2vm.input.Datastrip;
import esa.sen2vm.input.OutputFileManager;
import esa.sen2vm.input.DirectLocGrid;
import esa.sen2vm.utils.BandInfo;
import esa.sen2vm.utils.DetectorInfo;
import esa.sen2vm.utils.Sen2VMConstants;

import org.sxgeo.exception.SXGeoException;

/**
 * Main class
 *
 */
public class Sen2VM
{
    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(Sen2VM.class.getName());

    public static final void showPoints(double[][] pixels, double[][] grounds) {
        for (int i=0; i<pixels.length; i++) {
            LOGGER.info("pixels = "+pixels[i][0]+" "+pixels[i][1]+" grounds = "+grounds[i][0]+" "+grounds[i][1]+" "+grounds[i][2]);
        }
    }

    /**
     * Main process
     * @param args first arg: input json file. second param (optional): parameter json file
     * @throws Sen2VMException
     */
    public static void main( String[] args ) throws Sen2VMException
    {
        Options options = new Options();

        Option configOption = new Option("c", "config", true, "Mandatory. Path to the configuration file (in JSON format) regrouping all inputs");
        configOption.setRequired(true);
        options.addOption(configOption);

        Option paramOption = new Option("p", "param", true, "Optional. Path to parameter file (in JSON format) regrouping all parallelization specificities. All processed if not provided");
        paramOption.setRequired(false);
        options.addOption(paramOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Sen2VM", options);

            System.exit(1);
            return;
        }

        // Set sen2VM logger
        try {
            LogManager.getLogManager().readConfiguration( new FileInputStream("src/main/resources/log.properties") );

            // Create a custom FileHandler with date and time in the filename
            String pattern = "/tmp/sen2VM-%s.log";
            String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = String.format(pattern, dateTime);

            FileHandler fileHandler = new FileHandler(fileName, true);
            fileHandler.setLevel(Level.SEVERE);
            fileHandler.setFormatter(new SimpleFormatter());

            // Add the custom FileHandler to the root logger
            Logger.getLogger("").addHandler(fileHandler);

            // Run core
            String configFilepath = cmd.getOptionValue("config");
            String sensorManagerFile = cmd.getOptionValue("param");

            LOGGER.info("Start Sen2VM");

            // Read configuration file
            ConfigurationFile configFile = new ConfigurationFile(configFilepath);

            // Read parameter file
            ParamFile paramsFile = null;
            List<DetectorInfo> detectors = DetectorInfo.getAllDetectorInfo();
            List<BandInfo> bands = BandInfo.getAllBandInfo();
            if (sensorManagerFile != null) {
                paramsFile = new ParamFile(sensorManagerFile);
                if(paramsFile.getDetectorsList().size() > 0) {
                    detectors = paramsFile.getDetectorsList();
                }
                if(paramsFile.getBandsList().size() > 0) {
                    bands = paramsFile.getBandsList();
                }
            }
            LOGGER.info("detectors = "+detectors);
            LOGGER.info("bands = "+bands);

            // Read datastrip
            DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), configFile.getBooleanRefining());

            // Read GIPP
            GIPPManager gippManager = new GIPPManager(configFile.getGippFolder(), bands, dataStripManager, configFile.getGippVersionCheck());

            // Initialize SimpleLocEngine

            // Init demManager
            Boolean isOverlappingTiles = true; // geoid is a single file (not tiles) so set overlap to True by default
            SrtmFileManager demFileManager = new SrtmFileManager(configFile.getDem());
            if(!demFileManager.findRasterFile()) {
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
                    sensorList.put(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD(), sensor);
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

            // Safe Manager
            SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);
            Datastrip ds = sm.getDatastrip() ;
            //ds.checkNoVRT(detectors, bands) ;

            // GIPP
            Float georefConventionOffset = 0.5f;

            OutputFileManager outputFileManager = new OutputFileManager();
            LOGGER.info("bands = " + bands);

            for (BandInfo bandInfo: bands) {
                LOGGER.info("### BAND " + bandInfo.getName() );
                float res = (float) bandInfo.getPixelHeight() ;
                Float step = configFile.getStepFromBandInfo(bandInfo) / res ;
                String epsg = configFile.getReferential() ;

                LOGGER.info("res grid: " + String.valueOf(configFile.getStepFromBandInfo(bandInfo)));
                LOGGER.info("res band: " + String.valueOf(res));
                LOGGER.info("step: " + String.valueOf(step));
                LOGGER.info("epsg: " + epsg);

                for (DetectorInfo detectorInfo: detectors) {
                    LOGGER.info("### DET " + detectorInfo.getName() );

                    if (configFile.getOperation().equals(Sen2VMConstants.DIRECT)) {

                        int[] BBox = sm.getFullSize(dataStripManager, bandInfo, detectorInfo);
                        int startLine = BBox[0] ;
                        int startPixel = BBox[1] ;
                        int sizeLine = BBox[2];
                        int sizePixel = BBox[3];

                        // Load Granule Info
                        ArrayList<Granule> granulesToCompute = sm.getGranulesToCompute(detectorInfo, bandInfo);
                        LOGGER.info("Number of granules found: " +  String.valueOf(granulesToCompute.size()));

                        // Get Full Sensor Grid
                        DirectLocGrid dirGrid = new DirectLocGrid(georefConventionOffset, step,
                                    startPixel, startLine, sizeLine, sizePixel);
                        double[][] sensorGridForDictorLoc = dirGrid.get2Dgrid(step/2, step/2);

                        // Direct Loc
                        double[][] directLocGrid = simpleLocEngine.computeDirectLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()), sensorGridForDictorLoc);

                        Vector<String> inputTIFs = new Vector<String>();
                        float pixelOffset = dirGrid.getPixelOffsetGranule().floatValue();
                        for(int g = 0 ; g < granulesToCompute.size(); g++ ) {
                            Granule gr = granulesToCompute.get(g) ;
                            int startGranule = gr.getFirstLine(res);
                            int sizeGranule = gr.getSizeLines(res);

                            double[][][] subDirectLocGrid = dirGrid.extractPointsDirectLoc(directLocGrid, startGranule, sizeGranule, configFile.getExportAlt()) ;
                            float subLineOffset = dirGrid.getLineOffsetGranule(startGranule).floatValue();

                            // Save in TIF
                            String gridFileName = gr.getCorrespondingGeoFileName(bandInfo);

                            // Save with originY = - originY and stepY = -stepY for VRT construction
                            outputFileManager.createGeoTiff(gridFileName, pixelOffset, -(startGranule + subLineOffset),
                            step, -step, subDirectLocGrid, "", "EPSG:4326", subLineOffset, pixelOffset) ;

                            // Add TIF to the future VRT
                            inputTIFs.add(gridFileName) ;
                        }

                        // Create VRT
                        float lineOffset = dirGrid.getLineOffsetGranule(0).floatValue();
                        String vrtFileName = ds.getCorrespondingVRTFileName(detectorInfo, bandInfo);
                        outputFileManager.createVRT(vrtFileName, inputTIFs, step, lineOffset, pixelOffset, configFile.getExportAlt()) ;

                        // Correction post build VRT
                        outputFileManager.correctGeoGrid(inputTIFs);
                        outputFileManager.correctVRT(vrtFileName);

                    } else if (configFile.getOperation().equals(Sen2VMConstants.INVERSE)) {

                        Float[] bb =  configFile.getInverseLocBound() ;
                        String invOutputDir = configFile.getInverseLocOutputFolder() ;
                        String nameSensor = bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD();

                        // Start
                        step = step * 100 ; // TODO

                        InverseLocGrid invGrid = new InverseLocGrid(bb[0], bb[1], bb[2], bb[3], epsg, step);
                        double[][] groundGrid = invGrid.get2DgridLatLon();

                        double[][] inverseLocGrid = simpleLocEngine.computeInverseLoc(sensorList.get(bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD()),  groundGrid, "EPSG:4326");
                        double[][][] grid3D = invGrid.get3Dgrid(inverseLocGrid) ;

                        String invFileName = ds.getCorrespondingInverseLocGrid(detectorInfo, bandInfo, configFile.getInverseLocOutputFolder());
                        outputFileManager.createGeoTiff(invFileName, bb[0], bb[1], invGrid.getStepX(), invGrid.getStepY(), grid3D, epsg, "", 0.0f, 0.0f) ;

                    } else {
                        LOGGER.info("Operation " + configFile.getOperation() + " does not exist.");
                    }
                }
            }

        } catch ( IOException exception ) {
            throw new Sen2VMException(exception);
        } catch ( SXGeoException exception ) {
            throw new Sen2VMException(exception);
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }
}