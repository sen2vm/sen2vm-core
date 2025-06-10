package esa.sen2vm.output;

import java.util.Vector;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.io.FileReader;
import java.io.FileWriter;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Date;
import java.text.Format;
import java.text.SimpleDateFormat;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdal.BuildVRTOptions;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.input.Configuration;
import esa.sen2vm.input.Params;
import esa.sen2vm.input.SafeManager;
import esa.sen2vm.utils.Sen2VMConstants;
import esa.sen2vm.exception.Sen2VMException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.logging.Logger;

public class OutputFileManager
{
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(OutputFileManager.class.getName());
    protected Dataset dataset = null;
    protected Driver driver = null;

    /**
     * Constructor
     * @param filepath Path to the configuration file to parse
     */
    public OutputFileManager()
    {
        gdal.AllRegister();
    }

    /**
     * Save grid in 3D TIFF  (lon, lat, alt)
     * @param fileName path the file to save the grid in
     * @param startPixel pixel upper left
     * @param startLine line upper left
     * @param endLine line bottom left
     * @param stepX step of the grid in columns (metadata)
     * @param stepY step of the grid in rows (metadata)
     * @param bandVal 2d/3d for direct 2D : coordinate array as [[[lon00,lon01,...],[...]],[[lat00,lat01,,...], [...]]] in deg, deg
     *                      for direct : coordinate array as [[[lon00,lon01,...],[...]],[[lat00,lat01,,...], [...]],[[alt00,alt01,,...], [...]]] in deg, deg, m
     *                      for inverse : coordinate array as [[[col00,col01,,...]],[[row00,row01,...],[...]]] in pixel
     * @param src epsg
     * @param src epsgData (metatdata)
     * @param srs subLineOffset line offset of the grid (metadata)
     * @param lineOffset (metadata)
     * @param pixelOffset (metadata)
     * @param metadata true if export, false if not export
     */
     public void createGeoTiff(String fileName, double startPixel, double startLine,
            double stepX, double stepY, double[][][] bandVal, String epsg, String epsgData,
            double lineOffset, double pixelOffset, Boolean metadata)
    {

        int nbBand = bandVal.length;
        double[][] band1val = bandVal[0];
        int nbPixels = band1val[0].length;
        int nbLines = band1val.length;

        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        // String[] options = {"COMPRESS=LZW", "PREDICTOR=2"};

        Dataset ds = driver.Create(fileName, nbPixels, nbLines, nbBand, gdalconst.GDT_Float64);
        GdalGridFileInfo fileInfo = new GdalGridFileInfo();
        fileInfo.setDs(ds);

        if (metadata)
        {
            // Add metadata
            ds.SetMetadataItem("X_BAND", "1");
            ds.SetMetadataItem("Y_BAND", "2");
            if (nbBand == 3)
            {
                ds.SetMetadataItem("Z_BAND", "3");
            }
            ds.SetMetadataItem("PIXEL_OFFSET", String.valueOf(pixelOffset));
            ds.SetMetadataItem("PIXEL_STEP", String.valueOf(stepX));
            ds.SetMetadataItem("LINE_OFFSET", String.valueOf(lineOffset));
            ds.SetMetadataItem("LINE_STEP", String.valueOf(stepY));
            ds.SetMetadataItem("SRS", epsgData);
            ds.SetMetadataItem("GEOREFERENCING_CONVENTION", "CENTER_PIXEL");
            // ds.SetDescription("Direct Location Grid");
        }

        Vector<Band> bands = new Vector<Band>();
        for (int b = 0; b < nbBand; b++)
        {
            Band band = ds.GetRasterBand(b+1);
            fileInfo.setXBand(band);
            band.SetNoDataValue(Sen2VMConstants.noDataRasterValue);
            bands.add(band);
        }

        double[] gtInfo = getGeoTransformInfo(startPixel, stepX, startLine, stepY);
        ds.SetGeoTransform(gtInfo);
        ds.SetProjection(epsg);

        for (int b = 0; b < nbBand; b++)
        {
            for (int i = 0; i < nbLines; i++)
            {
                double[] band_online = new double[nbPixels];
                for (int j = 0; j < nbPixels; j++)
                {
                    band_online[j] = bandVal[b][i][j];
                }
                bands.get(b).WriteRaster(0, i, nbPixels, 1, band_online);
            }
            ds.GetRasterBand(b+1).FlushCache();
        }

        ds.FlushCache();

        for (int b = 0; b < nbBand; b++)
        {
            bands.get(b).delete();
        }
        ds.delete();

        LOGGER.info("Granule grid saved in: " + fileName);
    }


    /**
     * Create geotransform for the image
     * @param originX x-coordinate of the upper-left corner of the upper-left pixel.
     * @param gridXStep w-e pixel resolution / pixel width.
     * @param originY y-coordinate of the upper-left corner of the upper-left pixel.
     * @param gridYStep n-s pixel resolution / pixel height (negative value for a north-up image).
     */
    public double[] getGeoTransformInfo(double originX, double gridXStep, double originY, double gridYStep)
    {
        double[] gtInfo = new double[6];
        int idx = 0;
        gtInfo[idx++] = originX;
        gtInfo[idx++] = gridXStep;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = originY;
        gtInfo[idx++] = 0d;
        gtInfo[idx++] = gridYStep;
        return gtInfo;
    }

    /**
     * Build VRT from a list of input TIFFs and transform absolute to relative path from DS directory
     * @param vrtFilePath path of the VRT file to save to create
     * @param inputTIFs list of TIFFs
     * @param step of grid
     * @param lineOffset startLine
     * @param pixelOffset startPixel
     * @param export band3 (altitude case)
     */
    public void createVRT(String vrtFilePath, Vector<String> inputTIFs,  double step,
                            double lineOffset, double pixelOffset, boolean exportAlt)
    {
        // Create file tmp
        String vrtFilePath_tmp = vrtFilePath.substring(0, vrtFilePath.length() -4) + "_tmp" + Sen2VMConstants.VRT_EXTENSION;

        // Option code here
        final Vector<String> buildVRTOptions = new Vector<String>();
        // buildVRTOptions.add("-non_strict");

        // Start build VRT process
        gdal.AllRegister();
        final Dataset ds = gdal.BuildVRT(vrtFilePath_tmp, inputTIFs, new BuildVRTOptions(buildVRTOptions));

        // Add metadata
        ds.SetMetadataItem("X_BAND", "1"); // Longitude
        ds.SetMetadataItem("Y_BAND", "2"); // Latitude
        if (exportAlt)
        {
            ds.SetMetadataItem("Z_BAND", "3"); // Altitude
        }
        ds.SetMetadataItem("PIXEL_OFFSET", String.valueOf(pixelOffset));
        ds.SetMetadataItem("PIXEL_STEP", String.valueOf(step));
        ds.SetMetadataItem("LINE_OFFSET", String.valueOf(lineOffset));
        ds.SetMetadataItem("LINE_STEP", String.valueOf(step));
        ds.SetMetadataItem("SRS", "EPSG:4326");
        ds.SetMetadataItem("GEOREFERENCING_CONVENTION", "CENTER_PIXEL");
        ds.delete();
    }

    /**
     * Change GeoTransform stepY to -stepY and originY to -originY
     * @param inputTIFs list of TIFFs to change
     */
     public void correctGeoGrid(Vector<String> inputTIFs)
     {
        for(int g = 0; g < inputTIFs.size(); g++ )
        {
            Dataset ds = gdal.Open(inputTIFs.get(g), gdalconst.GA_Update);
            double[] transform = ds.GetGeoTransform();
            double[] gtInfo = getGeoTransformInfo(transform[0], transform[1], -transform[3], -transform[5]);
            ds.SetGeoTransform(gtInfo);
            ds.FlushCache();
            ds.delete();
       }
    }

    /**
     * Change absolute paths to relative paths from GRANULE dir
     * and change stepY to -stepY and originY to -originY
     * @param vrtFilePath path of the VRT file to correct
     */
    public void correctVRT(String vrtFilePath) throws Exception
    {
        String vrtFilePath_tmp = vrtFilePath.substring(0,vrtFilePath.length() -4) + "_tmp" + Sen2VMConstants.VRT_EXTENSION;

        // File reader
        File file = new File(vrtFilePath_tmp);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String s = br.readLine();

        // File writer
        File file_relative_path = new File(vrtFilePath);
        FileWriter fw = new FileWriter(file_relative_path);
        BufferedWriter bw = new BufferedWriter(fw);

        // Read file
        while(s != null)
        {
            // change the reference  of the path if SourceFilename
            if (s.contains("SourceFilename"))
            {
                int origin = s.lastIndexOf("GRANULE");
                s = "      <SourceFilename relativeToVRT=\"1\">../../../" + s.substring(origin);
            }
            if (s.contains("GeoTransform"))
            {
                String[] parts = s.split(",");
                double originYf = - Double.parseDouble(parts[3]);
                double stepYf = - Double.parseDouble(parts[5].split("<")[0]);
                s = parts[0] + "," + parts[1] + "," + parts[2];
                s = s + "," + String.valueOf(originYf) + "," + parts[4] + "," + stepYf + " </GeoTransform>";
            }
            bw.write(s);
            bw.newLine();
            s = br.readLine();
        }
        bw.flush();
        bw.close();
        br.close();
        file.delete();

        LOGGER.info("VRT saved in: " + vrtFilePath);
    }

    /**
     * Export all information used (from config and params) in a new json object
     * @param config object
     * @param params object
     * @return jsonobject
     */
    public JSONObject createConfig(Configuration config, Params params) throws IOException, Sen2VMException
    {
        JSONObject objJson = new JSONObject();

        // add info from config parameters
        objJson.put("l1b_product", config.getDatastripFilePath());
        objJson.put("gipp_folder", config.getGippFolder());
        objJson.put("gipp_version_check", config.getGippVersionCheck());
        objJson.put("dem", config.getDem());
        objJson.put("geoid", config.getGeoid());
        objJson.put("iers", config.getIers());
        objJson.put("operation", config.getOperation());
        objJson.put("deactivate_available_refining", config.getDeactivateRefining());

        JSONObject steps = new JSONObject();
        steps.put("10m_bands", config.getStepFromBandInfo(BandInfo.getBandInfoFromNameWithB("B02")));
        steps.put("20m_bands", config.getStepFromBandInfo(BandInfo.getBandInfoFromNameWithB("B05")));
        steps.put("60m_bands", config.getStepFromBandInfo(BandInfo.getBandInfoFromNameWithB("B01")));
        objJson.put("steps", steps);

        if (config.getOperation().equals(Sen2VMConstants.DIRECT))
        {
            objJson.put("export_alt", config.getExportAlt());
        }
        else
        {
            JSONObject inverse = new JSONObject();
            inverse.put("ul_x", config.getInverseLocBound()[0]);
            inverse.put("ul_y", config.getInverseLocBound()[1]);
            inverse.put("lr_x", config.getInverseLocBound()[2]);
            inverse.put("lr_y", config.getInverseLocBound()[3]);
            inverse.put("referential", config.getInverseLocReferential());
            inverse.put("output_folder", config.getInverseLocOutputFolder());
            objJson.put("inverse_location_additional_info", inverse);
        }

        // add info from param parameters
        List<DetectorInfo> detectors = params.getDetectorsList();
        List<String> detectors_list_json = new ArrayList<String>();
        for (DetectorInfo detectorInfo: detectors)
        {
            detectors_list_json.add(detectorInfo.getNameWithD());
        }

        List<BandInfo> bands = params.getBandsList();
        List<String> bands_list_json = new ArrayList<String>();
        for (BandInfo bandInfo: bands)
        {
            bands_list_json.add(bandInfo.getNameWithB());
        }

        JSONObject params_used = new JSONObject();
        params_used.put("detectors", detectors_list_json);
        params_used.put("bands", bands_list_json);
        objJson.put("params", params_used);
        return objJson;
    }

    /**
     * Write all information used (from config and params) in a json file in
     * output directory
     * @param config object
     * @param params object
     * @param output path dir
     */
    public void writeInfoJson(Configuration config, Params params, String output)  throws IOException, Sen2VMException
    {
        JSONObject objJson = createConfig(config, params);

        // Get date (string format)
        Format formatterDay = new SimpleDateFormat("YYYYMMdd");
        Format formatterTime = new SimpleDateFormat("HHmmss");
        String date = formatterDay.format(new Date()) + "T" + formatterTime.format(new Date());

        String configNameSave = "configuration" + "_" + date + Sen2VMConstants.JSON_EXTENSION;
        configNameSave = output + File.separator + configNameSave;

        // Write file
        FileWriter writer = new FileWriter(configNameSave, false);
        writer.write(objJson.toString());
        writer.close();
    }
}
