package esa.sen2vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;



public class SafeManager {

    private File dirGranules ;
    private File dirDataStrip ;
    private ArrayList<Granule> listGranules ;
    private Datastrip datastrip ;


    public SafeManager() {
    }

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

    public void checkEmptyGrid(String[] detectors,String[] bands) {
        for(int g = 0 ; g < this.listGranules.size() ; g++) {
            String det = this.listGranules.get(g).getDetector() ;
        }
    }

    public Datastrip getDatastrip() {
        return this.datastrip ;
    }

    public ArrayList<Granule> getGranulesToCompute(DetectorInfo detector, BandInfo band) {
        ArrayList<Granule> listGranulesToCompute = new ArrayList<Granule>() ;

        for(int g = 0 ; g < this.listGranules.size() ; g++) {

            if (this.listGranules.get(g).getDetector().equals("D" + detector.getName())){

                //if (this.listGranules.get(g).getImage(band).isFile()) {
                    listGranulesToCompute.add(listGranules.get(g)) ;
                //}
            }
        }
        return listGranulesToCompute ;
    }

}
