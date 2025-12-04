package esa.sen2vm.input;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.File;
import java.util.List;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.utils.Sen2VMConstants;
import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import esa.sen2vm.input.granule.Granule;
import esa.sen2vm.exception.Sen2VMException;

/**
 * Manager of the SAFE
 */
public class SafeManager
{
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(SafeManager.class.getName());

    /**
     * Directory of the datastrip
     */
    private File dirDataStrip;

    /**
     * List of all the granules found in dirGranules
     */
    private ArrayList<Granule> listGranules;


    /**
     * List of all the granule names found in dirGranules
     */
    private ArrayList<String> listGranuleNames;

    /**
     * Datastrip of the SAFE
     */
    private Datastrip datastrip;

    /**
     * option to overwrite grids
     */
    private Boolean overwrite_grids;

    /**
     * Constructor
     */
    public SafeManager(String path, DataStripManager dataStripManager, boolean overwrite_grids) throws Sen2VMException
    {
         // Inventory of the Datastrip
         String datastrip_path = path  + "/" + Sen2VMConstants.DATASTRIP;
         this.setAndProcessDataStrip(datastrip_path, dataStripManager);

         // Inventory of the Granules
         // Load all images and geo grid already existing (granule x det x band)
         String granules_path = path + "/" + Sen2VMConstants.GRANULE + "/";
         this.setAndProcessGranules(granules_path);
         this.overwrite_grids=overwrite_grids;
    }

    public SafeManager(String path, DataStripManager dataStripManager) throws Sen2VMException
    {
        this(path, dataStripManager,true);
    }

    /**
     * Take inventory of all granules with all images and geo info already existing
     * @param path directory contraing all granules
     */
    public void setAndProcessGranules(String path) throws Sen2VMException
    {
        this.listGranules = new ArrayList<Granule>();
        this.listGranuleNames = new ArrayList<String>();
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++)
            {
                if (listOfFiles[i].isDirectory())
                {
                    Granule gr = new Granule(listOfFiles[i]);
                    this.listGranules.add(gr);
                    this.listGranuleNames.add(gr.getName());
                }
            }
        }
    }

    /**
     * Create Datastrip with corresponding information
     * @param path directory the datastrip
     */
     public void setAndProcessDataStrip(String path, DataStripManager dataStripManager)
     {
        this.dirDataStrip = new File(path);

        File[] listOfFiles = this.dirDataStrip.listFiles();
        if(listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++)
            {
                if (listOfFiles[i].isDirectory())
                {
                    this.datastrip = new Datastrip(listOfFiles[i]);
                }
            }
        }
    }

    /**
     * Get datastrip
     * @return datastrip
     */
    public Datastrip getDatastrip()
    {
        return this.datastrip;
    }

    /**
     * List of granules which are on the detector with the existing band
     * @param detectorInfo
     * @param bandInfo
     * @return ArrayList<Granule>
     */
     public ArrayList<Granule> getGranulesToCompute(DetectorInfo detector, BandInfo band)
     {
        ArrayList<Granule> listGranulesToCompute = new ArrayList<Granule>();

        for(int g = 0; g < this.listGranules.size(); g++)
        {

            if (this.listGranules.get(g).getDetector().equals("D" + detector.getName()))
            {
                listGranulesToCompute.add(listGranules.get(g));
            }
        }
        return listGranulesToCompute;
    }

    /**
     * Compute Bounding Box in sensor grid of a datastrip
     * @param dataStripManager
     * @param detectorInfo
     * @param bandInfo
     * @return bounding box [uly, ulx, size lines, size pixels]
     */
    public int[] getFullSize(DataStripManager dataStripManager, BandInfo bandInfo, DetectorInfo detectorInfo)  throws Sen2VMException
    {
        String[] minmax = dataStripManager.getMinMaxGranule(bandInfo, detectorInfo,this.listGranuleNames);
        Granule minGranule = getGranuleByName(minmax[0]);
        Granule maxGranule = getGranuleByName(minmax[1]);

        if (minGranule == null || minGranule == null && !this.overwrite_grids)
        {
            Sen2VMException error = new Sen2VMException("Error GRANULE: no first or last granule of the datastrip.");
            throw error;
        }

        int[] ULpixel = minGranule.getULpixel(bandInfo.getPixelHeight());
        int[] BRpixel = maxGranule.getBRpixel(bandInfo.getPixelHeight());

        int[] bb = {ULpixel[0], ULpixel[1], BRpixel[0] - ULpixel[0], BRpixel[1] - ULpixel[1]};
        return bb;
    }

    /**
     * Get granule object of the corresponding granule name
     * @return granule
     */
     public Granule getGranuleByName(String name)
     {
        Granule res = null;
        for(int g = 0; g < this.listGranules.size(); g++)
        {
            if (this.listGranules.get(g).getName().equals(name))
            {
                res = this.listGranules.get(g);
            }
        }
        return res;
    }

    /**
     * Get granules list
     * @return granules
     */
     public ArrayList<Granule> getGranules()
     {
        return this.listGranules;
     }

    /**
     * Get all inverse grid from an output directory
     * @return grids[det][band]
     */
     public File[][] getInverseGrids(String outputDirPath)
     {
        File outputDir = new File(outputDirPath);
        File[][] inverseGrids = new File[Sen2VMConstants.NB_DETS][Sen2VMConstants.NB_BANDS];
        File[] listOfFiles = outputDir.listFiles();
        if(listOfFiles != null)
        {
            for (int i = 0; i < listOfFiles.length; i++)
            {
                if (listOfFiles[i].getName().contains(".")) {
                    String extension = listOfFiles[i].getName().substring(listOfFiles[i].getName().lastIndexOf("."), listOfFiles[i].getName().length());
                    if (listOfFiles[i].isFile() && extension.equals(Sen2VMConstants.TIFF_EXTENSION)) {
                        String[] name = listOfFiles[i].getName().substring(0, listOfFiles[i].getName().lastIndexOf(".")).split("_");

                        String bandName = name[name.length-1];
                        int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();

                        String detectorName = name[name.length-2].substring(1,3);
                        int indexDetector = DetectorInfo.getDetectorInfoFromName(detectorName).getIndex();
                        inverseGrids[indexDetector][indexBand] = listOfFiles[i];
                    }
                }
            }
        }
        return inverseGrids;
    }

    /**
     * Check if one grid (tif/vrt) in the wanted configs already exists
     * @param detectors and bands : list of configs to check
     * throw Sen2VMException if yes
     */
    public void testifDirectGridsToComputeAlreadyExist(List<DetectorInfo> detectors, List<BandInfo> bands) throws Sen2VMException
    {
        for (DetectorInfo detectorInfo: detectors)
        {
            for (BandInfo bandInfo: bands)
            {
                // Check grids by granules
                ArrayList<Granule> granulesToCompute = getGranulesToCompute(detectorInfo, bandInfo);
                for (Granule granuleToCompute: granulesToCompute)
                {
                    if (granuleToCompute.getGrid(bandInfo) != null && !this.overwrite_grids)
                    {
                        String error = "Direct grid(s) already exist(s)";
                        error = error + " (ex: " + detectorInfo.getNameWithD()  + "/" + bandInfo.getNameWithB() + ")";
                        throw new Sen2VMException(error);
                    }
                }

                // Check vrt
                File vrt = this.datastrip.getVRT(detectorInfo, bandInfo);
                if (vrt != null && !this.overwrite_grids)
                {
                    String error = "VRT grid(s) already exist(s)";
                    error = error + " (ex: " + detectorInfo.getNameWithD()  + "/" + bandInfo.getNameWithB() + ")";
                    throw new Sen2VMException(error);
                }

            }
        }
    }

    /**
     * Check if one inverse grid in the wanted configs already exists
     * @param detectors and bands : list of configs to check
     * throw Sen2VMException if yes
     */
    public void testifInverseGridsToComputeAlreadyExist(List<DetectorInfo> detectors, List<BandInfo> bands,
        String outputDirPath) throws Sen2VMException
    {
        for (BandInfo bandInfo: bands)
        {
            for (DetectorInfo detectorInfo: detectors)
            {
                String invFileName = datastrip.getCorrespondingInverseLocGrid(detectorInfo, bandInfo, outputDirPath);
                File f = new File(invFileName);
                if (f.exists() && !this.overwrite_grids)
                {
                    String error = "Inverse grid(s) already exist(s)";
                    error = error + " (ex: " + detectorInfo.getNameWithD()  + "/" + bandInfo.getNameWithB() + ")";
                    throw new Sen2VMException(error);
                }
            }
        }
    }
}