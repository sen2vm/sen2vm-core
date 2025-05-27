package esa.sen2vm;


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.io.File;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.Path;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.Configuration;

import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.enums.BandInfo;


public class Config
{

    private static final double THRESHOLD_DIR = 1e-9;
    private static final double THRESHOLD_INV = 1e-8;

    public static String config(String filePath, String l1b_product, int step, String operation, boolean refining) throws FileNotFoundException,
            IOException, ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        objJson.put("operation", operation);
        objJson.put("deactivate_available_refining", refining);

        JSONObject steps = (JSONObject) objJson.get("steps");
        steps.put("10m_bands", step / 10);
        steps.put("20m_bands", step / 20);
        steps.put("60m_bands", step / 60);

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


    public static String configInverseBB(String filePath,
                                        double ul_y, double ul_x,
                                        double lr_y, double lr_x,
                                        String referential, String l1b_product) throws FileNotFoundException,
                                         IOException, ParseException
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

    public static String changeDem(String filePath, String demPath, String l1b_product) throws FileNotFoundException,
            IOException, ParseException
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

    public static String configCheckGipp(String filePath, String gippPath, boolean checkGipp, String l1b_product) throws FileNotFoundException,
            IOException, ParseException
    {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("gipp_folder", gippPath);
        objJson.put("gipp_version_check", checkGipp);
        objJson.put("l1b_product", l1b_product);

        JSONObject inverse = (JSONObject) objJson.get("inverse_location_additional_info");
        inverse.put("output_folder", l1b_product);

        String outputConfig = l1b_product + "/configuration.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;
    }

    public static String createTestDir(String nameTest, String type) throws IOException
    {

        String inputRef = "src/test/resources/tests/input/TDS1/L1B_all";
        String outputDir = "src/test/resources/tests/output/" + nameTest;
        File outputDirFile = new File(outputDir);
        if(outputDirFile.exists()) {
            outputDirFile.delete();
        }
        copyFolder(new File(inputRef), new File(outputDir), true);
        return outputDir;
    }

public static String changeParams(String filePath, String[] detectors, String[] bands, String outputDir) throws FileNotFoundException,
            IOException, ParseException
{

        // String[] detectors, String[] bands

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
