package esa.sen2vm.input;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
public class ParamFile extends InputFileManager
{
    private static final Logger LOGGER = Logger.getLogger(ParamFile.class.getName());

    public JSONArray detectors;
    public JSONArray bands;

    /**
     * Constructor
     * @param paramPath path to the JSON parameters file
     * @throws Sen2VMException
     */
    public ParamFile(String paramPath) throws Sen2VMException {

        InputStream schemaParamStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(Sen2VMConstants.JSON_SCHEMA_PARAMS);
        
        if (schemaParamStream == null) {
           throw new Sen2VMException("Impossible to find the JSON schema for parameters file: " + Sen2VMConstants.JSON_SCHEMA_PARAMS);
        }

        if(check_schema(paramPath, schemaParamStream)) {
            parse(paramPath);
        }
    }

    /**
     * Parse JSON parameters file
     * @param jsonFilePath path to the JSON parameters file
     * @throws Sen2VMException
     */
    public void parse(String jsonFilePath) throws Sen2VMException {

        LOGGER.info("Parsing file " + jsonFilePath);
        try (InputStream fis = new FileInputStream(jsonFilePath)) {

            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));
            
            this.detectors = jsonObject.getJSONArray("detectors");
            this.bands = jsonObject.getJSONArray("bands");
            
        } catch (JSONException | IOException e) {
        	throw new Sen2VMException("Problem while reading JSON parameters file" + jsonFilePath + " : ", e);
        }
    }

    /**
     * Get the detectors list
     * @return the detectors list
     */
    public List<DetectorInfo> getDetectorsList() {
       List<DetectorInfo> detectorsList = new ArrayList<DetectorInfo>();
       for(int i=0; i < detectors.length(); i++) {
          detectorsList.add(DetectorInfo.getDetectorInfoFromName(detectors.getString(i)));
       }
       return detectorsList;
    }

    /**
     * Get the bands list
     * @return the bands list
     */
    public List<BandInfo> getBandsList() {
       List<BandInfo> bandsList = new ArrayList<BandInfo>();
       for(int i=0; i<bands.length(); i++) {
          bandsList.add(BandInfo.getBandInfoFromNameWithB(bands.getString(i)));
       }
       return bandsList;
    }
}
