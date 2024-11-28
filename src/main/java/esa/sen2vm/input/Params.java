package esa.sen2vm.input;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.Sen2VMConstants;

/**
 * Read the parameters file
 */
public class Params extends InputFileManager
{
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(Params.class.getName());

    /**
     * JSONArray of the detectors
     */
    public List<DetectorInfo> detectors;

    /**
     * JSONArray of the bands
     */
    public List<BandInfo>  bands;


    /**
     * Constructor
     * @param configPath path to the JSON configuration file
     * @throws Sen2VMException
     */
    public Params(CommandLine commandLine) throws Sen2VMException
    {
        // Read detectors
        Stream<String> detectorsList = Arrays.stream(commandLine.getOptionValues(OptionManager.OPT_DETECTORS_LIST_SHORT));

        for (String detector: detectorsList.toArray(String[]::new))
        {
            this.detectors.add(DetectorInfo.getDetectorInfoFromName(detector));
        }

        // Read bands
        Stream<String> bandsList = Arrays.stream(commandLine.getOptionValues(OptionManager.OPT_DETECTORS_LIST_SHORT));

        for (String band: bandsList.toArray(String[]::new))
        {
            this.bands.add(BandInfo.getBandInfoFromNameWithB(band));
        }
    } 

    /**
     * Constructor
     * @param paramPath path to the JSON parameters file
     * @throws Sen2VMException
     */
    public Params(String paramPath) throws Sen2VMException
    {
        InputStream schemaParamStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(Sen2VMConstants.JSON_SCHEMA_PARAMS);
        
        if (schemaParamStream == null)
        {
           throw new Sen2VMException("Impossible to find the JSON schema for parameters file: " + Sen2VMConstants.JSON_SCHEMA_PARAMS);
        }

        if(check_schema(paramPath, schemaParamStream))
        {
            parse(paramPath);
        }
    }

    /**
     * Parse JSON parameters file
     * @param jsonFilePath path to the JSON parameters file
     * @throws Sen2VMException
     */
    public void parse(String jsonFilePath) throws Sen2VMException
    {
        LOGGER.info("Parsing file " + jsonFilePath);
        try (InputStream fis = new FileInputStream(jsonFilePath))
        {
            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));
            
            // Load detectors
            JSONArray detectorsList = jsonObject.getJSONArray("detectors");
            this.detectors = new ArrayList<DetectorInfo>();
            for(int i=0; i < detectorsList.length(); i++)
            {
                this.detectors.add(DetectorInfo.getDetectorInfoFromName(detectorsList.getString(i)));
            }

            //Load bands
            JSONArray bandList = jsonObject.getJSONArray("bands");            
            this.bands = new ArrayList<BandInfo>();
            for(int i=0; i<bandList.length(); i++)
            {
               this.bands.add(BandInfo.getBandInfoFromNameWithB(bandList.getString(i)));
            }
        }
        catch (JSONException | IOException e)
        {
            throw new Sen2VMException("Problem while reading JSON parameters file" + jsonFilePath + " : ", e);
        }
    }

    /**
     * Get the detectors list
     * @return the detectors list
     */
    public List<DetectorInfo> getDetectorsList()
    {
       return this.detectors;
    }

    /**
     * Get the bands list
     * @return the bands list
     */
    public List<BandInfo> getBandsList()
    {
       return this.bands;
    }
}