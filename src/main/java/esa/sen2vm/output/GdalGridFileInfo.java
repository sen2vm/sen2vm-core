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

package esa.sen2vm.output;

import java.util.ArrayList;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;

/**
 * Store info when writing a raster using gdal
 */
public class GdalGridFileInfo
{
    /**
     * x band
     */
    protected Band xBand = null;
    /**
     * z band
     */
    protected Band yBand = null;
    /**
     * z band
     */
    protected Band zBand = null;

    /**
     * Dataset
     */
    protected Dataset ds = null;

    /**
     * Constructor
     */
    public GdalGridFileInfo()
    {
    }

    /**
     * @return the xBand
     */
    public Band getXBand()
    {
        return xBand;
    }

    /**
     * @return the yBand
     */
    public Band getYBand()
    {
        return yBand;
    }

    /**
     * @return the yBand
     */
    public Band getZBand()
    {
        return zBand;
    }

    /**
     * @return the ds
     */
    public Dataset getDs()
    {
        return ds;
    }

    /**
     * @param xBand the xBand to set
     */
    public void setXBand(Band xBand)
    {
        this.xBand = xBand;
    }

    /**
     * @param yBand the yBand to set
     */
    public void setYBand(Band yBand)
    {
        this.yBand = yBand;
    }

    /**
     * @param yBand the yBand to set
     */
    public void setZBand(Band zBand)
    {
        this.zBand = zBand;
    }

    /**
     * @param ds the ds to set
     */
    public void setDs(Dataset ds)
    {
        this.ds = ds;
    }

    /**
     * @return the bandList
     */
    public ArrayList<Band> getBandList()
    {
        ArrayList<Band> bandList = new ArrayList<>();
        if (xBand != null)
        {
            bandList.add(xBand);
        }
        if (yBand != null)
        {
            bandList.add(yBand);
        }
        if (zBand != null)
        {
            bandList.add(zBand);
        }
        return bandList;
    }
}