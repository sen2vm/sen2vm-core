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

package esa.sen2vm;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.io.File;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.Path;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import esa.sen2vm.input.Configuration;

public class Config
{

    private static final double THRESHOLD_DIR = 1e-9;
    private static final double THRESHOLD_INV = 1e-8;

    public static String config(String filePath, String l1b_product, double stepBand10m, String operation, boolean refining) throws FileNotFoundException,
            IOException, ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        objJson.put("operation", operation);
        objJson.put("deactivate_available_refining", refining);

        JSONObject steps = (JSONObject) objJson.get("steps");
        steps.put("10m_bands", stepBand10m);
        steps.put("20m_bands", stepBand10m / 2);
        steps.put("60m_bands", stepBand10m / 6);

        JSONObject inverse = (JSONObject) objJson.get("inverse_location_additional_info");
        inverse.put("output_folder", l1b_product);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig); //overwrites the content of file
        writer.write(objJson.toString());
        writer.flush();
        writer.close();

        return outputConfig;
    }

    /**
     * Configuration of a test with a new or no iers
     *
     */
    public static String configIERS(String filePath, String l1b_product, String iers) throws FileNotFoundException,
            IOException, ParseException
    {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        if (iers == null)
        {
            objJson.remove("iers");
        } else {
            objJson.put("iers", iers);
        }

        JSONObject inverse = (JSONObject) objJson.get("inverse_location_additional_info");
        inverse.put("output_folder", l1b_product);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;

    }

    public static String configInverseBB(String filePath, double ul_y, double ul_x, double lr_y, double lr_x,
                                        String referential, String l1b_product)
        throws FileNotFoundException, IOException, ParseException
    {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);

        JSONObject inverse = (JSONObject) objJson.get("inverse_location_additional_info");
        inverse.put("ul_y", ul_y);
        inverse.put("ul_x", ul_x);
        inverse.put("lr_y", lr_y);
        inverse.put("lr_x", lr_x);
        inverse.put("referential", referential);
        inverse.put("output_folder", l1b_product);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;
    }

    public static String configInverseBBwithStepBand10m(String filePath, double ul_y, double ul_x, double lr_y, double lr_x,
                                        double stepBand10m, String referential, String l1b_product)
        throws FileNotFoundException, IOException, ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);

        JSONObject inverse = (JSONObject) objJson.get("inverse_location_additional_info");
        inverse.put("ul_y", ul_y);
        inverse.put("ul_x", ul_x);
        inverse.put("lr_y", lr_y);
        inverse.put("lr_x", lr_x);
        inverse.put("referential", referential);
        inverse.put("output_folder", l1b_product);

        JSONObject steps = (JSONObject) objJson.get("steps");
        steps.put("10m_bands", stepBand10m);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;
    }

    public static String changeDem(String filePath, String demPath, String l1b_product)
            throws FileNotFoundException, IOException, ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        objJson.put("dem", demPath);

        JSONObject inverse = (JSONObject) objJson.get("inverse_location_additional_info");
        inverse.put("output_folder", l1b_product);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;
    }

    public static String configAutoGippSelection(String filePath, String gippPath, boolean autoGippSelection, String l1b_product)
        throws FileNotFoundException, IOException, ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("gipp_folder", gippPath);
        objJson.put("auto_gipp_selection", autoGippSelection);
        objJson.put("l1b_product", l1b_product);

        JSONObject inverse = (JSONObject) objJson.get("inverse_location_additional_info");
        inverse.put("output_folder", l1b_product);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;
    }

    public static boolean deleteDirectory(File directory) {
        File[] listOfFiles = directory.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                deleteDirectory(file);
            }
        }
        return directory.delete();
    }

    public static String createTestDir(String nameTest, String type) throws IOException
    {
        String inputRef = "src/test/resources/tests/input/TDS1/L1B_all";
        String outputDir = "src/test/resources/tests/output/" + nameTest;
        File outputDirFile = new File(outputDir);
        if(outputDirFile.exists()) {
            deleteDirectory(outputDirFile);
        }
        copyFolder(new File(inputRef), new File(outputDir), true);
        return outputDir;
    }

    public static String changeParams(String filePath, String[] detectors, String[] bands, String outputDir)
        throws FileNotFoundException, IOException, ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONArray detectorsJsonArray = new JSONArray();
        for (String det : detectors)
        {
          detectorsJsonArray.add(det);
        }

        JSONArray bandsJsonArray = new JSONArray();
        for (String band : bands)
        {
          bandsJsonArray.add(band);
        }

        JSONObject objJson = (JSONObject) obj;
        objJson.put("detectors",detectorsJsonArray);
        objJson.put("bands", bandsJsonArray);

        String outputParam = outputDir + "/params.json";
        FileWriter writer = new FileWriter(outputParam, false);
        writer.write(obj.toString());
        writer.close();

        return outputParam;
    }

    public static void copyFolder(File src, File dest, boolean copy) throws IOException
    {
        if(src.isDirectory())
        {
            dest.getParentFile().mkdirs();

            if(!dest.exists())
            {
                dest.mkdir();
            }

            String files[] = src.list();

            for (String file : files)
            {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                copyFolder(srcFile,destFile, copy);
            }
        }
        else
        {
            if (copy)
            {
                Files.copy(src.toPath(), dest.toPath(), REPLACE_EXISTING);
            }
            else
            {
                Path records = src.toPath();
                Path recordsLink = dest.toPath();
            }
        }
    }
}