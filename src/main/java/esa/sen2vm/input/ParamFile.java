package esa.sen2vm;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parameters class
 *
 */
public class ParamFile extends InputFileManager
{
    private String filepath;
    public JsonArray detectors;
    public JsonArray bands;

    public ParamFile(String jsonFilePath) {
        this.filepath = jsonFilePath;
        if(check_schema(this.filepath, "src/test/resources/schema_params.json")) {
            parse(this.filepath);
        }
    }

    public void parse(String jsonFilePath) {
        System.out.println("Parsing file : "+ jsonFilePath +"\n");

        try (InputStream fis = new FileInputStream(jsonFilePath);
             JsonReader reader = Json.createReader(fis)) {

            JsonObject jsonObject = reader.readObject();

            this.detectors = jsonObject.getJsonArray("detectors");
            this.bands = jsonObject.getJsonArray("bands");

            // TODO add a step to get rid of all band or detector that don't exist

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}