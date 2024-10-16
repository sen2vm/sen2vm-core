package esa.sen2vm.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.Sen2VMConstants;

/**
 * ConfigurationFile class
 *
 */
public class ConfigurationFile extends InputFileManager
{
    private static final Logger LOGGER = Logger.getLogger(ConfigurationFile.class.getName());

    private String configPath;
    private String l1bProduct;
    private String gippFolder;
    private boolean gippVersionCheck = true;
    private String dem;
    private String geoid;
    private String iers = "";
    private String orekitData;
    private String pod;
    private String operation;
    private boolean refining = false;
    private float band10m;
    private float band20m;
    private float band60m;
    private float ul_x;
    private float ul_y;
    private float lr_x;
    private float lr_y;
    private String referential;
    private String outputFolder;

    /**
     * Constructor
     * @param configPath path to the json configuration file
     * @throws Sen2VMException
     */
    public ConfigurationFile(String configPath) throws Sen2VMException {

        this.configPath = configPath;
        InputStream schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(Sen2VMConstants.JSON_SCHEMA_CONFIG);
        if (schemaStream == null) {
        	throw new Sen2VMException("Impossible to find the json schema for configuration file: " + Sen2VMConstants.JSON_SCHEMA_CONFIG);
        }
        // Check if the json file is correct
        if(check_schema(this.configPath, schemaStream)) {
            parse(this.configPath);
        }
    }

    /**
     * Parse json configuration file
     * @param jsonFilePath path to the json configuration file
     * @throws Sen2VMException
     */
    public void parse(String jsonFilePath) throws Sen2VMException {

    	LOGGER.info("Parsing file " + jsonFilePath);
        try (InputStream fis = new FileInputStream(jsonFilePath)) {

            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));

            this.l1bProduct = checkPath(jsonObject.getString("l1b_product"));
            this.gippFolder = checkPath(jsonObject.getString("gipp_folder"));
            this.dem = checkPath(jsonObject.getString("dem"));
            this.geoid = checkPath(jsonObject.getString("geoid"));
            this.orekitData = checkPath(jsonObject.getString("orekit_data"));
            this.pod = jsonObject.getString("pod");
            this.operation = jsonObject.getString("operation");

            JSONObject steps = jsonObject.getJSONObject("steps");
            this.band10m = steps.getFloat("10m_bands");
            this.band20m = steps.getFloat("20m_bands");
            this.band60m = steps.getFloat("60m_bands");

            // Optional parameters
            if (jsonObject.has("gipp_version_check")) {
                this.gippVersionCheck = jsonObject.getBoolean("gipp_version_check");
            }
            if (jsonObject.has("iers")) {
                this.iers = jsonObject.getString("iers");
                checkPath(this.iers);
            }
            if (jsonObject.has("deactivate_available_refining")) {
                this.refining = ! jsonObject.getBoolean("deactivate_available_refining");
            }

            // Check the type of location: direct or inverse
            if (this.operation.equals("inverse")) {
                if (!jsonObject.has("inverse_location_additional_info")) {
                    throw new Sen2VMException("Error inverse_location_additional_info parameter initialization is required when using inverse operation");
                }
                else {
                   try {
                       JSONObject inverseLoc = jsonObject.getJSONObject("inverse_location_additional_info");
                       this.ul_x = inverseLoc.getFloat("ul_x");
                       this.ul_y = inverseLoc.getFloat("ul_y");
                       this.lr_x = inverseLoc.getFloat("lr_x");
                       this.lr_y = inverseLoc.getFloat("lr_y");
                       this.referential = inverseLoc.getString("referential");
                       this.outputFolder = inverseLoc.getString("output_folder");
                   } catch(JSONException e) {
                       throw new Sen2VMException("Error when initializing inverse_location_additional_info", e);
                   }
                }
            }

        } catch (JSONException | IOException e) {
        	throw new Sen2VMException("Problem while reading json configuration file" + jsonFilePath + " : ", e);
        }
    }

    /*
     * Check that the input path exist, if not
     * @param filepath the path we want to check if it does exist
     * @throws Sen2VMException
     */
     public String checkPath(String filepath) throws Sen2VMException {
        File file = new File(filepath);
        if (!file.exists()) {
            throw new Sen2VMException("Path " + file + " does not exist");
        }
        return filepath;
     }

    /*
     * Search the datastrip metadata file path inside product folder
     * @throws Sen2VMException
     */
    public String getDatastripFilePath() throws Sen2VMException {
        File datastripFolder = new File(l1bProduct + "/" + Sen2VMConstants.DATASTRIP_MAIN_FOLDER);
        if (!datastripFolder.exists()) {
            throw new Sen2VMException("Datastrip folder " + datastripFolder + " does not exist");
        }

        File[] directories = datastripFolder.listFiles();
        String datastripFilePath = null;
        for (File dir: directories) {
            if (!dir.isDirectory()) {
                continue;
            }
            String filename = dir.getName().replaceAll("_N.*", "").replace(Sen2VMConstants.DATASTRIP_MSI_TAG, Sen2VMConstants.DATASTRIP_METADATA_TAG);
            datastripFilePath = dir + "/" + filename + Sen2VMConstants.xml_extention_small;
        }

        File datastripFile = new File(datastripFilePath);
        if (datastripFile.exists()) {
            LOGGER.info("Find the following datastrip metadata file: " + datastripFilePath);
            return datastripFilePath;
        }
        else {
            throw new Sen2VMException("No datastrip metadata file found inside folder: " + datastripFolder);
        }
    }

    /*
     * Get the product folder
     */
    public String getL1bProduct() {
       return l1bProduct;
    }

    /*
     * Get the gipp folder
     */
    public String getGippFolder() {
        return gippFolder;
    }

    /*
     * Get the boolean gippVersionCheck which, if set to false, will deactivate the
     * version check made on each GIPP to ensure compatibility
     */
    public Boolean getGippVersionCheck() {
        return gippVersionCheck;
    }

    /*
     * Get the DEM folder
     */
    public String getDem() {
       return dem;
    }

    /*
     * Get the geoid folder
     */
    public String getGeoid() {
       return geoid;
    }

    /*
     * Get the IERS bulletin file
     */
    public String getIers() {
       return iers;
    }

    /*
     * Get the Orekit data directory
     */
    public String getOrekitData() {
       return orekitData;
    }

   /*
     * Get the POD folder
     */
    public String getPod() {
       return pod;
    }

    /*
     * Get the boolean that tell if we want to deactivate the available refining or not
     */
    public Boolean getBooleanRefining() {
       return refining;
    }
}

