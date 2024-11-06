package esa.sen2vm;

import org.apache.commons.cli.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Float;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
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

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.ConfigurationFile;
import esa.sen2vm.input.OptionManager;
import esa.sen2vm.input.ParamFile;
import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.gipp.GIPPManager;
import esa.sen2vm.utils.PathUtils;
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
    public static void main( String[] args ) throws Sen2VMException {

    	// Get the logger configuration
        InputStream logProperties = Thread.currentThread().getContextClassLoader().getResourceAsStream("log.properties");
        try {
			LogManager.getLogManager().readConfiguration(logProperties);
		} catch (SecurityException | IOException e) {
            throw new Sen2VMException(e);
		}

        // Run core
        LOGGER.info("Start Sen2VM");

        // Read the command line arguments
        // ===============================
        CommandLine commandLine = OptionManager.readCommandLineArguments(args);

        try {

            // Init all detectors and bands by default
        	List<DetectorInfo> detectors = DetectorInfo.getAllDetectorInfo();
        	List<BandInfo> bands = BandInfo.getAllBandInfo();

        	String operation;
            String l1bPath;
            String iersPath = "";
            String gippPath;
            Boolean gipp_version_check;
            Boolean deactivate_refining;
            Boolean export_alt;
            String demPath;
            String geoidPath;
            Float[] stepsValues = new Float[3];
            String podPath;
            String epsg;
            Float[] inverseLocBB = new Float[4];
            String invlocOutputPath;


            // Check if configuration file and (optional) parameter file are present
            if (OptionManager.areFiles()) {
            	
            	// Get the configuration and parameter files
            	String configFilepath = commandLine.getOptionValue(OptionManager.OPT_CONFIG_SHORT);
            	String sensorManagerFile = commandLine.getOptionValue(OptionManager.OPT_PARAM_SHORT);

            	// Read configuration file
            	ConfigurationFile configFile = new ConfigurationFile(configFilepath);

            	// Read parameter file (optional)
            	ParamFile paramsFile = null;
            	if (sensorManagerFile != null) {
            		paramsFile = new ParamFile(sensorManagerFile);
            		if (paramsFile.getDetectorsList().size() > 0) {
            			detectors = paramsFile.getDetectorsList();
            		}
            		if (paramsFile.getBandsList().size() > 0) {
            			bands = paramsFile.getBandsList();
            		}
            	}
            	
            	// Read arguments from files (the operations were checked through JSON)
            	operation = configFile.getOperation();
            	
                l1bPath = configFile.getDatastripFilePath();
                iersPath = configFile.getIers();
                gippPath = configFile.getGippFolder();
                demPath = configFile.getDem();
                geoidPath = configFile.getGeoid();
                podPath = configFile.getPod();
                gipp_version_check = configFile.getGippVersionCheck();
                deactivate_refining = configFile.getDeactivateRefining();
                export_alt = configFile.getExportAlt();
                
                stepsValues[0] = configFile.getStepBand10m();
                stepsValues[1] = configFile.getStepBand20m();
                stepsValues[2] = configFile.getStepBand60m();
                

                // For inverse location
                if (configFile.getOperation().equals(Sen2VMConstants.INVERSE)) {
                	// at this stage the inverse loc options exist
                	epsg = configFile.getInverseLocReferential();
                	inverseLocBB =  configFile.getInverseLocBound();
                	invlocOutputPath = PathUtils.checkPath(configFile.getInverseLocOutputFolder());
                }

            } else { // not areFiles
            	
            	// Read arguments from command line options
            	operation = commandLine.getOptionValue(OptionManager.OPT_OPERATION_SHORT).toUpperCase();

            	if ( !operation.equals(Sen2VMConstants.DIRECT) && !operation.equals(Sen2VMConstants.INVERSE)) {
                	LOGGER.severe("Operation " + operation + " is not allowed. Only direct or inverse are possible.");
                	System.exit(1);
                }

                l1bPath = PathUtils.getDatastripFilePath(commandLine.getOptionValue(OptionManager.OPT_L1B_SHORT));
                
                gippPath = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_GIPP_SHORT));

                demPath = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_DEM_SHORT));
                
                geoidPath = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_GEOID_SHORT));
                
                // convert the string array to an float array
                stepsValues = Arrays.stream(commandLine.getOptionValues(OptionManager.OPT_STEP_SHORT)).map(Float::valueOf).toArray(Float[]::new);

                // Optional parameters
                 
                if (commandLine.hasOption(OptionManager.OPT_IERS_SHORT)) {
                	iersPath = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_IERS_SHORT));
                }
                
                if (commandLine.hasOption(OptionManager.OPT_POD_SHORT)) {
                	podPath = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_POD_SHORT));
                }
                
                // By default we want the check of GIPP version. The option deactivate the check
                if (commandLine.hasOption(OptionManager.OPT_NOT_GIPP_CHECK_SHORT)) {
                	gipp_version_check  = ! Sen2VMConstants.GIPP_CHECK;
                } else { // We let the check
                	gipp_version_check = Sen2VMConstants.GIPP_CHECK;
                }

                // By default we want the refining. The option deactivate the refining
                if (commandLine.hasOption(OptionManager.OPT_NOT_REFINING_SHORT)) {
                	deactivate_refining = ! Sen2VMConstants.DEACTIVATE_REFINING;
                } else { // We let the refining
                	deactivate_refining = Sen2VMConstants.DEACTIVATE_REFINING;
                }
                
                // By default we don't want to export the altitude in the direct loc grid. The option export the altitude
                if (commandLine.hasOption(OptionManager.OPT_EXPORT_ALT_SHORT)) {
                	export_alt = ! Sen2VMConstants.EXPORT_ALT;
                } else { // We don't export the altitude
                	export_alt = Sen2VMConstants.EXPORT_ALT;
                }

                // For inverse location
                if (operation.equals(Sen2VMConstants.INVERSE)) {
                	// at this stage the inverse loc options exist
                	epsg = commandLine.getOptionValue(OptionManager.OPT_REFERENTIAL_SHORT);
                	inverseLocBB[0] =  Float.parseFloat(commandLine.getOptionValue(OptionManager.OPT_ULX_SHORT));
                	inverseLocBB[1] =  Float.parseFloat(commandLine.getOptionValue(OptionManager.OPT_ULY_SHORT));
                	inverseLocBB[2] =  Float.parseFloat(commandLine.getOptionValue(OptionManager.OPT_LRX_SHORT));
                	inverseLocBB[3] =  Float.parseFloat(commandLine.getOptionValue(OptionManager.OPT_LRY_SHORT));
                	invlocOutputPath = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_OUTPUT_FOLDER_SHORT));
                }
                
            } // end areFiles

            // TODO A effacer
            LOGGER.info("10m " + stepsValues[0] + " 20m " + stepsValues[1] + " 60m " + stepsValues[2]);
            LOGGER.info("detectors = " + detectors);
            LOGGER.info("bands = " + bands);
 
            
            // Read datastrip
             DataStripManager dataStripManager = new DataStripManager(l1bPath, iersPath, deactivate_refining);

            // Read GIPP
            GIPPManager gippManager = new GIPPManager(gippPath, bands, dataStripManager, gipp_version_check);

            
            // Initialize SimpleLocEngine
            // ==========================
            // Init demManager
            // ---------------
            Boolean isOverlappingTiles = true; // geoid is a single file (not tiles) so set overlap to True by default
            
            SrtmFileManager demFileManager = new SrtmFileManager(demPath);
            if (!demFileManager.findRasterFile()) {
                throw new Sen2VMException("Error when checking for DEM file");
            }
            GeoidManager geoidManager = new GeoidManager(geoidPath, isOverlappingTiles);
            DemManager demManager = new DemManager(demFileManager, geoidManager, isOverlappingTiles);

            
            // Build sensors list
            // ------------------
            // Save sensors for each focal plane
            List<Sensor> sensorList = new ArrayList<Sensor>();
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
                    sensorList.add(sensor);
                }
            }
            

            // Init rugged instance
            // --------------------
            RuggedManager ruggedManager = RuggedManager.initRuggedManagerDefaultValues(
                demManager,
                dataStripManager.getDataSensingInfos(),
                Sen2VMConstants.MINMAX_LINES_INTERVAL_QUARTER,
                Sen2VMConstants.RESOLUTION_10M_DOUBLE,
                sensorList,
                Sen2VMConstants.MARGIN,
                dataStripManager.getRefiningInfo()
            );
            ruggedManager.setLightTimeCorrection(false);
            ruggedManager.setAberrationOfLightCorrection(false);

            // at this stage only the direct and inverse loc are allowed
            if (operation.equals(Sen2VMConstants.DIRECT)) { // direct location
            	
                // Init simpleLocEngine
                // --------------------
                SimpleLocEngine simpleLocEngine = new SimpleLocEngine(
                    dataStripManager.getDataSensingInfos(),
                    ruggedManager,
                    demManager
                );

            	// Compute direct loc
            	// ------------------
            	double[][] pixels = {{0., 0.}};
            	double[][] grounds = simpleLocEngine.computeDirectLoc(sensorList.get(0), pixels);

            	// Print result
            	// ------------
            	showPoints(pixels, grounds);
            	
            } else if (operation.equals(Sen2VMConstants.INVERSE)) { // inverse location
            	
                LOGGER.info("Operation " + operation + " NOT yet implemented.");
                
            } // test direct/inverse
            	

            LOGGER.info("End Sen2VM");

        } catch ( SXGeoException exception ) {
            throw new Sen2VMException(exception);
		}
    }
}