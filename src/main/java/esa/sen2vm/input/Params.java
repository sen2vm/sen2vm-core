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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.Sen2VMConstants;

/**
 * Read the parameters file
 */
public class Params extends InputFileManager
{
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(Params.class.getName());

    /**
     * JSONArray of the detectors
     */
    public List<DetectorInfo> detectors;

    /**
     * JSONArray of the bands
     */
    public List<BandInfo>  bands;


    /**
     * Constructor
     * @param configPath path to the JSON configuration file
     * @throws Sen2VMException
     */
    public Params(CommandLine commandLine) throws Sen2VMException
    {
        // Read detectors
        this.detectors = new ArrayList<DetectorInfo>();
        if (commandLine.hasOption(OptionManager.OPT_DETECTORS_LIST_SHORT))
        {
            String[] detectorsList = commandLine.getOptionValues(OptionManager.OPT_DETECTORS_LIST_SHORT);

            if (detectorsList.length > 12)
            {
                LOGGER.severe("Maximum number of detectors is 12 (" + detectorsList.length + ")");
                throw new Sen2VMException("Maximum number of detectors is 12 (" + detectorsList.length + ")");
            } 
            for (String detector: detectorsList)
            {
                this.detectors.add(DetectorInfo.getDetectorInfoFromName(detector));
            }
        }

        // Read bands
        this.bands = new ArrayList<BandInfo>();
        if (commandLine.hasOption(OptionManager.OPT_BANDS_LIST_SHORT))
        { 
            String[] bandsList = commandLine.getOptionValues(OptionManager.OPT_BANDS_LIST_SHORT);

            if (bandsList.length > 13)
            {
                LOGGER.severe("Maximum number of detectors is 13 (" + bandsList.length + ")");
                throw new Sen2VMException("Maximum number of detectors is 13 (" + bandsList.length + ")");
            } 
            for (String band: bandsList)
            {
                this.bands.add(BandInfo.getBandInfoFromNameWithB(band));
            }
        } 
    } 

    /**
     * Constructor
     * @param paramPath path to the JSON parameters file
     * @throws Sen2VMException
     */
    public Params(String paramPath) throws Sen2VMException
    {
        InputStream schemaParamStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(Sen2VMConstants.JSON_SCHEMA_PARAMS);
        
        if (schemaParamStream == null)
        {
           throw new Sen2VMException("Impossible to find the JSON schema for parameters file: " + Sen2VMConstants.JSON_SCHEMA_PARAMS);
        }

        if(check_schema(paramPath, schemaParamStream))
        {
            parse(paramPath);
        }
    }

    /**
     * Parse JSON parameters file
     * @param jsonFilePath path to the JSON parameters file
     * @throws Sen2VMException
     */
    public void parse(String jsonFilePath) throws Sen2VMException
    {
        LOGGER.info("Parsing file " + jsonFilePath);
        try (InputStream fis = new FileInputStream(jsonFilePath))
        {
            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));
            
            // Load detectors
            JSONArray detectorsList = jsonObject.getJSONArray("detectors");
            this.detectors = new ArrayList<DetectorInfo>();
            for(int i=0; i < detectorsList.length(); i++)
            {
                this.detectors.add(DetectorInfo.getDetectorInfoFromName(detectorsList.getString(i)));
            }

            //Load bands
            JSONArray bandList = jsonObject.getJSONArray("bands");            
            this.bands = new ArrayList<BandInfo>();
            for(int i=0; i<bandList.length(); i++)
            {
               this.bands.add(BandInfo.getBandInfoFromNameWithB(bandList.getString(i)));
            }
        }
        catch (JSONException | IOException e)
        {
            throw new Sen2VMException("Problem while reading JSON parameters file" + jsonFilePath + " : ", e);
        }
    }

    /**
     * Get the detectors list
     * @return the detectors list
     */
    public List<DetectorInfo> getDetectorsList()
    {
       return this.detectors;
    }

    /**
     * Get the bands list
     * @return the bands list
     */
    public List<BandInfo> getBandsList()
    {
       return this.bands;
    }
}