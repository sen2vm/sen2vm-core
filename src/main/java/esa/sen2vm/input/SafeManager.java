package esa.sen2vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;



public class SafeManager {

    private File dirGranules ;
    private File dirDataStrip ;
    private ArrayList<Granule> listGranules ;
    private File mtdDatastrip ;


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
                    System.out.println("Directory " + listOfFiles[i].getName());
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
                if (listOfFiles[i].isDirectory() && listOfFiles[i].getName().equals("QI_DATA")) {
                    System.out.println("Directory QI_DATA");
                } else if (listOfFiles[i].isFile()) {
                    // DEBUG
                    this.mtdDatastrip = listOfFiles[i];
                }
            }
        }
    }

    public ArrayList<Granule> getGranulesToCompute(String detector) {
        ArrayList<Granule> listGranulesToCompute = new ArrayList<Granule>() ;

        for(int g = 0 ; g < this.listGranules.size() ; g++ ) {

            if (this.listGranules.get(g).getDetector() == detector){
                listGranulesToCompute.add(listGranules.get(g)) ;
            }

            /*if (this.listGranules.get(g).getDetector(detector, band) != null &&
                this.listGranules.get(g).getGrid(detector, band) == null) {
                listGranulesToCompute.add(listGranules.get(g)) ;
            }*/
        }
        return listGranulesToCompute ;

    }
}
