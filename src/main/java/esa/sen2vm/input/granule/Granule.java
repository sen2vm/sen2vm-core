package esa.sen2vm.input.granule;


import java.io.File;
import java.util.logging.Logger;

import esa.sen2vm.input.SafeManager;
import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.utils.Sen2VMConstants;

public class Granule
{
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
    private Integer pixelOrigin;

    /**
     * Pixel Origin of the granule (from granule metadata)
     */
    private Integer granulePosition;

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
    public Granule(File path) throws Sen2VMException
    {
        this.path = path;
        this.name = path.getName();

        String[] name_array = this.name.split("_");
        this.detector = name_array[name_array.length-2];
        LOGGER.info("Granule " + this.name + " (" + detector + ")");

        this.images = new File[13];
        this.grids = new File[13];

        File[] listOfFiles = this.path.listFiles();
        if(listOfFiles != null)
        {
            for (int p = 0; p < listOfFiles.length; p++)
            {
                if (listOfFiles[p].isDirectory())
                {
                    if (listOfFiles[p].getName().equals(Sen2VMConstants.IMG_DATA))
                    {
                        loadImages(listOfFiles[p]);
                    }
                    if (listOfFiles[p].getName().equals(Sen2VMConstants.GEO_DATA_GR))
                    {
                        loadGrids(listOfFiles[p]);

                    }
                }
                else if (listOfFiles[p].isFile())
                {
                    this.path_mtd = listOfFiles[p];
                    loadMTDinformations();
                }
            }
        }
    }

    /**
     * Get Granule Name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Get Granule path
     */
    public String getPath()
    {
        return this.path.toString();
    }


    /**
     * Get Granule MTD path
     */
    public String getPathMTD()
    {
        return this.path_mtd.toString();
    }

    /**
     * Get Detector Name
     */
    public String getDetector()
    {
        return this.detector;
    }

    /**
     * Load info of the granule MTD
     */
    private void loadMTDinformations() throws Sen2VMException
    {
        GranuleManager granuleManager = new GranuleManager(this.path_mtd.toString());
        pixelOrigin = granuleManager.getPixelOrigin();
        granulePosition = granuleManager.getGranulePosition() - this.pixelOrigin;
        sizeRes10 = granuleManager.getSizeRes10();
        sizeRes20 = granuleManager.getSizeRes20();
        sizeRes60 = granuleManager.getSizeRes60();
    }



    /**
     * Load granule image by band in this.images[band]
     */
    private void loadImages(File img_data)
    {
        File[] list_img = img_data.listFiles();
        for (int i = 0; i < list_img.length; i++)
        {
            String[] name = list_img[i].getName().substring(0, list_img[i].getName().lastIndexOf(".")).split("_");
            String bandName = name[name.length-1];
            int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();
            this.images[indexBand] = list_img[i];
        }
    }

     /*
     * Get image in images array by index of a specific band
     * return path file
     */
    private void loadGrids(File geo_data)
    {
        File[] list_img = geo_data.listFiles();
        int nbGranule = 0;
        for (int i = 0; i < list_img.length; i++)
        {
            String[] name = list_img[i].getName().substring(0, list_img[i].getName().lastIndexOf(".")).split("_");
            String bandName = name[name.length-1];

            if (bandName.length() == 3)
            {
                int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();
                // LOGGER.debug(list_img[i].getName());
                this.grids[indexBand] = list_img[i];
                nbGranule++;
            }
        }
        LOGGER.info(" --> Number of grids already existing: " + String.valueOf(nbGranule));
    }

     /*
     * Get image in images array by index of a specific band
     * return path file
     */
    public File getImage(BandInfo band)
    {
        String bandName = band.getName();
        int indexBand = BandInfo.getBandInfoFromNameWithB(bandName).getIndex();
        return this.images[indexBand];
    }

    /*
     * Get geo grid in geo grids array by index of a specific band
     * return path file
     */
    public File getGrid(String band)
    {
        int indexBand = BandInfo.valueOf(band).ordinal();
        return this.grids[indexBand];
    }

    /*
     * Get geo grids
     * return List path file
     */
    public File[] getGrids()
    {
        return this.grids;
    }

    /*
     * Get geo grid file name for this granule and a specific band
     * return path file
     */
    public String getCorrespondingGeoFileName(BandInfo band)
    {
        File geo_data = new File(this.path + File.separator + "GEO_DATA");
        geo_data.mkdir();

        String image = this.images[band.getIndex()].getName();
        String grid = image.replace(Sen2VMConstants.JP2_EXTENSION, Sen2VMConstants.TIFF_EXTENSION).replace("_MSI_", "_GEO_");
        return new File(geo_data.getPath() + File.separator + grid).getPath();
    }

    /*
     * Get bottom right pixel in sensor grid of the granule into a specific res
     * return bry, brx
     */
    public int[] getBRpixel(double resolution)
    {
        int[] pixel = null;
        pixel = new int[]{getFirstLine(resolution) + getSizeLines(resolution), getSizePixels(resolution)};
        return pixel;
    }

    /*
     * Get upper left coordinates in sensor grid of the granule in a specific res
     * return uly, ulx
     */
    public int[] getULpixel(double resolution)
    {
        int[] pixel = new int[]{getFirstLine(resolution), 0};
        return pixel;
    }

    /*
     * Get upper left line in sensor grid of the granule in a specific res
     * return uly
     */
    public int getFirstLine(double resolution)
    {
        int pixel;

        if ((int) resolution == Sen2VMConstants.RESOLUTION_10M)
        {
            pixel = this.granulePosition;
        }
        else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M)
        {
            pixel = this.granulePosition / 2;
        }
        else
        {
            pixel = this.granulePosition / 6;
        }
        return pixel;
    }

    /*
     * Get pixels number in sensor grid of the granule in a specific res
     * return sizex
     */
    public int getSizePixels(double resolution)
    {
        int sizex;
        if ((int) resolution == Sen2VMConstants.RESOLUTION_10M)
        {
            sizex = this.sizeRes10[1];
        }
        else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M)
        {
            sizex = this.sizeRes20[1];
        }
        else
        {
            sizex = this.sizeRes60[1];
        }
        return sizex;
    }

    /*
     * Get lines number in sensor grid of the granule in a specific res
     * return sizey
     */
    public int getSizeLines(double resolution)
    {
        int sizey;
        if ((int) resolution == Sen2VMConstants.RESOLUTION_10M)
        {
            sizey = this.sizeRes10[0];
        }
        else if ((int) resolution == Sen2VMConstants.RESOLUTION_20M)
        {
            sizey = this.sizeRes20[0];
        }
        else
        {
            sizey = this.sizeRes60[0];
        }
        return sizey;
    }
}
