/** Copyright 2024-2025, CS GROUP, https://www.cs-soprasteria.com/
*
* This file is part of the Sen2VM Core project
*     https://gitlab.acri-cwa.fr/opt-mpc/s2_tools/sen2vm/sen2vm-core
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.*/

package esa.sen2vm.input;

import java.io.FileInputStream;
import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import esa.sen2vm.exception.Sen2VMException;

/**
 * Manager of the input configuration file
 */
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