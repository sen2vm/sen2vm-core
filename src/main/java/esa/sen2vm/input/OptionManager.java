package esa.sen2vm.input;

import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.Sen2VMConstants;

/**
 * Definition of all command line arguments 
 * @author Guylaine Prat
 */
public class OptionManager
{
    /*******************************
     * Options using files 
     *******************************/
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

    /*******************************
     * Options for the configuration 
     *******************************/
    /**
     * Option for the operation (direct, inverse)
     */
    public static final String OPT_OPERATION_LONG = "operation";
    public static final String OPT_OPERATION_SHORT = "op";

    /**
     * Option for the l1b_product
     */
    public static final String OPT_L1B_LONG = "l1b_product";
    public static final String OPT_L1B_SHORT = "l1b";

    /**
     * Option for the gipp_folder
     */
    public static final String OPT_GIPP_LONG = "gipp_folder";
    public static final String OPT_GIPP_SHORT = "gipp";

    /**
     * Option for the dem folder
     */
    public static final String OPT_DEM_LONG = "dem_folder";
    public static final String OPT_DEM_SHORT = "dem";

    /**
     * Option for the geoid file
     */
    public static final String OPT_GEOID_LONG = "geoid_file";
    public static final String OPT_GEOID_SHORT = "geoid";

    /**
     * Option for the steps (pixels)
     */
    public static final String OPT_STEP_LONG = "steps";
    public static final String OPT_STEP_SHORT = "s";

    /**
     * Option for the GIPP version check or not (optional; no argument)
     */
    public static final String OPT_DEACTIVATE_AUTO_GIPP_SELECTION_LONG = "deactivate_auto_gipp_selection";
    public static final String OPT_DEACTIVATE_AUTO_GIPP_SELECTION_SHORT = "dgc";

    /**
     * Option for the IERS file path (optional)
     */
    public static final String OPT_IERS_LONG = "iers_file";
    public static final String OPT_IERS_SHORT = "iers";
    
    /**
     * Option to deactivate the refining or not (optional; no argument)
     */
    public static final String OPT_IGNORE_REFINING_LONG = "ignore_refining";
    public static final String OPT_IGNORE_REFINING_SHORT = "ir";
    
    /**
     * Option to export altitude in direct location grid = add a 3rd band 
     * to the grid of latitude/longitude (optional; no argument)
     */
    public static final String OPT_EXPORT_ALT_LONG = "export_altitude";
    public static final String OPT_EXPORT_ALT_SHORT = "alt";

    /**
     * Options for the inverse location (compulsory if operation is inverse location)
     */
    public static final String OPT_ULX_SHORT = "ulx";
    public static final String OPT_ULY_SHORT = "uly";
    public static final String OPT_LRX_SHORT = "lrx";
    public static final String OPT_LRY_SHORT = "lry";
    
    public static final String OPT_REFERENTIAL_LONG = "referential";
    public static final String OPT_REFERENTIAL_SHORT = "ref";
    
    public static final String OPT_OUTPUT_FOLDER_LONG = "output_folder";
    public static final String OPT_OUTPUT_FOLDER_SHORT = "o";

    /**
     * Option for the output image res (referential)
     */
    public static final String OPT_OUTPUT_IMAGE_RES_LONG = "output_image_res";
    public static final String OPT_OUTPUT_IMAGE_RES_SHORT = "or";

    /*******************************
     * Options for the params 
     *******************************/
    /**
     * Option to process only some detectors 
     * (optional; no argument = all detectors are processed)
     */
    public static final String OPT_DETECTORS_LIST_LONG = "detectors";
    public static final String OPT_DETECTORS_LIST_SHORT = "d";

    /**
     * Option to process only some detectors 
     * (optional; no argument = all detectors are processed)
     */
    public static final String OPT_BANDS_LIST_LONG = "bands";
    public static final String OPT_BANDS_LIST_SHORT = "b";

    /**
     * Option to overwrite grid
     * (optional; no argument = no overwrite)
     */
    public static final String OPT_OVERWRITE_GRIDS_LONG= "grids_overwritings";
    public static final String OPT_OVERWRITE_GRIDS_SHORT = "go";


    // If configuration file and (optional) parameter file are present: true
    private static boolean areFiles;
   
    private static final Logger LOGGER = Logger.getLogger(OptionManager.class.getName());

    /**
    * Read the command line arguments.
    * Two ways to call:
    *    * with configuration file and (optional) parameter file.
    *    * with each values defines in the files
    *    No combination of files and values is allowed.
    *    If configuration file is present, no values are read.
    * @param args the command line arguments
    * @return the commandLine
    * @throws Sen2VMException
    */
    public static CommandLine readCommandLineArguments(String[] args) throws Sen2VMException
    {
        CommandLine cmd = null;
      
        // Tell if configuration file and (optional) parameter file are present
        areFiles = false;
       
        // Read the files if present
        // =========================
        Option configOption = new Option(OPT_CONFIG_SHORT, OPT_CONFIG_LONG, true, "Mandatory. Path to the configuration file (JSON format) regrouping all inputs");
        configOption.setRequired(true);

        Option paramOption = new Option(OPT_PARAM_SHORT, OPT_PARAM_LONG, true, "Optional. Path to parameters file (JSON format) regrouping all parallelization specificities");
        paramOption.setRequired(false);

        Options optionsFiles = new Options();
        optionsFiles.addOption(configOption);
        optionsFiles.addOption(paramOption);

        // Parse the command line but doesn't stop on unknown options
        try
        {
            cmd = new DefaultParser().parse(optionsFiles, args, false);
            if (cmd != null)
            { // Configuration file found
                areFiles = true;
            }
        }
        catch (ParseException e)
        {
            // do nothing here as 2 ways to call sen2vm
        }


        // Read all other arguments if necessary
        // =====================================
        if (!areFiles)
        { // No configuration file is present

            // In case parameter file is present
            Options optionParam = new Options();
            optionParam.addOption(paramOption);
            
            try
            {
                cmd = new DefaultParser().parse(optionParam, args);
                // If parameter file option is present: exit as the config fle is not present
                if (cmd.hasOption(OPT_PARAM_SHORT))
                {
                    LOGGER.severe("If option -" + OPT_PARAM_SHORT + " or --" + OPT_PARAM_LONG + 
                                 " is present: the option -" +  OPT_CONFIG_SHORT + " or --" + 
                                 OPT_CONFIG_LONG + " is needed \n");
                    showHelp("With", optionsFiles);
                    System.exit(1);
                }
            }
            catch (ParseException e)
            {
                // Must not stop on other options ! The unknown options will be dealt later
            }


            Options optionsNoFile = new Options();

            // Compulsory arguments
            // --------------------
            Option operationOption = new Option(OPT_OPERATION_SHORT, OPT_OPERATION_LONG, true, 
                                               "!!! MANDATORY !!!: Type of operation available between direct or inverse operation [\"direct\", \"inverse\"]");
            operationOption.setRequired(true);            

            Option l1bOption = new Option(OPT_L1B_SHORT, OPT_L1B_LONG, true, "!!! MANDATORY !!!: Path to L1B product folder");
            l1bOption.setRequired(true);

            Option gippOption = new Option(OPT_GIPP_SHORT, OPT_GIPP_LONG, true, "!!! MANDATORY !!!: Path to GIPP folder");
            gippOption.setRequired(true);

            Option demOption = new Option(OPT_DEM_SHORT, OPT_DEM_LONG, true, "!!! MANDATORY !!!: Path to DEM folder");
            demOption.setRequired(true);

            Option geoidOption = new Option(OPT_GEOID_SHORT, OPT_GEOID_LONG, true, "!!! MANDATORY !!!: Path to GEOID file");
            geoidOption.setRequired(true);
           
            Option steps = new Option(OPT_STEP_SHORT, OPT_STEP_LONG, true, "!!! MANDATORY !!!: Steps (pixels) for bands 10, 20 and 60m \n"
                                        + "(separated with whitespace, respect the order)");
            steps.setType(Double.class); // TODO  does not seem to work: read as array of String
            steps.setArgs(3);
            steps.setRequired(true);
           
            // Optional arguments
            // ------------------
            Option iersOption = new Option(OPT_IERS_SHORT, OPT_IERS_LONG, true, "(optional) Path to IERS file");
            iersOption.setRequired(false);  

            Option noAutoGippSelectionOption = new Option(OPT_DEACTIVATE_AUTO_GIPP_SELECTION_SHORT, OPT_DEACTIVATE_AUTO_GIPP_SELECTION_LONG, false,
                                                    "(optional) Deactivate the check of GIPP version;\n"
                                                    + "if present= \"true\", if not= \"false\". ");
            noAutoGippSelectionOption.setRequired(false);

            Option overwrite_grids = new Option(OPT_OVERWRITE_GRIDS_SHORT, OPT_OVERWRITE_GRIDS_LONG, false,
                                                    "(optional) Activate grids overwriting;\n"
                                                    + "if present= \"true\", if not= \"false\". ");
            overwrite_grids.setRequired(false);

            Option noRefiningOption = new Option(OPT_IGNORE_REFINING_SHORT, OPT_IGNORE_REFINING_LONG, false, 
                                                "(optional) Allows to ignore refining parameters if they are available  in the Datastrip Metadata;\n"
                                                + "if present= \"true\", if not= \"false\". ");
            noRefiningOption.setRequired(false);

            Option exportAltOption = new Option(OPT_EXPORT_ALT_SHORT, OPT_EXPORT_ALT_LONG, false, 
                                               "(optional) Export altitude in direct location grid;\n"
                                                  + "if present= \"true\", if not= \"false\".");
            exportAltOption.setRequired(false);

            // For inverse location: the following options are compulsory
            Option ulxOption = new Option(OPT_ULX_SHORT, true, "(Mandatory for inverse loc) Upper Left X (referential unit)");
            // TODO ulxOption.setType(Double.class) does not seem to work: read as String
            ulxOption.setRequired(false);
            Option ulyOption = new Option(OPT_ULY_SHORT, true, "(Mandatory for inverse loc) Upper Left Y (referential unit)");
            ulyOption.setRequired(false);
            Option lrxOption = new Option(OPT_LRX_SHORT, true, "(Mandatory for inverse loc) Lower Right X (referential unit)");;
            lrxOption.setRequired(false);
            Option lryOption = new Option(OPT_LRY_SHORT, true, "(Mandatory for inverse loc) Lower Right Y (referential unit)");
            lryOption.setRequired(false);
          
            Option referentialOption = new Option(OPT_REFERENTIAL_SHORT, OPT_REFERENTIAL_LONG, true, "(Mandatory for inverse loc) ground referential");
            referentialOption.setRequired(false);
            
            Option outputFolderOption = new Option(OPT_OUTPUT_FOLDER_SHORT, OPT_OUTPUT_FOLDER_LONG, true, "(Mandatory for inverse loc) output folder");
            outputFolderOption.setRequired(false);

            Option outputImageRes = new Option(OPT_OUTPUT_IMAGE_RES_SHORT, OPT_OUTPUT_IMAGE_RES_LONG, true, "(Mandatory for inverse loc) output folder");
            outputImageRes.setType(Double.class); // TODO  does not seem to work: read as array of String
            outputImageRes.setArgs(3);
            outputImageRes.setRequired(true);
     
            // params arguments
            // ------------------
            Option detectors = new Option(OPT_DETECTORS_LIST_SHORT, OPT_DETECTORS_LIST_LONG, true, "(optional) List of detectors to process separated by spaces, example: 01 05 06 10 11");
            detectors.setType(Double.class);
            detectors.setArgs(Option.UNLIMITED_VALUES);
            detectors.setRequired(false);

            Option bands = new Option(OPT_BANDS_LIST_SHORT, OPT_BANDS_LIST_LONG, true, "(optional) List of bands to process separated by spaces, example: B01 B08 B8A B10 B11");
            bands.setType(Double.class);
            bands.setArgs(Option.UNLIMITED_VALUES);
            bands.setRequired(false);

            // Add the compulsory arguments
            optionsNoFile.addOption(operationOption);
            optionsNoFile.addOption(l1bOption);
            optionsNoFile.addOption(gippOption);
            optionsNoFile.addOption(demOption);
            optionsNoFile.addOption(geoidOption);
            optionsNoFile.addOption(steps);
           
            // Add the optional arguments
            optionsNoFile.addOption(iersOption);
            optionsNoFile.addOption(noAutoGippSelectionOption);
            optionsNoFile.addOption(overwrite_grids);
            optionsNoFile.addOption(noRefiningOption);
            optionsNoFile.addOption(exportAltOption);
            optionsNoFile.addOption(ulxOption);
            optionsNoFile.addOption(ulyOption);
            optionsNoFile.addOption(lrxOption);
            optionsNoFile.addOption(lryOption);
            optionsNoFile.addOption(referentialOption);
            optionsNoFile.addOption(outputFolderOption);
            optionsNoFile.addOption(outputImageRes);

            // Add the params arguments
            optionsNoFile.addOption(detectors);
            optionsNoFile.addOption(bands);

            try
            {
                cmd = new DefaultParser().parse(optionsNoFile, args);
            }
            catch (ParseException e)
            {
                if (cmd != null )
                { // No unknown option
                   
                    // Test if all mandatory options are missing.
                    // As the config file is also missing, we must stop here !
                    if (!cmd.hasOption(OPT_OPERATION_SHORT) && !cmd.hasOption(OPT_L1B_SHORT) &&
                           !cmd.hasOption(OPT_GIPP_SHORT) &&  !cmd.hasOption(OPT_DEM_SHORT) && 
                           !cmd.hasOption(OPT_GEOID_SHORT) && !cmd.hasOption(OPT_STEP_SHORT)) 
                    {
                        LOGGER.severe("At least one mandatory option is missing");
                        // print the help with configuration file also
                        showHelp("With", optionsFiles);
                    }
                    else
                    {
                        // Print the missing compulsory options (but not all options are missing here ...)
                        LOGGER.severe(e.getMessage());
                    }
                }
                else
                { // an unknown option was detected
                    LOGGER.severe(e.getMessage());
                    // print the help with configuration file also
                    showHelp("With", optionsFiles);
                }
                // Complete the help for arguments without files
                showHelp("Without", optionsNoFile);
                System.exit(1);
            }

            // In case of inverse location: must check the needed options
            if (cmd.getOptionValue(OptionManager.OPT_OPERATION_SHORT).toUpperCase().equals(Sen2VMConstants.INVERSE))
            {
                if (!cmd.hasOption(OPT_ULX_SHORT) || !cmd.hasOption(OPT_ULY_SHORT) || 
                    !cmd.hasOption(OPT_LRX_SHORT) || !cmd.hasOption(OPT_LRY_SHORT) ||
                    !cmd.hasOption(OPT_REFERENTIAL_SHORT) || !cmd.hasOption(OPT_OUTPUT_FOLDER_SHORT))
                {
                    LOGGER.severe("Some mandatory options are missing for Inverse Location process");
                    showHelp("Without", optionsNoFile);
                    System.exit(1);
                } // test presence of all inverse loc options
            } // test if inverse loc
        } // test ! areFiles
        return cmd;
    }


    /**
    * Tell if configuration file and (optional) parameter are present
    * @return true if configuration file is present
    */
    public static boolean areFiles()
    {
        return areFiles;
    }
   
    /**
    * Show all the options with appropriate message
    * @param head specific message
    * @param options list of options
    */
    private static void showHelp(String head, Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(head + " files \n  java -jar sen2vm.jar",  "\n", options, "", true);
    }
}