package esa.sen2vm.input;

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
import java.util.Arrays;

import esa.sen2vm.input.granule.GranuleManager;
import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.BandInfo;
import esa.sen2vm.utils.DetectorInfo;
import esa.sen2vm.utils.Sen2VMConstants;

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
     * Size of the granule for band res = 10
     */
    protected int[] sizeRes10 = null;

    /**
     * Size of the granule for band res = 20
     */
    protected int[] sizeRes20 = null;

    /**
     * Size of the granule for band res = 60
     */
    protected int[] sizeRes60 = null;

    /**
     * Constructor
     * @param path path to granule directory
     */
     public Granule(File path) throws Sen2VMException {
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
                    loadMTDinformations() ;
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

    /**
     * Load info of the granule MTD
     */
     private void loadMTDinformations() throws Sen2VMException {
        GranuleManager granuleManager = new GranuleManager(this.path_mtd.toString());
        pixelOrigin = granuleManager.getPixelOrigin();
        granulePosition = granuleManager.getGranulePosition() - pixelOrigin ;
        sizeRes10 = granuleManager.getSizeRes10();
        sizeRes20 = granuleManager.getSizeRes20();
        sizeRes60 = granuleManager.getSizeRes60();
    }



    /**
     * Load granule image by band in this.images[band]
     */
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
            info.put("granuleDimensions_nrows", this.sizeRes10[0]);
            info.put("granuleDimensions_ncols", this.sizeRes10[1]);
        } else if (BandInfo.values()[indexBand].getPixelHeight() == Sen2VMConstants.RESOLUTION_20M) {
            info.put("granuleDimensions_nrows", this.sizeRes20[0]);
            info.put("granuleDimensions_ncols", this.sizeRes20[1]);
        } else {
            info.put("granuleDimensions_nrows", this.sizeRes60[0]);
            info.put("granuleDimensions_ncols", this.sizeRes60[1]);
        }

        return info;
    }

    /*
     * Get geo grid file name for this granule and a specific band
     * return path file
     */
    public String getCorrespondingGeoFileName(BandInfo band) {
        File geo_data = new File(this.path + File.separator + "GEO_DATA");
        geo_data.mkdir();

        String image = this.images[band.getIndex()].getName();
        String grid = image.replace(".jp2", ".tif").replace("_MSI_", "_GEO_");
        return new File(geo_data.getPath() + File.separator + grid).getPath();
    }

    /*
     * Get bottom right pixel in sensor grid of the granule into a specific res
     * return bry, brx
     */
    public int[] getBRpixel(double resolution) {
        int[] pixel = null ;
        if (this.granulePosition == 1){
            if ((int) resolution == Sen2VMConstants.RESOLUTION_10M) {
                pixel = new int[]{ this.granulePosition + this.sizeRes10[0], this.sizeRes10[1]};
            } else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M) {
                    pixel = new int[]{ this.granulePosition + this.sizeRes20[0], this.sizeRes20[1]};
            } else {
                pixel = new int[]{ this.granulePosition + this.sizeRes60[0], this.sizeRes60[1]};

            }
        } else {
            if ((int) resolution == Sen2VMConstants.RESOLUTION_10M) {
                pixel = new int[]{ this.granulePosition + this.sizeRes10[0], this.sizeRes10[1]};
            } else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M) {
                pixel = new int[]{ this.granulePosition/2 + this.sizeRes20[0], this.sizeRes20[1]};
            } else {
                pixel = new int[]{ this.granulePosition/6+ this.sizeRes60[0], this.sizeRes60[1]};
            }
        }
        return pixel ;
    }

    /*
     * Get upper left pixel in sensor grid of the granule into a specific res
     * return uly, ulx
     */
    public int[] getULpixel(double resolution) {
        System.out.println("this origin");
        System.out.println(this.pixelOrigin);

       int[] pixel = null ;

        if (this.granulePosition == 1){
            pixel = new int[]{this.granulePosition - this.pixelOrigin, 0};
        } else {
            if ((int) resolution == Sen2VMConstants.RESOLUTION_10M) {
                pixel = new int[]{this.granulePosition, 0};
            } else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M) {
                pixel = new int[]{this.granulePosition / 2, 0};
            } else {
                pixel = new int[]{this.granulePosition/ 6, 0};
            }
        }
        return pixel ;
    }

    public int getFirstLine(double resolution) {
        int pixel = 0 ;
        if (this.granulePosition == this.pixelOrigin){
            pixel = this.granulePosition;
        } else {
            if ((int) resolution == Sen2VMConstants.RESOLUTION_10M) {
                pixel = this.granulePosition;
            } else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M) {
                pixel = this.granulePosition / 2;
            } else {
                pixel = this.granulePosition / 6;
            }
        }
        return pixel ;
    }

    public int getFirstPixel() {
        return this.pixelOrigin;
    }

    public int getSizePixels(double resolution) {
         int pixel ;
         if ((int) resolution == Sen2VMConstants.RESOLUTION_10M) {
            pixel = this.sizeRes10[1];
         } else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M) {
            pixel = this.sizeRes20[1];
         } else {
            pixel = this.sizeRes60[1];
        }
        return pixel;
    }

    public int  getSizeLines(double resolution) {
         int pixel ;
         if ((int) resolution == Sen2VMConstants.RESOLUTION_10M) {
            pixel = this.sizeRes10[0];
         } else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M) {
            pixel = this.sizeRes20[0];
         } else {
            pixel = this.sizeRes60[0];
        }
        return pixel ;
    }
}
