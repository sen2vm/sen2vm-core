package esa.sen2vm.input;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import esa.sen2vm.utils.BandInfo;
import esa.sen2vm.utils.DetectorInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Parameters class
 *
 */
public class ParamFile extends InputFileManager
{
    private static final Logger LOGGER = Logger.getLogger(ParamFile.class.getName());

    private String filepath;
    public JSONArray detectors;
    public JSONArray bands;

    /**
     * Constructor
     * @param jsonFilePath Path to the parameter file to parse
     */
    public ParamFile(String jsonFilePath) {
        this.filepath = jsonFilePath;
        if(check_schema(this.filepath, "src/main/resources/schema_params.json")) {
            parse(this.filepath);
        }
    }

    /**
     * Parse parameter file
     * @param jsonFilePath Path to the configuration file to parse
     */
    public void parse(String jsonFilePath) {
        LOGGER.info("Parsing file "+ filepath);

        try (InputStream fis = new FileInputStream(jsonFilePath)) {

            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));

            this.detectors = jsonObject.getJSONArray("detectors");
            this.bands = jsonObject.getJSONArray("bands");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Get the detectors list
     */
    public JSONArray getDetectors() {
       return detectors;
    }

    /*
     * Get the bands list
     */
    public List<DetectorInfo> getDetectorsList() {
       List<DetectorInfo> detectorsList = new ArrayList<DetectorInfo>();
       for(int i=0; i<detectors.length(); i++) {
          detectorsList.add(DetectorInfo.getDetectorInfoFromName(detectors.getString(i)));
       }
       return detectorsList;
    }

    /*
     * Get the bands list
     */
    public JSONArray getBands() {
       return bands;
    }

    /*
     * Get the bands list
     */
    public List<BandInfo> getBandsList() {
       List<BandInfo> bandsList = new ArrayList<BandInfo>();
       for(int i=0; i<bands.length(); i++) {
          bandsList.add(BandInfo.getBandInfoFromNameWithB(bands.getString(i)));
       }
       return bandsList;
    }
}