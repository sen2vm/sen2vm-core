package esa.sen2vm.input;

import java.io.FileInputStream;
import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import esa.sen2vm.exception.Sen2VMException;

public class InputFileManager
{
    /**
     * Check that file path respect a certain JSON schema
     * @param filepath path to the input JSON file to check
     * @param schemaStream input stream containing the JSON schema to respect (not null at this stage)
     * @return true if the input file contains the required fields given by the JSON schema
     *         and false if it doesn't
     * @throws Sen2VMException
     */
    public boolean check_schema(String filepath, InputStream schemaStream) throws Sen2VMException
    {
        boolean correct_schema = false;
        try (InputStream jsonStream = new FileInputStream(filepath))
        {

            JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
            Schema schema = SchemaLoader.load(jsonSchema);

            JSONObject jsonObject = new JSONObject(new JSONTokener(jsonStream));

            schema.validate(jsonObject);

            correct_schema = true;
        }
        catch (Exception e)
        {
            // TODO give an explicit message for operation for instance (only direct or inverse possible)
            throw new Sen2VMException("Validation schema has failed for: " + filepath, e);
        }
        return correct_schema;
    }
}