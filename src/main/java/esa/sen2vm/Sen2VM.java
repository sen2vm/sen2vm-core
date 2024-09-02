package esa.sen2vm;

import org.apache.commons.cli.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Float;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;

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

/**
 * Main class
 *
 */
public class Sen2VM
{
    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(Sen2VM.class.getName());

    /**
     * Main process
     * @param args first arg: input json file. second param (optional): parameter json file
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
            File datastripFolder = new File(configFile.getL1bProduct() + "/DATASTRIP");
            File[] directories = datastripFolder.listFiles();
            String datastripFilePath = null;

            for (File dir: directories) {
                if (!dir.isDirectory()) {
                    continue; // Ignore non-directory files
                }
                String filename = dir.getName().replaceAll("_N.*", "").replace("MSI", "MTD");
                datastripFilePath = dir + "/" + filename + ".xml";
            }
            LOGGER.info("Reading datastrip file at path: " + datastripFilePath);
            DataStripManager dataStripManager = DataStripManager.getInstance();
            dataStripManager.initDataStripManager(datastripFilePath, configFile.getIers());

            String granulesFolder = configFile.getL1bProduct() + "/GRANULE/";
            String granuleExempleFolder = granulesFolder + "S2B_OPER_MSI_L1B_GR_2BPS_20240804T104054_S20240804T083750_D08_N05.11/S2B_OPER_MTD_L1B_GR_2BPS_20240804T104054_S20240804T083750_D08.xml" ;
            LOGGER.info("Granule" + granuleExempleFolder);

            dataStripManager.computeFullSize(granulesFolder);
            //GranuleManager granuleManager = GranuleManager.getInstance();
            //granuleManager.initGranuleManager(granuleExempleFolder);


            // Read GIPP
            GIPPManager gippManager = GIPPManager.getInstance();
            gippManager.setGippFolderPath(configFile.getGippFolder(), bands);

            // Initialize SimpleLocEngine

            // Init demManager
            Boolean isOverlappingTiles = true; // geoid is a single file (not tiles) so set overlap to True by default
            GeoidManager geoidManager = new GeoidManager(configFile.getGeoid(), isOverlappingTiles);
            SrtmFileManager demFileManager = new SrtmFileManager(configFile.getDem());
            if(!demFileManager.findRasterFile()) {
                throw new Sen2VMException("Error when checking for DEM file");
            }

            DemManager demManager = new DemManager(
                demFileManager,
                geoidManager,
                isOverlappingTiles);

            /*
            // Build sensor list

            // Save sensors for each focal plane
            HashMap<String, ArrayList<Sensor>> focalplaneSensors = new HashMap<String, ArrayList<Sensor>>();
            List<Sensor> sensorList = new ArrayList<Sensor>();
            for (DetectorInfo detectorInfo: detectors) {
                for (BandInfo bandInfo: bands) {
                    SensorViewingDirection viewing = gippManager.getSensorViewingDirections(bandInfo, detectorInfo);
                    LineDatation lineDatation = DataStripManager.getInstance().getLineDatation(bandInfo, detectorInfo);
                    SpaceCraftModelTransformation pilotingToMsi = gippManager.getPilotingToMsiTransformation();
                    SpaceCraftModelTransformation msiToFocalplane = gippManager.getMsiToFocalPlaneTransformation(bandInfo);
                    SpaceCraftModelTransformation focalplaneToSensor = gippManager.getFocalPlaneToDetectorTransformation(bandInfo, detectorInfo);

                    // Save sensor information
                    String sensor = bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD();
                    Sensor j_sensor = new Sensor(
                        sensor,
                        viewing,
                        lineDatation,
                        bandInfo.getPixelHeight(),
                        focalplaneToSensor,
                        msiToFocalplane,
                        pilotingToMsi
                    );
                    sensorList.add(j_sensor);
                }
            }

            // Init rugged instance
            RefiningInfo refiningInfo = new RefiningInfo();
            RuggedManager ruggedManager = RuggedManager.initRuggedManagerDefaultValues(
                demManager,
                DataStripManager.getInstance().getDataSensingInfos(),
                Sen2VMConstants.MINMAX_LINES_INTERVAL_QUARTER,
                Sen2VMConstants.PIXEL_HEIGHT_10,
                sensorList,
                Sen2VMConstants.MARGIN,
                refiningInfo
            );

            // Init simpleLocEngine
            SimpleLocEngine simpleLocEngine = new SimpleLocEngine(
                DataStripManager.getInstance().getDataSensingInfos(),
                ruggedManager,
                demManager
            );

            double[][] pixels = {{0.0, 0.0},
                                 {1.0, 0.0}};
            double[][] grounds = simpleLocEngine.computeDirectLoc(sensorList.get(0), pixels);
            LOGGER.info("pixels="+pixels[0][0]+" "+pixels[0][1]);
            LOGGER.info("grounds="+grounds[0][0]+" "+grounds[0][1]);

            System.out.println();
            System.out.println();
            System.out.println("Start");
            System.out.println();

            // GIPP
            int pixelOffset = 0;
            int lineOffset = 0;
            int startPixel = 1 ;
            int startLine = 1 ;

            // Safe Manager
            SafeManager sm = new SafeManager();

            // Load all images and geo grid already existing (granule x det x band)
            sm.setAndProcessGranules(configFile.getL1bProduct() + "/GRANULE");
            sm.setAndProcessDataStrip(configFile.getL1bProduct() + "/DATASTRIP");
            sm.checkEmptyGrid(detectors, bands) ;

            // VERFIER QUIL Y A QU DOSSIER S2* DANS DS

            // Datastrip Information
            Datastrip ds = sm.getDatastrip();

            // Load all ifnormations and geo grid already existing (granule x det x band)
            int fullSizeLine = 200 ; // DS + Granule
            int fullSizePixel = 93 ; // DS + Granule




            OutputFileManager outputFileManager = new OutputFileManager();

            for (int b = 0 ; b < bands.size() ; b++) {
                BandInfo band = bands.get(b);
                Float step ;

                switch(band.getPixelHeight()){
                    case Sen2VMConstants.RESOLUTION_10M:
                        step = configFile.getStepBand10m() ; break;
                    case Sen2VMConstants.RESOLUTION_20M:
                        step = configFile.getStepBand20m() ; break;
                    default:
                        step = configFile.getStepBand60m() ; break;
                }

                DirectLocGrid dirGrid = new DirectLocGrid(pixelOffset, lineOffset, step,
                                startPixel, startLine, fullSizePixel, fullSizeLine);

                double[][] sensorGrid = dirGrid.get2Dgrid();

                for (int d = 0 ; d < detectors.size() ; d++) {
                    DetectorInfo detector =  detectors.get(d);
                    System.out.println("Detector: " + detector.getName());

                    ArrayList<Granule> granulesToCompute = sm.getGranulesToCompute(detector, band);
                    System.out.print("Number of granules found: ");
                    System.out.println(granulesToCompute.size());

                    double[][] directLocGrid = sensorGrid; // TODO

                    for(int g = 0 ; g < granulesToCompute.size() ; g++ ) {
                        Granule gr = granulesToCompute.get(g) ;
                        String gridFileName = gr.getCorrespondingGeoFileName(band);

                        int startGranule = 20; // MTD granule
                        int sizeGranule = 20; // MTD granule

                        double[][][] subDirectLocGrid = dirGrid.extractPointsDirectLoc(directLocGrid, startGranule, sizeGranule) ;
                        String srs = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433],AUTHORITY[\"EPSG\",\"4326\"]]" ;
                        // Jonathan
                        outputFileManager.createGeoTiff(gridFileName, 1, startGranule, step, 2, srs, subDirectLocGrid) ;


                    }
                    // System.out.print(ds.getCorrespondingVRTFileName(detector, band));
                    // create list vrt
                    // outputFileManager.createVRT(ds.getCorrespondingVRTFileName(detector, band), Vector<String> inputVRTs) ;
                }
            }

            LOGGER.info("End Sen2VM");
            */
        } catch ( IOException exception ) {
            throw new Sen2VMException(exception);
        } catch ( SXGeoException exception ) {
            throw new Sen2VMException(exception);
        }
    }
}