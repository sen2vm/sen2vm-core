package esa.sen2vm.input;

import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import esa.sen2vm.exception.Sen2VMException;


/**
 * Definition of all command line arguments 
 * @author Guylaine Prat
 */
public class OptionManager {

	
	/**
	 * Option for the configuration file
	 */
	public static final String OPT_CONFIG_LONG = "config";
	public static final String OPT_CONFIG_SHORT = "c";
	
	/**
	 * Option for the parameter file
	 */
	public static final String OPT_PARAM_LONG = "param";
	public static final String OPT_PARAM_SHORT = "p";

	
	/**
	 * Option for the operation (direct, inverse)
	 */
	public static final String OPT_OPERATION_LONG = "operation";
	public static final String OPT_OPERATION_SHORT = "op";

	/**
	 * Option for the l1b_product
	 */
	private static final String OPT_L1B_LONG = "l1b_product";
	private static final String OPT_L1B_SHORT = "l1b";

	/**
	 * Option for the gipp_folder
	 */
	private static final String OPT_GIPP_LONG = "gipp_folder";
	private static final String OPT_GIPP_SHORT = "gipp";

	/**
	 * Option for the dem folder
	 */
	private static final String OPT_DEM_LONG = "dem_folder";
	private static final String OPT_DEM_SHORT = "dem";

	/**
	 * Option for the geoid file
	 */
	private static final String OPT_GEOID_LONG = "geoid_file";
	private static final String OPT_GEOID_SHORT = "geoid";

	/**
	 * Option for the steps
	 */
	private static final String OPT_STEP_LONG = "steps";
	private static final String OPT_STEP_SHORT = "s";

//	/**
//	 * Option for the 
//	 */
//	private static final String OPT__LONG = "";
//	private static final String OPT__SHORT = "";
//
//	/**
//	 * Option for the 
//	 */
//	private static final String OPT__LONG = "";
//	private static final String OPT__SHORT = "";
//
//	/**
//	 * Option for the 
//	 */
//	private static final String OPT__LONG = "";
//	private static final String OPT__SHORT = "";
//
//	/**
//	 * Option for the 
//	 */
//	private static final String OPT__LONG = "";
//	private static final String OPT__SHORT = "";

	
	
//   /**
//    * Option for the input XML file
//    */
//   private static final String OPTION_INPUT_LONG = "input";
//   private static final String OPTION_INPUT_SHORT = "i";
//
// 
//   /**
//    * Value for directLoc associated to "operation" option
//    */
//   private static final String OPTION_OPERATION_DIRECT_LOC_VALUE = "directLoc";
//   /**
//    * Value for inverseLoc associated to "operation" option
//    */
//   private static final String OPTION_OPERATION_INVERSE_LOC_VALUE = "inverseLoc";
//
//   /**
//    * Option for band value
//    */
//   private static final String OPTION_BAND_LONG = "band";
//   private static final String OPTION_BAND_SHORT = "b";
//
//   /**
//    * Option for detector value
//    */
//   private static final String OPTION_DETECTOR_LONG = "detector";
//   private static final String OPTION_DETECTOR_SHORT = "d";
//
//   /**
//    * Option to choose GDAL numbering (if present)
//    * No argument with this option.
//    */
//   private static final String OPTION_GDAL_NUMBERING_LONG = "gdal-numbering";
//   private static final String OPTION_GDAL_NUMBERING_SHORT = "g";
//
//   /**
//    * Option to set the SRS
//    */
//   private static final String OPTION_SRS_LONG = "srs";
//   private static final String OPTION_SRS_SHORT = "s";
//
//   /**
//    * Option to set the longitude (for inverseLoc)
//    */
//   private static final String OPTION_LON_LONG = "longitude";
//   private static final String OPTION_LON_SHORT = "lon";
//
//   /**
//    * Option to set the latitude (for inverseLoc)
//    */
//   private static final String OPTION_LAT_LONG = "latitude";
//   private static final String OPTION_LAT_SHORT = "lat";
//
//   /**
//    * Option to set the pixel number (for directLoc)
//    */
//   private static final String OPTION_PIXEL_LONG = "pixel";
//   private static final String OPTION_PIXEL_SHORT = "px";
//
//   /**
//    * Option to set the line number (for directLoc)
//    */
//   private static final String OPTION_LINE_LONG = "line";
//   private static final String OPTION_LINE_SHORT = "li";


   /**
    * Logger 
    */
   private static final Logger LOGGER = Logger.getLogger(OptionManager.class.getName());

   /**
    * Read the command line parameters.
    * Two ways to call:
    *    * with configuration file and (optionnal) paramater file
    *    * with each values defines in the files
    *    No combination of files and values is allowed.
    * @param args the command line arguments
    * @return set the CommandLine
    * @throws Sen2VMException
    */
   public static CommandLine readCommandLineArguments(String[] args) throws Sen2VMException {
	   
//      try {

//         CommandLineParameter commandLineParam = null; 
       CommandLine cmd = null;

//         // At least : 1 argument is needed 
//         if (args.length < 1) {
////            Options options = OptionManager.getOptions();
////            OptionManager.showHelp(options);
//            LOGGER.severe("Less than 1 arguments : unable to process");
//            System.exit(1);
//
//         } else { // At least 1 argument
       
       // Group option for files
       Option configOption = new Option(OPT_CONFIG_SHORT, OPT_CONFIG_LONG, true, "Mandatory. Path to the configuration file (in JSON format) regrouping all inputs");
       configOption.setRequired(false);

       Option paramOption = new Option(OPT_PARAM_SHORT, OPT_PARAM_LONG, true, "Optional. Path to parameter file (in JSON format) regrouping all parallelization specificities. All processed if not provided");
       paramOption.setRequired(false);

//       OptionGroup groupFiles = new OptionGroup();
//       groupFiles.addOption(configOption)
//                 .addOption(paramOption);


       Options optionsFile = new Options();
       optionsFile.addOption(configOption);
       optionsFile.addOption(paramOption);

//       // this parses the command line but doesn't stop on unknown options (throw a ParseException)
//       try {
//    	   cmd = new DefaultParser().parse(optionsFile, args, false);
//       } catch (ParseException e) {
//    	   // do nothing here
//       }
//
//       if (cmd.getOptions().length == 0) {
//           // print the help here
//    	   System.out.println("Vide" + cmd.getOptions());
//       } else {
//    	   System.out.println("Arguments" + cmd.getOptions().toString());
//
//       }

//       } else {
//           OptionGroup group = new OptionGroup();
//           group.add(OptionsBuilder.withLongOpt("input").hasArg().create("i"));
//           group.add(OptionsBuilder.withLongOpt("output").hasArg().create("o"));
//           group.setRequired(true);
           
           
           
       // Then read all arguments

       // Group option for separate arguments
       Option operationOption = new Option(OPT_OPERATION_SHORT, OPT_OPERATION_LONG, true, "Type of operation available between direct or inverse operation. [\"direct\", \"inverse\"]");
       operationOption.setRequired(false);            


        	//                    Option l1bOption = new Option(OPT_L1B_SHORT, OPT_L1B_LONG, true, "Path to L1B product folder");
        	//                    l1bOption.setRequired(true);
        	//                    options.addOption(l1bOption);
        	//                    
        	//                    Option gippOption = new Option(OPT_GIPP_SHORT, OPT_GIPP_LONG, true, "Path to GIPP folder");
        	//                    gippOption.setRequired(true);
        	//                    options.addOption(gippOption);

//        	Option demOption = new Option(OPT_DEM_SHORT, OPT_DEM_LONG, true, "Path to DEM folder");
//        	demOption.setRequired(true);
//        	options.addOption(demOption);

        	//                    Option geoidOption = new Option(OPT_GEOID_SHORT, OPT_GEOID_LONG, true, "Path to GEOID file");
        	//                    geoidOption.setRequired(true);
        	//                    options.addOption(geoidOption);

//        	Option stepsOption = new Option(OPT_STEP_SHORT, OPT_STEP_LONG, true, "Steps for band: 10m, 20m, 60m.");
//        	stepsOption.setRequired(true);
//        	options.addOption(stepsOption);
       	
        	
        	
 
//            OptionGroup groupNoFiles = new OptionGroup();
//            groupNoFiles.addOption(configOption)
//                        .addOption(paramOption);
//
//            // Create all the options possible
//            Options options = new Options();
//            options.addOptionGroup(groupFiles)
//                   .addOptionGroup(groupNoFiles);
//
//            // Parse the arguments
//            CommandLineParser parser = new DefaultParser();
//            HelpFormatter formatter = new HelpFormatter();
//            try {
//            	cmd = parser.parse(options, args);
//            	
//            } catch (ParseException e) {
//                System.out.println(e.getMessage());
//                formatter.printHelp("Sen2VM", options);
//                System.exit(1);
//            }
            
            // Check if the config file exists
            if (cmd.hasOption(OPT_CONFIG_SHORT)) { // the configuration file exists
            	// all the other options are ignored except the parameter file

            	if (cmd.hasOption(OPT_PARAM_SHORT)) { // check if the parameter file exists

            	}

            } else { 
            	// one needs to read each values from the command line
            	// even if parameter file exist: it will not be read

            	if (cmd.hasOption(OPT_PARAM_SHORT)) { // check if the parameter file exists
            		LOGGER.warning("The parameter file will not be read: " + cmd.getOptionValue("param"));
            	}


//                try {
//                	cmd = parserWithoutFiles.parse(options, args);
//                } catch (ParseException e) {
//                    System.out.println(e.getMessage());
//                    formatterWithoutFiles.printHelp("Sen2VM", options);
//                    System.exit(1);
//                }


            } // test if the configuration file exists


            // As the command line arguments may vary : must parse the line 2 times according to the number of arguments 

//            CommandLineParser parser = new BasicParser();

//            // For grids (colocation, direct or inverse), the only arguments needed are : -i s2geolib_input_interface.xml 
//            if (args.length == 2) { // only grids are asked for
//
//               Options optionsGrids = OptionManager.getOptionsGrids();
//               CommandLine cmdGrids = parser.parse(optionsGrids, args, true);
//
//               // the 2 arguments "-i s2geolib_input_interface.xml" are needed
//
//               // Get the path to S2geolib_input_interface XML file
//               String readInputXmlFilePath = null;
//               if (cmdGrids.hasOption(OptionManager.OPTION_INPUT_SHORT) ) {
//                  readInputXmlFilePath = cmdGrids.getOptionValue(OptionManager.OPTION_INPUT_SHORT);
//               } else { // Compulsory option 
//                  OptionManager.showHelp(optionsGrids);
//                  LOGGER.fatal("The option -" + OptionManager.OPTION_INPUT_SHORT + 
//                               " or --" + OptionManager.OPTION_INPUT_LONG + " is compulsory");
//                  System.exit(1);
//               }
//
//               // Initialize the commandLineParam parameters (for grids only in that case)
//               commandLineParam = new CommandLineParameter(S2GeolibInterfaceInputParam.PARAMETER_NO_SIMPLE_LOC, readInputXmlFilePath);
//               // if directLoc or inverseLoc are also asked for : will be reinitialized with all the needed data
//
//
//            } else { // more than 2 arguments: direct or inverse loc is asked for
//
//               Options options = OptionManager.getOptions();
//               CommandLine cmd = parser.parse(options, args);
//
//               // The 2 arguments "-i s2geolib_input_interface.xml" are needed
//
//               // Get the path to S2geolib_input_interface XML file
//               String readInputXmlFilePath = null;
//               if (cmd.hasOption(OptionManager.OPTION_INPUT_SHORT) ) {
//                  readInputXmlFilePath = cmd.getOptionValue(OptionManager.OPTION_INPUT_SHORT);
//               } else { // Compulsory option 
//                  OptionManager.showHelp(options);
//                  LOGGER.fatal("The option -" + OptionManager.OPTION_INPUT_SHORT + 
//                        " or --" + OptionManager.OPTION_INPUT_LONG + " is compulsory");
//                  System.exit(1);
//               }
//
//               // Initialize the commandLineParam parameters
//               commandLineParam = new CommandLineParameter(S2GeolibInterfaceInputParam.PARAMETER_NO_SIMPLE_LOC, readInputXmlFilePath);
//               // if directLoc or inverseLoc are also asked for : will be reinitialized with all the needed data
//
//
//               // Check if the operation {directLoc or InverseLoc} is asked for ...
//               // otherwise only the grids are asked for 
//
//               // if "-o" exists : direct or inverse loc is asked for
//               boolean simpleLocAsked = cmd.hasOption(OptionManager.OPTION_OPERATION_SHORT);
//
//               if (simpleLocAsked) { // read all the needed options for directLoc or inverseLoc
//
//                  //                   options = OptionManager.getComplementaryOptions(options);
//                  //                   CommandLine cmd = parser.parse(options, args);
//
//                  // Get Band param
//                  List<BandInfo> bandList = new ArrayList<BandInfo>();
//                  if (cmd.hasOption(OptionManager.OPTION_BAND_SHORT)) {
//                     String bandName = cmd.getOptionValue(OptionManager.OPTION_BAND_SHORT);
//                     BandInfo bandInfo = BandInfo.getBandInfoFromNameWithB(bandName);
//                     if (bandInfo == null) {
//                        OptionManager.showHelp(options);
//                        LOGGER.fatal("The option -" + OptionManager.OPTION_BAND_SHORT + 
//                              " or --" + OptionManager.OPTION_BAND_LONG + " is invalid");
//                        System.exit(1);
//                     }
//                     bandList.add(bandInfo);
//
//                  } else { // compulsory
//                     OptionManager.showHelp(options);
//                     LOGGER.fatal("The option -" + OptionManager.OPTION_BAND_SHORT + 
//                           " or --" + OptionManager.OPTION_BAND_LONG + " is compulsory");
//                     System.exit(1);
//                  }
//
//                  // Get Detector param
//                  List<DetectorInfo> detectorList = new ArrayList<DetectorInfo>();
//                  if (cmd.hasOption(OptionManager.OPTION_DETECTOR_SHORT)) {
//                     String detectorName = cmd.getOptionValue(OptionManager.OPTION_DETECTOR_SHORT);
//                     DetectorInfo detectorInfo = DetectorInfo.getDetectorInfoFromName(detectorName);
//                     if (detectorInfo == null) {
//                        OptionManager.showHelp(options);
//                        LOGGER.fatal("The option -" + OptionManager.OPTION_DETECTOR_SHORT + 
//                              " or --" + OptionManager.OPTION_DETECTOR_LONG + " is invalid");
//                        System.exit(1);
//                     }
//                     detectorList.add(detectorInfo);
//                  } else { // compulsory
//                     OptionManager.showHelp(options);
//                     LOGGER.fatal("The option -" + OptionManager.OPTION_DETECTOR_SHORT + 
//                           " or --" + OptionManager.OPTION_DETECTOR_LONG + " is compulsory");
//                     System.exit(1);
//                  }
//
//                  // Initialize the commandLineParam parameters
//                  commandLineParam = new CommandLineParameter(S2GeolibInterfaceInputParam.PARAMETER_COMPUTE_SIMPLE_LOC, 
//                                                              readInputXmlFilePath, bandList, detectorList);
//
//                  // Operation type : directLoc or inverseLoc
//                  String operation = cmd.getOptionValue(OptionManager.OPTION_OPERATION_SHORT);
//                  if (OptionManager.OPTION_OPERATION_DIRECT_LOC_VALUE.equals(operation)) {
//                     commandLineParam.setDirectLoc(true);
//                     commandLineParam.setLocOperationName(OptionManager.OPTION_OPERATION_DIRECT_LOC_VALUE);
//                  } else if (OptionManager.OPTION_OPERATION_INVERSE_LOC_VALUE.equals(operation)) {
//                     commandLineParam.setDirectLoc(false);
//                     commandLineParam.setLocOperationName(OptionManager.OPTION_OPERATION_INVERSE_LOC_VALUE);
//                  } else {
//                     OptionManager.showHelp(options);
//                     LOGGER.fatal("The value for the option -" + OptionManager.OPTION_OPERATION_SHORT + 
//                           " or --" + OptionManager.OPTION_OPERATION_LONG + " is invalid : found "  + operation + 
//                           "\n Possible values are : " + OptionManager.OPTION_OPERATION_DIRECT_LOC_VALUE + " or " + OptionManager.OPTION_OPERATION_INVERSE_LOC_VALUE);
//                     System.exit(1);
//                  }
//
//                  // GDAL numbering
//                  // Pixel and line number start at zero. (0, 0) is the upper left corner of first pixel at first line
//                  boolean useGdalNumbering = cmd.hasOption(OptionManager.OPTION_GDAL_NUMBERING_SHORT);
//                  // if -g present : useGdalNumbering = true ; otherwise = false
//                  commandLineParam.setGDALconvention(useGdalNumbering);
//
//                  // SRS param
//                  if (cmd.hasOption(OptionManager.OPTION_SRS_SHORT)) {
//                     String srs = cmd.getOptionValue(OptionManager.OPTION_SRS_SHORT);
//                     commandLineParam.setSrs(srs);
//                  } else { // Set default SRS
//                     commandLineParam.setSrs(GdalTransformManager.EPSG_4326);
//                  }
//
//                  // According to the operation : some arguments are compulsory
//                  if (commandLineParam.isDirectLoc()){ // direct loc
//
//                     double pixel =  Double.NaN;
//                     double line = Double.NaN;
//                     
//                     // pixel 
//                     if (cmd.hasOption(OptionManager.OPTION_PIXEL_SHORT)) {
//                        pixel = Double.parseDouble(cmd.getOptionValue(OptionManager.OPTION_PIXEL_SHORT));
//                     } else {
//                        OptionManager.showHelp(options);
//                        LOGGER.fatal("The value for option -" + OptionManager.OPTION_PIXEL_SHORT + 
//                                     " or --" + OptionManager.OPTION_PIXEL_LONG + " is not a double. Found : " +
//                                     cmd.getOptionValue(OptionManager.OPTION_PIXEL_SHORT));
//                        System.exit(1);
//                     }
//                     // line 
//                     if (cmd.hasOption(OptionManager.OPTION_LINE_SHORT)) {
//                        line = Double.parseDouble(cmd.getOptionValue(OptionManager.OPTION_LINE_SHORT));
//                     } else {
//                        OptionManager.showHelp(options);
//                        LOGGER.fatal("The value for option -" + OptionManager.OPTION_LINE_SHORT + 
//                                     " or --" + OptionManager.OPTION_LINE_LONG + " is not a double. Found : " +
//                                     cmd.getOptionValue(OptionManager.OPTION_LINE_SHORT));
//                        System.exit(1);
//                     }
//                     if (!Double.isNaN(pixel) && !Double.isNaN(line)) {
//                        commandLineParam.setPixel(pixel);
//                        commandLineParam.setLine(line);
//                     } else {
//                        if (Double.isNaN(pixel)){
//                           LOGGER.fatal("The pixel is not set to a valid double value : Double.NaN. Impossible to compute the direct location");
//                        }
//                        if (Double.isNaN(line)){
//                           LOGGER.fatal("The line is not set to a valid double value : Double.NaN. Impossible to compute the direct location");
//                        }
//                        System.exit(1);
//                     }
//
//                  } else { // inverse loc 
//                     
//                     double longitude =  Double.NaN;
//                     double latitude = Double.NaN;
//                     
//                     // longitude (deg)
//                     if (cmd.hasOption(OptionManager.OPTION_LON_SHORT)) {
//                        longitude = Double.parseDouble(cmd.getOptionValue(OptionManager.OPTION_LON_SHORT));
//                     } else {
//                        OptionManager.showHelp(options);
//                        LOGGER.fatal("The value for option -" + OptionManager.OPTION_LON_SHORT + 
//                              " or --" + OptionManager.OPTION_LON_LONG + " is not a double. Found : " +
//                              cmd.getOptionValue(OptionManager.OPTION_LON_SHORT));
//                        System.exit(1);
//                     }
//                     // latitude (deg)
//                     if (cmd.hasOption(OptionManager.OPTION_LAT_SHORT)) {
//                        latitude = Double.parseDouble(cmd.getOptionValue(OptionManager.OPTION_LAT_SHORT));
//                     } else {
//                        OptionManager.showHelp(options);
//                        LOGGER.fatal("The value for option -" + OptionManager.OPTION_LAT_SHORT + 
//                              " or --" + OptionManager.OPTION_LAT_LONG + " is not a double. Found : " +
//                              cmd.getOptionValue(OptionManager.OPTION_LAT_SHORT));
//
//                        System.exit(1);
//                     }
//                     if (!Double.isNaN(longitude) && !Double.isNaN(latitude)) {
//                        commandLineParam.setLongitude(longitude);
//                        commandLineParam.setLatitude(latitude);
//                     } else {
//                        if (Double.isNaN(longitude)){
//                           LOGGER.fatal("The longitude is not set to a valid double value : Double.NaN. Impossible to compute the inverse location");
//                        }
//                        if (Double.isNaN(latitude)){
//                           LOGGER.fatal("The latitude is not set to a valid double value : Double.NaN. Impossible to compute the inverse location");
//                        }
//                        System.exit(1);
//                     }
//
//                  } // end test isDirectLoc
//
//               } // end test simpleLocAsked 
//
//
//            } // more the 2 arguments : direct or inverse loc is asked for 
//

//         } // at least 1 argument 

         return cmd;

         //
         //         } catch (ParseException pe){
         //        	 throw new Sen2VMException(pe);
   }




   /**
    * Get the options for grids only 
    * @return the Options for grids only
    * @throws S2GeolibException
    */
   private static Options getOptionsGrids() throws Sen2VMException {

      Options options = new Options();

//      String message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_FILE_PATH);
//      Option option = new Option(OPTION_INPUT_SHORT, OPTION_INPUT_LONG, true, message);
//      option.setRequired(true);
//      options.addOption(option);

      return options;
   }

   /**
    * Get all the options in case direct or inverse loc are asked for
    * @return the Options for direct or inverse loc
    * @throws S2GeolibException
    */
   private static Options getOptions() throws Sen2VMException {

      Options options = new Options();

//      String message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_FILE_PATH);
//      Option option = new Option(OPTION_INPUT_SHORT, OPTION_INPUT_LONG, true, message);
//      option.setRequired(true);
//      options.addOption(option);
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_OPERATION);
//      option = new Option(OPTION_OPERATION_SHORT, OPTION_OPERATION_LONG, true, message);
//      option.setRequired(true);
//      options.addOption(option);
//
//      //       return options;
//      //    }
//      //
//      //
//      //    private static Options getComplementaryOptions(Options options) throws S2GeolibException {
//      //
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_BAND_ID);
//      option = new Option(OPTION_BAND_SHORT, OPTION_BAND_LONG, true, message);
//      option.setRequired(true);
//      options.addOption(option);
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_DETECTOR_ID);
//      option = new Option(OPTION_DETECTOR_SHORT, OPTION_DETECTOR_LONG, true, message);
//      option.setRequired(true);
//      options.addOption(option);
//
//      //        OptionGroup optionGroupOperation = new OptionGroup();
//      //        optionGroupOperation.addOption(option);
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_LON_VALUE);
//      option = new Option(OPTION_LON_SHORT, OPTION_LON_LONG, true, message);
//      option.setRequired(false);
//      options.addOption(option);
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_LAT_VALUE);
//      option = new Option(OPTION_LAT_SHORT, OPTION_LAT_LONG, true, message);
//      option.setRequired(false);
//      options.addOption(option);
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_PIXEL_VALUE);
//      option = new Option(OPTION_PIXEL_SHORT, OPTION_PIXEL_LONG, true, message);
//      option.setRequired(false);
//      options.addOption(option);
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_LINE_VALUE);
//      option = new Option(OPTION_LINE_SHORT, OPTION_LINE_LONG, true, message);
//      option.setRequired(false);
//      options.addOption(option);
//
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_GDAL_NUMBERING);
//      option = new Option(OPTION_GDAL_NUMBERING_SHORT, OPTION_GDAL_NUMBERING_LONG, false, message);
//      option.setRequired(false);
//      options.addOption(option);
//
//      message = S2GeolibResourceBundle.getString(S2GeolibResourceBundle.INPUT_SRS);
//      option = new Option(OPTION_SRS_SHORT, OPTION_SRS_LONG, true, message);
//      option.setRequired(false);
//      options.addOption(option);

      return options;
   }

   /**
    * Show all the options with descriptions according to the Locale set
    * @param options
    */
   private static void showHelp(Options options) {

      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("s2geolib", options, true);
      System.out.println("\n");
   }

}
