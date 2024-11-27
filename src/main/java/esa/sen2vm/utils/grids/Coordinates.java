package esa.sen2vm.utils.grids;

import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import java.text.DecimalFormat;

public class Coordinates
{
    private Float x;
    private Float y;
    private Float z;
    private SpatialReference sourceSRS;
    private Double latitude;
    private Double longitude;
    private Double altitude;

    public Coordinates(Float x, Float y, int epsg)
    {
        this.x = x;
        this.y = y;
        this.z = 0f;
        this.sourceSRS = new SpatialReference();
        this.sourceSRS.ImportFromEPSG(epsg);
    }

    public Float getX()
    {
        return this.x;
    }

    public Float getY()
    {
        return this.y;
    }

    public Double getLatitude()
    {
        return this.latitude;
    }

    public Double getLongitude()
    {
        return this.longitude;
    }

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

    public String geodetictoString()
    {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(17);
        df.setMinimumFractionDigits(14);
        return "(" + df.format(this.latitude) + ", " + df.format(this.longitude) + ", " + df.format(this.altitude) + ")";
    }

    public String toString()
    {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(17);
        df.setMinimumFractionDigits(14);
        return "(" + df.format(this.x) + ", " + df.format(this.y) + ", " + df.format(this.z) + ")";
    }
}
