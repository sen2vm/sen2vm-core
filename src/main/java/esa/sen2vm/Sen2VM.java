package esa.sen2vm;

import org.apache.commons.cli.*;

/**
 * Main class
 *
 */
public class Sen2VM
{
    /**
     * Main process
     * @param args first arg : input json file. second param (optional) : parameter json file
     */
    public static void main( String[] args )
    {
        Options options = new Options();

        Option configOption = new Option("c", "config", true, "Mandatory. Path to the configuration file (in JSON format) regrouping all inputs");
        configOption.setRequired(true);
        options.addOption(configOption);

        Option paramOption = new Option("p", "param", true, "Optional. Path to parameter file (in JSON format) regrouping all parallelisation specificities. All processed if not provided");
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

        String configFilepath = cmd.getOptionValue("config");
        String sensorManagerFile = cmd.getOptionValue("param");

        System.out.println("Running Sen2VM core :\n");

        ConfigurationFile configFile = new ConfigurationFile(configFilepath);

        if (sensorManagerFile != null) {
            ParamFile paramsFile = new ParamFile(sensorManagerFile);
        }
    }
}