package esa.sen2vm;

import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * ConfigurationFile class
 *
 */
public class ConfigurationFile extends InputFileManager
{
    private static final Logger LOGGER = Logger.getLogger(ConfigurationFile.class.getName());

    private String filepath;
    private String l1bProduct;
    private String gippFolder;
    private boolean gippVersionCheck;
    private String dem;
    private String geoid;
    private String iers;
    private String pod;
    private String operation;
    private boolean refining;
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
     * @param filepath Path to the configuration file to parse
     * @param filepath Path to the configuration file to parse
     */
    public ConfigurationFile(String filepath) throws Sen2VMException {
        this.filepath = filepath;
        if(check_schema(this.filepath, "src/test/resources/schema_config.json")) {
            parse(this.filepath);
        }
    }

    /**
     * Parse configuration file
     * @param filepath Path to the configuration file to parse
     */
    public void parse(String filepath) throws Sen2VMException {
        LOGGER.info("Parsing file "+ filepath);

        try (InputStream fis = new FileInputStream(filepath)) {

            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));

            this.l1bProduct = checkPath(jsonObject.getString("l1b_product"));
            this.gippFolder = checkPath(jsonObject.getString("gipp_folder"));
            this.gippVersionCheck = jsonObject.getBoolean("gipp_version_check");
            this.dem = checkPath(jsonObject.getString("dem"));
            this.geoid = checkPath(jsonObject.getString("geoid"));
            this.iers = jsonObject.getString("iers");
            this.pod = jsonObject.getString("pod");
            this.operation = jsonObject.getString("operation");
            this.refining = jsonObject.getBoolean("deactivate_available_refining");

            JSONObject steps = jsonObject.getJSONObject("steps");
            this.band10m = steps.getFloat("10m_bands");
            this.band20m = steps.getFloat("20m_bands");
            this.band60m = steps.getFloat("60m_bands");

            JSONObject inverseLoc = jsonObject.getJSONObject("inverse_location_additional_info");
            this.ul_x = inverseLoc.getFloat("ul_x");
            this.ul_y = inverseLoc.getFloat("ul_y");
            this.lr_x = inverseLoc.getFloat("lr_x");
            this.lr_y = inverseLoc.getFloat("lr_y");
            this.referential = inverseLoc.getString("referential");
            this.outputFolder = inverseLoc.getString("output_folder");

        } catch (Sen2VMException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Check that the input path exist, if not
     * @param filepath the path we want to check if it does exist
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
     * Get the IERS folder
     */
    public String getIers() throws Sen2VMException {
       return checkPath(iers);
    }

    /*
     * Get the POD folder
     */
    public String getPod() throws Sen2VMException {
       return checkPath(pod);
    }
}

