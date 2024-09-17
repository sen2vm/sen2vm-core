package esa.sen2vm.input;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import esa.sen2vm.exception.Sen2VMException;

public class InputFileManager
{
    private static final Logger LOGGER = Logger.getLogger(InputFileManager.class.getName());

    /**
     * Check that filepath respect a certain json schema
     *
     * @param filepath, path to the input json file to check
     * @param configSchema, path to the file that contains the json schema to respect
     * @return true if the input file contains the required fields given by the json schema
     *  and false if it doesn't
     * @throws Sen2VMException
     */
    public boolean check_schema(String filepath, String configSchema) throws Sen2VMException {
        boolean correct_schema = false;

        try (InputStream schemaStream = new FileInputStream(configSchema);
             InputStream jsonStream = new FileInputStream(filepath)) {

            JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
            Schema schema = SchemaLoader.load(jsonSchema);

            JSONObject jsonObject = new JSONObject(new JSONTokener(jsonStream));

            schema.validate(jsonObject);

            correct_schema = true;
        } catch (Exception e) {
            throw new Sen2VMException("Validation schema has failed for: " + filepath, e);
        }

        return correct_schema;
    }
}