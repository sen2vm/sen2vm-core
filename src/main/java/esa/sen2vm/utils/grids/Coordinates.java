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

package esa.sen2vm.utils.grids;

import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

public class Coordinates
{
    private double x;
    private double y;
    private double z;
    private SpatialReference sourceSRS;
    private double latitude;
    private double longitude;
    private double altitude;

    /**
     * Constructor
     * @param x in epsg referencial
     * @param y in epsg referencial
     * @param epsg referencial
     * @throws Sen2VMException
     */
    public Coordinates(double x, double y, int epsg)
    {
        this.x = x;
        this.y = y;
        this.z = 0f;
        this.sourceSRS = new SpatialReference();
        this.sourceSRS.ImportFromEPSG(epsg);
    }

    /**
     * Get x
     * @return x in epsg referencial
     */
    public double getX()
    {
        return this.x;
    }

    /**
     * Get y
     * @return y in epsg referencial
     */
    public double getY()
    {
        return this.y;
    }

    /**
     * Get latitude
     * @return latitude in WGS84
     */
    public double getLatitude()
    {
        return this.latitude;
    }

    /**
     * Get longitude
     * @return longitude in WGS84
     */
    public double getLongitude()
    {
        return this.longitude;
    }

    /**
     * Transfrom (x, y, z) in epsg referencial to WGS84
     */
    public void transform()
    {
        SpatialReference targetSRS = new SpatialReference();
        targetSRS.ImportFromEPSG(4326);
        CoordinateTransformation transformer = new CoordinateTransformation(sourceSRS, targetSRS);
        double[] res = transformer.TransformPoint(x, y, z);
        this.longitude = res[1];
        this.latitude = res[0];
        this.altitude = res[2];
    }
}
