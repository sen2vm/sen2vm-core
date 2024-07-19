package esa.sen2vm;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    public ParamFile(String jsonFilePath) {
        this.filepath = jsonFilePath;
        if(check_schema(this.filepath, "src/test/resources/schema_params.json")) {
            parse(this.filepath);
        }
    }

    public void parse(String jsonFilePath) {
        LOGGER.info("Parsing file "+ filepath);

        try (InputStream fis = new FileInputStream(jsonFilePath)) {

            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));

            this.detectors = jsonObject.getJSONArray("detectors");
            this.bands = jsonObject.getJSONArray("bands");

            // TODO add a step to get rid of all band or detector that don't exist

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}