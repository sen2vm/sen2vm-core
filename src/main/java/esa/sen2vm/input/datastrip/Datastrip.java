package esa.sen2vm.input.datastrip;

import java.io.File;
import java.util.logging.Logger;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.utils.Sen2VMConstants;

public class Datastrip
{
    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(Datastrip.class.getName());

    private String name;
    private File path;
    private File path_mtd;

    /**
     * List of the vrt (by [dets index] and [bands index]) of the geo grid
     */
    private File[][] vrts;

    /**
     * Constructor
     * @param filepath Path of the datastrip directory
     */
    public Datastrip(File path)
    {
        this.path = path;
        this.name = path.getName();
        LOGGER.info("Datastrip " + this.name);

        this.vrts = new File[Sen2VMConstants.NB_DETS][Sen2VMConstants.NB_BANDS];

        File[] listOfFiles = this.path.listFiles();
        if(listOfFiles != null)
        {
            for (int p = 0; p < listOfFiles.length; p++)
            {
                if (listOfFiles[p].getName().equals(Sen2VMConstants.GEO_DATA_DS))
                {
                    loadVRTs(listOfFiles[p]);
                }
                else if (listOfFiles[p].isFile())
                {
                    this.path_mtd = listOfFiles[p];
                }
            }
        }
    }

    /**
     * Load all VRT in vrts list by dectector/band indices in directory
     * @param directory
     */
    private void loadVRTs(File directory)
    {
        File[] list_img = directory.listFiles();
        for (int i = 0; i < list_img.length; i++)
        {
            if (list_img[i].getName().endsWith(".vrt")) {
                String[] name = list_img[i].getName().substring(0, list_img[i].getName().lastIndexOf(".")).split("_");
                String bandName = name[name.length-1];
                System.out.println(list_img[i].getName());
                int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();
                String detectorName = name[name.length-2].substring(1);
                int indexDetector = Integer.valueOf(detectorName);
                this.vrts[indexDetector-1][indexBand] = list_img[i];
            }

        }
    }

    /*
     * Get the name
     */
    public String getName()
    {
        return this.name;
    }

    /*
     * Get the path
     */
    public File getPath()
    {
        return this.path;
    }

    /*
     * Get the name
     */
    public File[][] getVRT()
    {
        return this.vrts;
    }

    /**
     * Get name of the future vrt from a detector and a band in {Datastrip}/GEO_DATA (create it if none)
     * @param detector
     * @param band
     */
    public String getCorrespondingVRTFileName(DetectorInfo detector, BandInfo band)
    {
        File geo_data = new File(this.path + File.separator + Sen2VMConstants.GEO_DATA_DS);
        geo_data.mkdir();

        String suffix = "_D" + detector.getName() + "_B" + band.getName2Digit() + Sen2VMConstants.VRT_EXTENSION;
        String vrt = this.path_mtd.getName().replace(".xml", suffix).replace("_MTD_", "_GEO_");
        return new File(geo_data.getPath() + File.separator + vrt).getPath();

    }


    /**
     * Get corresponding inverse loc grid file name by band/detector in output dir
     * @param detector
     * @param band
     */
    public String getCorrespondingInverseLocGrid(DetectorInfo detector, BandInfo band, String outputDir)
    {
        File geo_data = new File(outputDir);
        geo_data.mkdir();

        String suffix = "_D" + detector.getName() + "_B" + band.getName2Digit() + Sen2VMConstants.TIFF_EXTENSION;
        String name = this.path_mtd.getName().replace(".xml", suffix).replace("_MTD_", "_INV_");
        return new File(geo_data.getPath() + File.separator + name).getPath();

    }


}
