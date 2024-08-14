package esa.sen2vm;
import java.util.ArrayList;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;

/**
 * Store info when writing a raster using gdal
 */
public class GdalGridFileInfo {
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
    public GdalGridFileInfo() {
    }

    /**
     * @return the xBand
     */
    public Band getXBand() {
        return xBand;
    }

    /**
     * @return the yBand
     */
    public Band getYBand() {
        return yBand;
    }

    /**
     * @return the yBand
     */
    public Band getZBand() {
        return zBand;
    }

    /**
     * @return the ds
     */
    public Dataset getDs() {
        return ds;
    }

    /**
     * @param xBand the xBand to set
     */
    public void setXBand(Band xBand) {
        this.xBand = xBand;
    }

    /**
     * @param yBand the yBand to set
     */
    public void setYBand(Band yBand) {
        this.yBand = yBand;
    }

    /**
     * @param yBand the yBand to set
     */
    public void setZBand(Band zBand) {
        this.zBand = zBand;
    }

    /**
     * @param ds the ds to set
     */
    public void setDs(Dataset ds) {
        this.ds = ds;
    }

    /**
     * @return the bandList
     */
    public ArrayList<Band> getBandList() {
        ArrayList<Band> bandList = new ArrayList<>();
        if (xBand != null) {
            bandList.add(xBand);
        }
        if (yBand != null) {
            bandList.add(yBand);
        }
        if (zBand != null) {
            bandList.add(zBand);
        }
        return bandList;
    }
}
