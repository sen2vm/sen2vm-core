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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import esa.sen2vm.enums.BandInfo;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.PathUtils;
import esa.sen2vm.utils.Sen2VMConstants;

/**
 * Read the configuration file
 */
public class Configuration extends InputFileManager
{
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    private String configPath;
    private String l1bProduct;
    private String gippFolder;
    private boolean gippVersionCheck = Sen2VMConstants.AUTO_GIPP_SELECTION;
    private boolean gridsOverwriting = Sen2VMConstants.GRIDS_OVERWRITING;
    private String dem;
    private String geoid;
    private String iers = "";
    private String operation;
    private boolean deactivateRefining = Sen2VMConstants.DEACTIVATE_REFINING;
    private double step_band10m;
    private double step_band20m;
    private double step_band60m;
    private boolean exportAlt = Sen2VMConstants.EXPORT_ALT;
    private double ul_x;
    private double ul_y;
    private double lr_x;
    private double lr_y;
    private String referential;
    private String outputFolder;


    /**
     * Constructor
     * @param configPath path to the JSON configuration file
     * @throws Sen2VMException
     */
    public Configuration(CommandLine commandLine) throws Sen2VMException
    {
        // Read arguments from command line options
        this.operation = commandLine.getOptionValue(OptionManager.OPT_OPERATION_SHORT).toUpperCase();

        if ( !this.operation.equals(Sen2VMConstants.DIRECT) && !this.operation.equals(Sen2VMConstants.INVERSE))
        {
            LOGGER.severe("Operation " + this.operation + " is not allowed. Only direct or inverse are possible.");
            System.exit(1);
        }

        this.l1bProduct = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_L1B_SHORT));
        
        this.gippFolder = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_GIPP_SHORT));

        this.dem = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_DEM_SHORT));
        
        this.geoid = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_GEOID_SHORT));
        
        // convert the string array to an double array
        Double[] stepsValues = Arrays.stream(commandLine.getOptionValues(OptionManager.OPT_STEP_SHORT)).map(Double::valueOf).toArray(Double[]::new);

        this.step_band10m = stepsValues[0];
        this.step_band20m = stepsValues[1];
        this.step_band60m = stepsValues[2];

        // Optional parameters
            
        if (commandLine.hasOption(OptionManager.OPT_IERS_SHORT))
        {
            LOGGER.info("Reading IERS file at: " + commandLine.getOptionValue(OptionManager.OPT_IERS_SHORT));
            this.iers = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_IERS_SHORT));
        }
        
        // By default we want the check of GIPP version. The option deactivate the check
        if (commandLine.hasOption(OptionManager.OPT_DEACTIVATE_AUTO_GIPP_SELECTION_SHORT))
        {
            this.gippVersionCheck  = false;
        }
        else
        { // We let the check
            this.gippVersionCheck = true;
        }

        // By default we won't overwrite grids. The option deactivate the overwriting
        if (commandLine.hasOption(OptionManager.OPT_OVERWRITE_GRIDS_SHORT))
        {
            this.gridsOverwriting  = true;
        }
        else
        { // We let the check
            this.gridsOverwriting = false;
        }

        // By default we want the refining. The option deactivate the refining
        if (commandLine.hasOption(OptionManager.OPT_IGNORE_REFINING_SHORT))
        {
            this.deactivateRefining = true;
        }
        else
        { // We let the refining
            this.deactivateRefining = false;
        }
        
        // By default we don't want to export the altitude in the direct loc grid. The option export the altitude
        if (commandLine.hasOption(OptionManager.OPT_EXPORT_ALT_SHORT))
        {
            this.exportAlt = ! Sen2VMConstants.EXPORT_ALT;
        }
        else
        { // We don't export the altitude
            this.exportAlt = Sen2VMConstants.EXPORT_ALT;
        }

        // For inverse location
        if (operation.equals(Sen2VMConstants.INVERSE))
        {
            // at this stage the inverse loc options exist
            this.referential = commandLine.getOptionValue(OptionManager.OPT_REFERENTIAL_SHORT);
            this.ul_x =  Double.parseDouble(commandLine.getOptionValue(OptionManager.OPT_ULX_SHORT));
            this.ul_y =  Double.parseDouble(commandLine.getOptionValue(OptionManager.OPT_ULY_SHORT));
            this.lr_x =  Double.parseDouble(commandLine.getOptionValue(OptionManager.OPT_LRX_SHORT));
            this.lr_y =  Double.parseDouble(commandLine.getOptionValue(OptionManager.OPT_LRY_SHORT));
            this.outputFolder = PathUtils.checkPath(commandLine.getOptionValue(OptionManager.OPT_OUTPUT_FOLDER_SHORT));
        }
    }

    /**
     * Constructor
     * @param configPath path to the JSON configuration file
     * @throws Sen2VMException
     */
    public Configuration(String configPath) throws Sen2VMException
    {
        this.configPath = configPath;
        InputStream schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(Sen2VMConstants.JSON_SCHEMA_CONFIG);
        
        if (schemaStream == null)
        {
            throw new Sen2VMException("Impossible to find the JSON schema for configuration file: " + Sen2VMConstants.JSON_SCHEMA_CONFIG);
        }
        // Check if the JSON file is correct
        if(check_schema(this.configPath, schemaStream))
        {
            parse(this.configPath);
        }
    }

    /**
     * Parse JSON configuration file
     * @param jsonFilePath path to the JSON configuration file
     * @throws Sen2VMException
     */
    public void parse(String jsonFilePath) throws Sen2VMException
    {
        LOGGER.info("Parsing file " + jsonFilePath);
        
        try (InputStream fis = new FileInputStream(jsonFilePath))
        {
            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));

            this.l1bProduct = PathUtils.checkPath(jsonObject.getString("l1b_product"));
            this.gippFolder = PathUtils.checkPath(jsonObject.getString("gipp_folder"));
            this.dem = PathUtils.checkPath(jsonObject.getString("dem"));
            this.geoid = PathUtils.checkPath(jsonObject.getString("geoid"));

            this.operation = jsonObject.getString("operation");

            JSONObject steps = jsonObject.getJSONObject("steps");
            this.step_band10m = steps.getDouble("10m_bands");
            this.step_band20m = steps.getDouble("20m_bands");
            this.step_band60m = steps.getDouble("60m_bands");

            this.exportAlt = jsonObject.getBoolean("export_alt");

            // Optional parameters
            if (jsonObject.has("auto_gipp_selection"))
            {
                this.gippVersionCheck = jsonObject.getBoolean("auto_gipp_selection");
            }
            if (jsonObject.has("grids_overwritings"))
            {
                this.gridsOverwriting = jsonObject.getBoolean("grids_overwritings");
            }
            if (jsonObject.has("grids_overwriting"))
            {
                this.gridsOverwriting = jsonObject.getBoolean("grids_overwriting");
            }
            if (jsonObject.has("grids_overwriting"))
            {
                this.gridsOverwriting = jsonObject.getBoolean("grids_overwriting");
            }
            if (jsonObject.has("iers"))
            {
                this.iers = jsonObject.getString("iers");
                LOGGER.info("Reading IERS file at: " + this.iers);
                PathUtils.checkPath(this.iers);
            }
            if (jsonObject.has("deactivate_available_refining"))
            {
                this.deactivateRefining = jsonObject.getBoolean("deactivate_available_refining");
            }

            // Check the type of location: direct or inverse
            if (this.operation.equals("inverse"))
            {
                if (!jsonObject.has("inverse_location_additional_info"))
                {
                    throw new Sen2VMException("Error inverse_location_additional_info parameter initialization is required when using inverse operation");
                }
                else
                {
                   try
                   {
                       JSONObject inverseLoc = jsonObject.getJSONObject("inverse_location_additional_info");
                       this.ul_x = inverseLoc.getDouble("ul_x");
                       this.ul_y = inverseLoc.getDouble("ul_y");
                       this.lr_x = inverseLoc.getDouble("lr_x");
                       this.lr_y = inverseLoc.getDouble("lr_y");
                       this.referential = inverseLoc.getString("referential");
                       this.outputFolder = inverseLoc.getString("output_folder");
                   }
                   catch(JSONException e)
                   {
                       throw new Sen2VMException("Error when initializing inverse_location_additional_info", e);
                   }
                }
            }
        }
        catch (JSONException | IOException e)
        {
            throw new Sen2VMException("Problem while reading JSON configuration file" + jsonFilePath + " : ", e);
        }
    }

    /**
     * Get the datastrip file path
     * @return the datastrip file path
     * @throws Sen2VMException
     */
    public String getDatastripFilePath() throws Sen2VMException
    {
        return PathUtils.getDatastripFilePath(l1bProduct);
    }

    /**
     * Get the operation
     * @return the operation (DIRECT, INVERSE)
     */
    public String getOperation()
    {
        return operation.toUpperCase();
    }

    /**
     * Get the L1B product folder
     * @return the L1B product folder
     */
    public String getL1bProduct()
    {
       return l1bProduct;
    }

    /**
     * Get the GIPP folder
     * @return the GIPP folder
     */
    public String getGippFolder()
    {
        return gippFolder;
    }

    /**
     * Get the boolean which, if set to false, will deactivate the
     * version check made on each GIPP to ensure compatibility
     * @return deactivate the GIPP version check if false (true by default) 
     */
    public Boolean getGippVersionCheck()
    {
        return gippVersionCheck;
    }

    /**
     * Get the boolean which, if set to false, will activate the
     * grids overwriting
     * @return activate the grids overwriting (false by default)
     */
    public Boolean getGridsOverwriting()
    {
        return gridsOverwriting;
    }

    /**
     * Get the DEM folder
     * @return the DEM folder
     */
    public String getDem()
    {
       return dem;
    }

    /**
     * Get the geoid folder
     * @return the geoid folder
     */
    public String getGeoid()
    {
       return geoid;
    }

    /**
     * Get the IERS bulletin file
     * @return the IERS bulletin file
     */
    public String getIers()
    {
       return iers;
    }

    /**
     * Get the boolean that tell if we want to deactivate the refining or not
     * @return deactivate refining if true (false by default)
     */
    public Boolean getDeactivateRefining()
    {
       return deactivateRefining;
    }
    
    /**
     * Get the boolean which, if set to false, will deactivate the
     * saving of the altitude in the direct location grid
     * @return activate the saving of altitude if true (false by default)
     */
    public Boolean getExportAlt()
    {
        return exportAlt;
    }
    
    /**
     * Get the step of 10m band
     * @return the step for 10m band (pixels)
     */
    public double getStepBand10m()
    {
       return this.step_band10m;
    }

    /**
     * Get the step of 20m band
     * @return the step for 20m band (pixels)
     */
    public double getStepBand20m()
    {
       return this.step_band20m;
    }

    /**
     * Get the step of 60m band
     * @return the step for 60m band (pixels)
     */
    public double getStepBand60m()
    {
       return this.step_band60m;
    }
    
    /**
     * Get the step for a given band
     * @param bandInfo
     * @return the step for a given band (pixels)
     */
    public double getStepFromBandInfo(BandInfo bandInfo)
    {
        double step;
        switch((int) bandInfo.getPixelHeight())
        {
            case Sen2VMConstants.RESOLUTION_10M:
                step = this.getStepBand10m();
                break;
            case Sen2VMConstants.RESOLUTION_20M:
                step = this.getStepBand20m();
                break;
            default:
                step = this.getStepBand60m();
                break;
        }
        return step;
    }
    
    /**
     * Get the inverse location bounds
     * @return ulx, uly, lrx, lry (in referential unit)
     */
    public double[] getInverseLocBound()
    {
        double[] bb = {this.ul_x, this.ul_y, this.lr_x, this.lr_y};
        return bb;
    }

    /** 
     * Get the inverse location referential
     * @return the inverse location referential
     */
    public String getInverseLocReferential()
    {
        return this.referential;
    }

    /**
     * Get the inverse location output folder
     * @return the inverse location output folder
     */
    public String getInverseLocOutputFolder()
    {
        return this.outputFolder;
    }
}