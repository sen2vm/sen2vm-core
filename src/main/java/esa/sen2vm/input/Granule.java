package esa.sen2vm;

import org.apache.commons.cli.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Granule {

    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(SafeManager.class.getName());

    /**
     * Name of the granule
     */
    private String name;

    /**
     * Name of the detector of the granule
     */
    private String detector;

    /**
     * Path of the granule
     */
    private File path;

    /**
     * File of xml metadata file
     */
    private File path_mtd;

    /**
     * List of the 13 images (by bands index) of the granule
     */
    private File[] images; // juste 1x13 bandes

     /**
     * List of the 13 geo grid (by bands index, if exists)
     */
     private File[] grids;

     /**
     * Pixel Origin of the granule (from granule metadata)
     */
    private Integer pixelOrigin ;

    /**
     * Pixel Origin of the granule (from granule metadata)
     */
    private Integer granulePosition ;

    /**
     * Dimensions in x for all the res {res 10, res 20, res 60}
     */
    private ArrayList<Integer> granuleDimensions_nrows ;

    /**
     * Dimensions in y for all the res {res 10, res 20, res 60}
     */
    private ArrayList<Integer> granuleDimensions_ncols ;

    /**
     * Constructor
     * @param path path to granule directory
     */
     public Granule(File path) {
        this.path = path;
        this.name = path.getName() ;
        System.out.print("Granule " + this.name);

        String[] name_array = this.name.split("_");
        this.detector = name_array[name_array.length-2];
        System.out.println(" (" + detector + ")");

        this.images = new File[13];
        this.grids = new File[13];

        File[] listOfFiles = this.path.listFiles();
        if(listOfFiles != null) {
            for (int p = 0; p < listOfFiles.length; p++) {
                if (listOfFiles[p].isDirectory() ) {
                    if (listOfFiles[p].getName().equals(Sen2VMConstants.IMG_DATA)) {
                        loadImages(listOfFiles[p]) ;
                    }
                    if (listOfFiles[p].getName().equals(Sen2VMConstants.GEO_DATA_GR)) {
                        loadGrids(listOfFiles[p]) ;
                    }
                } else if (listOfFiles[p].isFile()) {
                    this.path_mtd = listOfFiles[p] ;
                    // loadMTDinformations() ;
                }
            }
        }
    }

    /**
     * Get Granule Name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get Detector Name
     */
    public String getDetector() {
        return this.detector;
    }

    private void loadMTDinformations() throws Sen2VMException {
        // [TODO]
        GranuleManager granuleManager = GranuleManager.getInstance();
        granuleManager.initGranuleManager(this.path_mtd.toString());
    }

    private void loadImages(File img_data) {
        File[] list_img = img_data.listFiles();
        for (int i = 0; i < list_img.length; i++) {
            String[] name = list_img[i].getName().substring(0, list_img[i].getName().lastIndexOf(".")).split("_");
            String bandName = name[name.length-1];
            int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();
            this.images[indexBand] = list_img[i] ;
        }
    }

     /*
     * Get image in images array by index of a specific band
     * return path file
     */
     private void loadGrids(File geo_data) {
        File[] list_img = geo_data.listFiles();
        System.out.println(" --> Number of grids already existing: " + String.valueOf(list_img.length));
        for (int i = 0; i < list_img.length; i++) {
            String[] name = list_img[i].getName().substring(0, list_img[i].getName().lastIndexOf(".")).split("_");
            String bandName = name[name.length-1];
            int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();
            this.images[indexBand] = list_img[i] ;
        }
    }

     /*
     * Get image in images array by index of a specific band
     * return path file
     */
     public File getImage(BandInfo band) {
        String bandName = band.getName();
        int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();
        return this.images[indexBand];
    }

    /*
     * Get geo grid in geo grids array by index of a specific band
     * return path file
     */
     public File getGrid(String band) {
        int indexBand = BandInfo.valueOf(band).ordinal();
        return this.grids[indexBand];
    }

    /*
     * Get geo grid file name for this granule and a specific band
     * return path file
     */
    public Map<String, Object> infoForGridDirectLocation(String detector, String band) {
        Map<String, Object> info = new HashMap<>();
        info.put("pixelOrigin", this.pixelOrigin);
        info.put("granulePosition", this.granulePosition);
        int indexBand = BandInfo.valueOf(band).ordinal();
        if (BandInfo.values()[indexBand].getPixelHeight() == Sen2VMConstants.RESOLUTION_10M) {
            info.put("granuleDimensions_nrows", this.granuleDimensions_nrows.get(0));
            info.put("granuleDimensions_ncols", this.granuleDimensions_ncols.get(0));
        } else if (BandInfo.values()[indexBand].getPixelHeight() == Sen2VMConstants.RESOLUTION_20M) {
            info.put("granuleDimensions_nrows", this.granuleDimensions_nrows.get(1));
            info.put("granuleDimensions_ncols", this.granuleDimensions_ncols.get(1));
        } else {
            info.put("granuleDimensions_nrows", this.granuleDimensions_nrows.get(2));
            info.put("granuleDimensions_ncols", this.granuleDimensions_ncols.get(2));
        }

        return info;
    }

    /*
     * Get geo grid file name for this granule and a specific band
     * return path file
     */
    public String getCorrespondingGeoFileName(BandInfo band) {
        File geo_data = new File(this.path + File.separator + "GEO_DATA");
        if(geo_data.mkdir()) {
            System.out.println("Already Existing");
        }

        String image = this.images[band.index].getName();
        String grid = image.replace(".jp2", ".tif").replace("_MSI_", "_GEO_");
        return new File(geo_data.getPath() + File.separator + grid).getPath();
    }

}
