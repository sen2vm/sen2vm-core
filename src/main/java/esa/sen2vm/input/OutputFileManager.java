package esa.sen2vm;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import java.io.File;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.gdal.gdal.BuildVRTOptions;

public class OutputFileManager
{
    protected Dataset dataset = null;
    protected Driver driver = null;
    protected Double noDataRasterValue = Double.NaN;


    /**
     * Constructor
     * @param filepath Path to the configuration file to parse
     */
    public OutputFileManager() {
        gdal.AllRegister();
    }

    public void create_archi() {
        // File dossier = new File(this.filepath + File.separator + "dir");
        //boolean res = dossier.mkdir();
    }


    public void createGeoTiff(String fileName, int startPixel, int startLine,
            float step, int nbBand, String srs, double[][][] bandVal) {

        double[][] band1val = bandVal[0];
        double[][] band2val = bandVal[1];
        int nbPixels = band1val[0].length ;
        int nbLines = band1val.length ;

        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        Dataset ds;
        Band band1;
        Band band2;

        ds = driver.Create(fileName, nbPixels, nbLines, nbBand, gdalconst.GDT_Int32);

        GdalGridFileInfo fileInfo = new GdalGridFileInfo();
        fileInfo.setDs(ds);

        band1 = ds.GetRasterBand(1);
        fileInfo.setXBand(band1);
        band1.SetNoDataValue(noDataRasterValue);

        band2 = ds.GetRasterBand(2);
        fileInfo.setYBand(band2);
        band2.SetNoDataValue(noDataRasterValue);


        double[] gtInfo = getGeoTransformInfo(startPixel, step, startLine, step) ;
        ds.SetGeoTransform(gtInfo);
        ds.SetProjection(srs);

        for (int i = 0; i < nbLines; i++) {
            double[] band1_online = new double[nbPixels];
            double[] band2_online = new double[nbPixels];

            for (int j = 0; j < nbPixels; j++) {
                band1_online[j] = band1val[i][j];
                band2_online[j] = band2val[i][j];
            }

            band1.WriteRaster(0, i, nbPixels, 1, band1_online);
            band2.WriteRaster(0, i, nbPixels, 1, band2_online);
        }

        ds.GetRasterBand(1).FlushCache();
        ds.GetRasterBand(2).FlushCache();
        close(ds, band1, band2);

        System.out.println("Tiff saved in:" + fileName);
    }


    protected void close(Dataset ds, Band pixelBand, Band lineBand) {
        pixelBand.delete();
        lineBand.delete();
        ds.delete();
    }



    public double[] getGeoTransformInfo(int pixelStartIndex, float gridXStep, int upperLine, float gridYStep)  {

        double[] gtInfo = new double[6];
        int idx = 0;
        gtInfo[idx++] = pixelStartIndex;
        gtInfo[idx++] = gridXStep;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = upperLine;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = -gridYStep;

        return gtInfo;
    }

    public void createVRT(String vrtFilePath, Vector<String> inputVRTs){

        final Vector<String> buildVRTOptions = new Vector<String>();

        // Option code here
        // buildVRTOptions.add("-te");
        // buildVRTOptions.add("start granule"); // surement deja fait

        gdal.AllRegister();
        final Dataset dataset = gdal.BuildVRT(vrtFilePath, inputVRTs, new BuildVRTOptions( buildVRTOptions));
        dataset.delete();
    }


}
