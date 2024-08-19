package esa.sen2vm;

import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import java.lang.Math;
import java.text.DecimalFormat;

public class Coordinates {
    private Float x;
    private Float y;
    private Float z;
    private SpatialReference sourceSRS;
    private Double latitude;
    private Double longitude;
    private Double altitude;

    public Coordinates(Float x, Float y, int epsg) {
        this.x = x;
        this.y = y;
        this.z = 0f;
        this.sourceSRS = new SpatialReference();
        this.sourceSRS.ImportFromEPSG(epsg);
    }

    public void transform() {

        SpatialReference targetSRS = new SpatialReference();
        targetSRS.ImportFromEPSG(4326);
        System.out.println(sourceSRS); // does get printed
        System.out.println(targetSRS); // doesn't get printed!?

        CoordinateTransformation transformer = new CoordinateTransformation(sourceSRS, targetSRS);
		double[] res = transformer.TransformPoint(x, y, z);
		this.latitude = Math.toRadians(res[1]);
		this.longitude = Math.toRadians(res[0]);
		this.altitude = Math.toRadians(res[2]);
    }

    public String geodetictoString() {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(17);
        df.setMinimumFractionDigits(14);
        return "(" + df.format(this.latitude) + ", " + df.format(this.longitude) + ", " + df.format(this.altitude) + ")";
    }


    public String toString() {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(17);
        df.setMinimumFractionDigits(14);
        return "(" + df.format(this.x) + ", " + df.format(this.y) + ", " + df.format(this.z) + ")";
    }

}