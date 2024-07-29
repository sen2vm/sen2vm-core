package esa.sen2vm;

import org.apache.commons.cli.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Granule {

    private String name;
    private String detector;
    private File path;
    private File path_mtd;

    private File[] images; // juste 1x13 bandes
    private File[] grids;

    private Integer pixelOrigin ;
    private Integer granulePosition ;
    private ArrayList<Integer> granuleDimensions_nrows ;
    private ArrayList<Integer> granuleDimensions_ncols ;

    // constructor
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
                    if (listOfFiles[p].getName().equals("IMG_DATA")) {
                        loadImages(listOfFiles[p]) ;
                    }
                    if (listOfFiles[p].getName().equals("GEO_DATA")) {
                        loadGrids(listOfFiles[p]) ;
                    }

                } else if (listOfFiles[p].isFile()) {
                    this.path_mtd = listOfFiles[p] ;
                    loadMTDinformations() ;
                }
            }
        }
        System.out.println();

    }

    public String getName() {
        return this.name;
    }

    public String getDetector() {
        return this.detector;
    }

    private void loadMTDinformations() {
        // [TODO]
        this.pixelOrigin = 1 ;
        this.granulePosition = 100 ;
        this.granuleDimensions_nrows = new ArrayList<Integer>() {{add(50);add(50);add(50);}};
        this.granuleDimensions_ncols = new ArrayList<Integer>(){{add(100);add(100);add(100);}};
    }

    private void loadImages(File img_data) {
        File[] list_img = img_data.listFiles();
        for (int i = 0; i < list_img.length; i++) {
            String[] name = list_img[i].getName().substring(0, list_img[i].getName().lastIndexOf(".")).split("_");
            String band = name[name.length-1];
            int indexBand = BandS2.valueOf(band).ordinal();
            System.out.println(band + ": " + list_img[i]);
            this.images[indexBand] = list_img[i] ;
        }
    }

    private void loadGrids(File img_data) {
        File[] list_img = img_data.listFiles();
        System.out.println("Number of grids already existing:" + String.valueOf(list_img.length));

        for (int i = 0; i < list_img.length; i++) {
            String[] name = list_img[i].getName().substring(0, list_img[i].getName().lastIndexOf(".")).split("_");
            String band = name[name.length-1];
            int indexBand = BandS2.valueOf(band).ordinal();
            System.out.println(detector + "/" + band + ": " + list_img[i]);
            this.images[indexBand] = list_img[i] ;
        }
    }

    public File getImage(String detector, String band) {
        int indexBand = BandS2.valueOf(band).ordinal();
        int indexDetector = DetectorS2.valueOf(detector).ordinal();
        return this.images[indexBand];
    }

    public File getGrid(String detector, String band) {
        int indexBand = BandS2.valueOf(band).ordinal();
        int indexDetector = DetectorS2.valueOf(detector).ordinal();
        return this.grids[indexBand];
    }

    public Map<String, Object> infoForGridDirectLocation(String detector, String band) {
        Map<String, Object> info = new HashMap<>();
        info.put("pixelOrigin", this.pixelOrigin);
        info.put("granulePosition", this.granulePosition);
        int indexBand = BandS2.valueOf(band).ordinal();
        if (BandS2.values()[indexBand].getResolution() == 10) {
            info.put("granuleDimensions_nrows", this.granuleDimensions_nrows.get(0));
            info.put("granuleDimensions_ncols", this.granuleDimensions_ncols.get(0));
        } else if (BandS2.values()[indexBand].getResolution() == 20) {
            info.put("granuleDimensions_nrows", this.granuleDimensions_nrows.get(1));
            info.put("granuleDimensions_ncols", this.granuleDimensions_ncols.get(1));
        } else {
            info.put("granuleDimensions_nrows", this.granuleDimensions_nrows.get(2));
            info.put("granuleDimensions_ncols", this.granuleDimensions_ncols.get(2));
        }

        return info;
     }


}
