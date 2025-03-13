package esa.sen2vm.utils.grids;

import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import java.text.DecimalFormat;

public class Coordinates
{
    private float x;
    private float y;
    private float z;
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
    public Coordinates(float x, float y, int epsg)
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
    public float getX()
    {
        return this.x;
    }

    /**
     * Get y
     * @return y in epsg referencial
     */
    public float getY()
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
