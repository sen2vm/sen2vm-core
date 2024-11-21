package esa.sen2vm;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import java.io.File;
import java.nio.file.Path;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Float;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.ConfigurationFile;
import esa.sen2vm.utils.Sen2VMConstants;

import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import esa.sen2vm.input.granule.GranuleManager;
import esa.sen2vm.input.granule.Granule;
import esa.sen2vm.input.gipp.GIPPManager;
import esa.sen2vm.input.SafeManager;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.gdal.gdal.BuildVRTOptions;

import esa.sen2vm.utils.BandInfo;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


/**
 * Unit test for Sen2VM.
 */
public class Sen2VMTest
{
    /**
     * Functional test
     */
    @Test
    public void readConfigurationFile()
    {
        try {
            // Read configuration file
            ConfigurationFile configFile = new ConfigurationFile("src/test/resources/configuration_example.json");
            System.out.println("Datastrip file path: " + configFile.getDem() + "\nIERS bulletin path: "+ configFile.getIers() + "\nboolean refining: " + configFile.getBooleanRefining());


        }
        catch (Sen2VMException e) {
            e.printStackTrace();
        }
    }

    public String config(String filePath, String l1b_product, int step, String operation, boolean refining) throws FileNotFoundException,
            IOException, ParseException {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        objJson.put("operation", operation);
        objJson.put("deactivate_available_refining", refining);

        JSONObject steps = (JSONObject) objJson.get("steps");
        steps.put("10m_bands", (int) step / 10);
        steps.put("20m_bands", (int) step / 20);
        steps.put("60m_bands", (int) step / 60);

        String outputConfig = "src/test/resources/tests/config/configuration_tmp.json";
        FileWriter writer = new FileWriter(outputConfig, false); //overwrites the content of file
        writer.write(obj.toString());
        writer.close();

        return outputConfig;

    }

    public String configSuppIERS(String filePath, String l1b_product) throws FileNotFoundException,
            IOException, ParseException {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONObject objJson = (JSONObject) obj;
        objJson.put("l1b_product", l1b_product);
        objJson.remove("iers");

        String outputConfig = "src/test/resources/tests/config/configuration_tmp.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;

    }

    public String createTestDir(String nameTest) throws IOException {
        String inputRef = "/Sen2vm/sen2vm-core/src/test/resources/TDS1_SmallIsland/L1B_test";
        String outputRef = "/Sen2vm/sen2vm-core/src/test/resources/tests/output/" + nameTest ;
        copyFolder(new File(inputRef), new File(outputRef), true);
        return outputRef ;
    }





    public void verifyStepDirectLoc(String configFilepath, int step) throws Sen2VMException {

        ConfigurationFile configFile = new ConfigurationFile(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), configFile.getBooleanRefining());
        SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);

        ArrayList<Granule> granules = sm.getGranules();

        for(int g = 0 ; g < granules.size() ; g++) {
            File[] grids = granules.get(g).getGrids();

            int b = 0;
            for (File grid : grids) {
                if (grid != null) {
                    double res = BandInfo.getBandInfoFromIndex(b).getPixelHeight();
                    Dataset ds = gdal.Open(grid.getPath());
                    double[] transform = ds.GetGeoTransform();
                    assertEquals(transform[1] * res, step);
                    assertEquals(transform[5] * res, step);
                    ds.delete();
                }
                b = b + 1;
            }
        }
    }

    public void verifyDirectLoc(String configFilepath, String outputRef) throws Sen2VMException, IOException {

        ConfigurationFile configFile = new ConfigurationFile(configFilepath);
        DataStripManager dataStripManager = new DataStripManager(configFile.getDatastripFilePath(), configFile.getIers(), configFile.getBooleanRefining());
        SafeManager sm = new SafeManager(configFile.getL1bProduct(), dataStripManager);

        ArrayList<Granule> granules = sm.getGranules();

        for(int g = 0 ; g < granules.size() ; g++) {
            File[] grids = granules.get(g).getGrids();

            int b = 0;
            for (File grid : grids) {
                if (grid != null) {
                    int len = grid.toPath().getNameCount();
                    String refGrid = outputRef + File.separator + grid.toPath().subpath(len - 4, len);
                    assertEquals(imagesEqual(grid.toString(), refGrid), true);
                }
                b = b + 1;
            }
        }
     }



    public boolean imagesEqual(String img1Path, String img2Path) throws IOException{
        Dataset ds1 = gdal.Open(img1Path, 0);
        Dataset ds2 = gdal.Open(img2Path, 0);
        if (ds1.GetRasterCount() == ds2.GetRasterCount() && ds1.getRasterXSize() == ds2.getRasterXSize() && ds1.getRasterYSize() == ds2.getRasterYSize()) {
            Band b1 = ds1.GetRasterBand(1);
            Band b2 = ds2.GetRasterBand(1);
            for(int i = 0; i < ds1.getRasterYSize(); i++)
            {
                double[] data1 = new double[ds1.getRasterXSize()];
                b1.ReadRaster(0, i, ds1.getRasterXSize(), 1, data1);
                double[] data2 = new double[ds2.getRasterXSize()];
                b2.ReadRaster(0, i, ds2.getRasterXSize(), 1, data2);
                for(int d = 0; d < ds1.getRasterXSize(); d++) {
                    if (data1[d] != data2[d]) {
                        return false ;
                    }
                }
            }
            return true ;
        }
        return false ;
    }


    public void step()
    {
        String config = "src/test/resources/tests/config/configuration_base.json";
        String param = "src/test/resources/tests/config/params_base.json";

        try {
            int step1 = 3000;
            String outputDir1 = createTestDir("step_3km");
            String outputConfig1 = config(config, outputDir1, step1, "direct", false);
            String[] args = {"-c", outputConfig1, "-p", param};
            Sen2VM.main(args);
            verifyStepDirectLoc(outputConfig1, step1);

            int step2 = 6000;
            String outputDir2 = createTestDir("step_6km");
            String outputConfig2 = config(config, outputDir2, step2, "direct", false);
            String[] args2 = {"-c", outputConfig2, "-p", param};
            Sen2VM.main(args2);
            verifyStepDirectLoc(outputConfig2, step2);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void geoLoc()
    {
        String config = "src/test/resources/configuration_example.json";
        String param = "src/test/resources/tests/config/params_all.json";
        String outputRef = "/Sen2vm/sen2vm-core/src/test/resources/tests/6km_ref";
        try {
            int step = 6000;
            String outputDir = createTestDir("step_6km");
            String outputConfig = config(config, outputDir, step, "direct", false);
            String[] args = {"-c", outputConfig, "-p", param};
            Sen2VM.main(args);
            verifyDirectLoc(config, outputRef);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void geoRefining()
    {
        String config = "src/test/resources/configuration_example.json";
        String param = "src/test/resources/tests/config/params_base.json";

        try {
            String outputDir1 = createTestDir("step_6km_ref");
            String outputConfig1 = config(config, outputDir1, 6000, "direct", false);
            String[] args1 = {"-c", outputConfig1, "-p", param};
            Sen2VM.main(args1);
            String outputRef = "/Sen2vm/sen2vm-core/src/test/resources/tests/6km_ref";
            verifyDirectLoc(config, outputRef);

            String outputDir2 = createTestDir("step_6km_ref");
            String outputConfig2 = config(config, outputDir2, 6000, "direct", true);
            String[] args2 = {"-c", outputConfig2, "-p", param};
            Sen2VM.main(args2);
            outputRef = "/Sen2vm/sen2vm-core/src/test/resources/tests/6km_nonref";
            verifyDirectLoc(config, outputRef);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public void iersHandling()
    {
        String config = "src/test/resources/configuration_example.json";
        String param = "src/test/resources/tests/config/params_base.json";
        String outputRef = "/Sen2vm/sen2vm-core/src/test/resources/tests/no_iers";
        try {
            String outputDir = createTestDir("no_iers");
            String outputConfig = configSuppIERS(outputDir, config);
            String[] args = {"-c", outputConfig, "-p", param};
            Sen2VM.main(args);
            verifyDirectLoc(outputConfig, outputRef);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public String changeParams(String filePath, String[] detectors, String[] bands) throws FileNotFoundException,
            IOException, ParseException {

        // String[] detectors, String[] bands

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(filePath));

        JSONArray detectorsJsonArray = new JSONArray();
        for (String det : detectors) {
          detectorsJsonArray.add(det);
        }

        JSONArray bandsJsonArray = new JSONArray();
        for (String band : bands) {
          bandsJsonArray.add(band);
        }

        JSONObject objJson = (JSONObject) obj;
        objJson.put("detectors",detectorsJsonArray);
        objJson.put("bands", bandsJsonArray);

        String outputConfig = "src/test/resources/tests/config/param_tmp.json";
        FileWriter writer = new FileWriter(outputConfig, false);
        writer.write(obj.toString());
        writer.close();

        return outputConfig;

    }

    @Test
    public void parallelisationRobustness()
    {
        String config = "src/test/resources/configuration_example.json";
        String param = "src/test/resources/tests/config/params_base.json";


        try {
            String outputDir1 = createTestDir("order_1");
            String[] detectors_order_1 = new String[]{"01", "02"};
            String[] bands_order_1 = new String[]{"B01", "B02"};
            String config_order_1 = config(config, outputDir1, 6000, "direct", false);
            String param_order_1 = changeParams(param, detectors_order_1, bands_order_1);
            String[] args_order_1 = {"-c", config_order_1, "-p", param_order_1};
            Sen2VM.main(args_order_1);

            String outputDir2 = createTestDir("order_2");
            String[] detectors_order_2 = new String[]{"02", "01"};
            String[] bands_order_2 = new String[]{"B02", "B01"};
            String param_order_2 = changeParams(param, detectors_order_2, bands_order_2);
            String config_order_2 = config(config, outputDir2, 6000, "direct", false);
            String[] args_order_2 = {"-c", config_order_2, "-p", param_order_2};
            Sen2VM.main(args_order_2);

            verifyDirectLoc(config_order_2, outputDir1);
            verifyDirectLoc(config_order_1, outputDir2);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void copyFolder(File src, File dest, boolean copy) throws IOException{
        if(src.isDirectory()){
            if(!dest.exists()){
                dest.mkdir();
            }

            String files[] = src.list();

            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                copyFolder(srcFile,destFile, copy);
            }

        } else {

            if (copy) {
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0){
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            } else {
                Path records = src.toPath();
                Path recordsLink = dest.toPath();

            }
        }
    }



}
