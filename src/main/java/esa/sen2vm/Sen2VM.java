package esa.sen2vm;

import org.apache.commons.cli.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;

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
     * @param args first arg : input json file. second param (optional) : parameter json file
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
            if (sensorManagerFile != null) {
                ParamFile paramsFile = new ParamFile(sensorManagerFile);
            }

            // Read GIPP
            GIPPManager gippManager = GIPPManager.getInstance();
            gippManager.setGippFolderPath(configFile.getGippFolder());

            String[] detectors = {"D01"} ;
            String[] bands = {"B01"} ;

            System.out.println();
            System.out.println();
            System.out.println("Start");
            System.out.println();

            SafeManager sm = new SafeManager();
            // Load all images and geo grid already existing (granule x det x band)
            sm.setAndProcessGranules("../data/S2A_MSIL1B_20240508T075611_N0510_R035_20240605T140412.SAFE/GRANULE");
            // sm.setAndProcessDataStrip("../data/S2A_MSIL1B_20240508T075611_N0510_R035_20240605T140412.SAFE/DATASTRIP");

            // Create Grid with DS information [TODO]
            int pixelOffset = 0; // GIPP
            int lineOffset = 0; // GIPP

            int startPixel = 1 ; // GIPP
            int startLine = 1 ; // GIPP

            int fullSizeLine = 200 ; // DS + Granule
            int fullSizePixel = 93 ; // DS + Granule

            for (int b = 0 ; b < bands.length ; b++) {
                String bandName = bands[b];
                BandInfo band = BandInfo.getBandInfoFromNameWithB(bandName);
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

                for (int d = 0 ; d < detectors.length ; d++) {
                    String detectorName =  detectors[d];
                    DetectorInfo detector = DetectorInfo.getDetectorInfoFromNameWithD(detectorName);
                    System.out.println(detector);

                    ArrayList<Granule> granulesToCompute = sm.getGranulesToCompute(detector, band);
                    System.out.print("Number of granules found: ");
                    System.out.println(granulesToCompute.size());

                    for(int g = 0 ; g < granulesToCompute.size() ; g++ ) {
                        Granule gr = granulesToCompute.get(g) ;

                        double[][] directLocGrid = sensorGrid;
                        System.out.println(directLocGrid[0][0]);

                        int startGranule = 20; // MTD granule
                        int sizeGranule = 20; // MTD granule
                        //double[][][] subDirectLocGrid = dirGrid.extractPointsDirectLoc(directLocGrid, startGranule, sizeGranule) ;

                    }

                }
            }

            LOGGER.info("End Sen2VM");

        } catch ( IOException exception ) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}