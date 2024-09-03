package esa.sen2vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SafeManager {

    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(SafeManager.class.getName());

    /**
     * Directory containing all the granules
     */
    private File dirGranules ;

    /**
     * Directory of the datastrip
     */
    private File dirDataStrip ;

    /**
     * List of all the granules found in dirGranules
     */
    private ArrayList<Granule> listGranules ;


    /**
     * Datastrip of the SAFE
     */
    private Datastrip datastrip ;

    /**
     * Constructor
     */
    public SafeManager() {
    }

    /**
     * Take inventory of all granules with all images and geo info already existing
     * @param path directory contraing all granules
     */
    public void setAndProcessGranules(String path) {
        this.listGranules = new ArrayList<Granule>() ;
        this.dirGranules = new File(path) ;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isDirectory()) {
                    Granule gr = new Granule(listOfFiles[i]) ;
                    listGranules.add(gr);
                }
            }
        }
    }

    /**
     * Create Datastrip with corresponding information
     * @param path directory the datastrip
     */
     public void setAndProcessDataStrip(String path) {
        this.dirDataStrip = new File(path);

        File[] listOfFiles = this.dirDataStrip.listFiles();
        if(listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isDirectory()) {
                    this.datastrip = new Datastrip(listOfFiles[i]) ;
                }
            }
        }
    }

    /**
     * Get datastrip
     * @return datastrip
     */
    public Datastrip getDatastrip() {
        return this.datastrip ;
    }

    /**
     * List of granules which are on the detector with the existing band
     * TODO test grid
     * @return ArrayList<Granule>
     */
     public ArrayList<Granule> getGranulesToCompute(DetectorInfo detector, BandInfo band) {
        ArrayList<Granule> listGranulesToCompute = new ArrayList<Granule>() ;

        for(int g = 0 ; g < this.listGranules.size() ; g++) {

            if (this.listGranules.get(g).getDetector().equals("D" + detector.getName())){

                //if (this.listGranules.get(g).getImage(band).isFile()) { TODO
                    listGranulesToCompute.add(listGranules.get(g)) ;
                //}
            }
        }
        return listGranulesToCompute ;
    }

}
