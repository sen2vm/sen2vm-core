package esa.sen2vm;

import org.apache.commons.cli.*;
import org.sxgeo.input.datamodels.sensor.Sensor;


/**
 * Main class
 *
 */
public class App
{
    /**
     * Main process
     * @param args first arg : input json file. second param (optional) : parameter json file
     */
    public static void main( String[] args )
    {
        Options options = new Options();

        Option configOption = new Option("c", "config", true, "mandatory path to the configuration file (in JSON format)");
        configOption.setRequired(true);
        options.addOption(configOption);

        Option paramOption = new Option("p", "param", true, "optional path to parameter file (in JSON format)");
        paramOption.setRequired(false);
        options.addOption(paramOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Sen2vm", options);

            System.exit(1);
            return;
        }

        String configFilepath = cmd.getOptionValue("config");
        String sensorManagerFile = cmd.getOptionValue("param");

        System.out.println("Running Sen2vm core :\n");

        ConfigurationFile configFile = new ConfigurationFile(configFilepath);
        System.out.println("configFile = " + configFile.operation);

        if (sensorManagerFile != null) {
            System.out.println("sensorManagerFile = " + sensorManagerFile);
            ParamFile paramsFile = new ParamFile(sensorManagerFile);
            System.out.println("paramsFile = " + paramsFile.detectors);
            System.out.println("paramsFile = " + paramsFile.bands);
        }
    }
}