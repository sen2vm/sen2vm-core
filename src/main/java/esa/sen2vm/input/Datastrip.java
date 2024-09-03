package esa.sen2vm;

import org.apache.commons.cli.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Datastrip {

    private String name;
    private String detector;
    private File path;
    private File path_mtd;


    /**
     * Constructor
     * @param filepath Path of the datastrip directory
     */
    public Datastrip(File path) {
        this.path = path;
        this.name = path.getName() ;
        System.out.print("Datastrip " + this.name);

        // this.vrt = new File[];

        File[] listOfFiles = this.path.listFiles();
        if(listOfFiles != null) {
            for (int p = 0; p < listOfFiles.length; p++) {
                if (listOfFiles[p].isFile()) {
                    this.path_mtd = listOfFiles[p] ;
                    loadMTDinformations() ;
                }

                if (listOfFiles[p].getName().equals(Sen2VMConstants.GEO_DATA_DS)) {
                    // loadVRTs(listOfFiles[p]) ;
                    // TODO
                    System.out.println("GEO DATA EXISTS IN DS");
                }
            }
        }
    }

    /*
     * Get the name
     */
    public String getName() {
        return this.name;
    }

    private void loadMTDinformations() {

    }

    /**
     * Get name of the future vrt from a detector and a band in {Datastrip}/GEO_DATA (create it if none)
     * @param detector
     * @param band
     */
    public String getCorrespondingVRTFileName(DetectorInfo detector, BandInfo band) {
        File geo_data = new File(this.path + File.separator + "GEO_DATA");
        if(geo_data.mkdir()) {
            System.out.println("Already Existing");
        }

        String suffix = "_D" + detector.getName() + "_B" + band.getName2Digit() + ".vrt";
        String vrt = this.path_mtd.getName().replace(".xml", suffix).replace("_MTD_", "_GEO_");
        return new File(geo_data.getPath() + File.separator + vrt).getPath();

    }

}
