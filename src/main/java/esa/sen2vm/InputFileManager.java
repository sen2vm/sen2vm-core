package esa.sen2vm;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.InputStream;

public class InputFileManager
{
    public boolean check_schema(String filepath, String configSchema) {
        boolean correct_schema = false;

        try (InputStream schemaStream = new FileInputStream(configSchema);
             InputStream jsonStream = new FileInputStream(filepath)) {

            JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
            Schema schema = SchemaLoader.load(jsonSchema);

            JSONObject jsonObject = new JSONObject(new JSONTokener(jsonStream));

            schema.validate(jsonObject);

            correct_schema = true;
        } catch (ValidationException e) {
            System.err.println("Validation failed: ");
            e.getCausingExceptions().stream()
                .map(ValidationException::getMessage)
                .forEach(System.err::println);
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
        }

        return correct_schema;
    }
}