package esa.sen2vm;

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
     */
    public ConfigurationFile(String filepath) {
        this.filepath = filepath;
        if(check_schema(this.filepath, "src/test/resources/schema_config.json")) {
            parse(this.filepath);
        }
    }

    /**
     * Parse configuration file
     * @param filepath Path to the configuration file to parse
     */
    public void parse(String filepath) {
        LOGGER.info("Parsing file "+ filepath);

        try (InputStream fis = new FileInputStream(filepath)) {

            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));

            this.l1bProduct = jsonObject.getString("l1b_product");
            this.gippFolder = jsonObject.getString("gipp_folder");
            this.gippVersionCheck = jsonObject.getBoolean("gipp_version_check");
            this.dem = jsonObject.getString("dem");
            this.geoid = jsonObject.getString("geoid");
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

            // TODO add verification of each parameter
            // for file see if it does really exist
            // for value, if possible, check that value is in the range of possible value

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Get the gipp folder
     */
    public String getGippFolder() {
       return gippFolder;
    }
}

