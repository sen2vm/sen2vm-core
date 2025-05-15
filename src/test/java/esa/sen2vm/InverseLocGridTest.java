package esa.sen2vm;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.enums.BandInfo;

import esa.sen2vm.input.Configuration;
import esa.sen2vm.input.SafeManager;
import esa.sen2vm.input.datastrip.DataStripManager;
import esa.sen2vm.input.datastrip.Datastrip;
import esa.sen2vm.utils.grids.InverseLocGrid;
import esa.sen2vm.utils.grids.Coordinates;


import java.io.IOException;
import org.json.simple.parser.ParseException;
import esa.sen2vm.exception.Sen2VMException;


import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;


/**
 * Unit test for InverseLocGridTest.
 */
public class InverseLocGridTest
{


    String configTmpInverse = "src/test/resources/tests/input/TDS1/configuration_TDS1_inverse.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/ref";

    /**
     * Functional test
     */
    @Test
    public void createGrid()
    {

        float ulX = 199980.0f;
        float ulY = 3700020.0f;
        float lrX = 309780.0f;
        float lrY = 3590220.0f;
        float res = 10.0f;
        float step = 4.5f;
        String epsg = "EPSG:32628";

        // new bounding box
        InverseLocGrid invGrid = new InverseLocGrid(ulX, ulY, lrX, lrY, epsg, res, step);
        ArrayList<Float> gridX = invGrid.getGridX() ;
        ArrayList<Float> gridY = invGrid.getGridY() ;

        System.out.println("SIZE_GRIDX = " + String.valueOf(gridX.size()));
        System.out.println("UL_X_input= " + String.valueOf(ulX));
        System.out.println("UL_X_grid =" + String.valueOf(invGrid.getUlX()));
        System.out.println("Start_X_sans_conv = " + String.valueOf(gridX.get(0) - invGrid.getStepX() / 2));
        System.out.println("Start_X_avec_conv = " + String.valueOf(gridX.get(0)));
        System.out.println("END_X_avec_conv = " + String.valueOf(gridX.get(gridX.size()-1)));
        System.out.println("END_X_sans_conv = " + String.valueOf(gridX.get(gridX.size()-1) - invGrid.getStepX() / 2) );
        System.out.println("LR_X_input = " + String.valueOf(lrX));
        System.out.println("");

        System.out.println("SIZE_GRIDY = " + String.valueOf(gridY.size()));
        System.out.println("UL_Y_input = " + String.valueOf(ulY));
        System.out.println("UL_Y_grid = " + String.valueOf(invGrid.getUlY()));
        System.out.println("Start_Y_sans_conv = " + String.valueOf(gridY.get(0) - invGrid.getStepY() / 2));
        System.out.println("Start_Y_avec_conv = " + String.valueOf(gridY.get(0)));
        System.out.println("END_Y_avec_conv = " + String.valueOf(gridY.get(gridY.size()-1)));
        System.out.println("END_Y_sans_conv = " + String.valueOf(gridY.get(gridY.size()-1) - invGrid.getStepY() / 2) );
        System.out.println("LR_Y_input = " + String.valueOf(lrY));

        float stepX = invGrid.getStepX();
        float stepY = invGrid.getStepY();
        assertEquals(stepX, step * res);
        assertEquals(stepY, - step * res);
        assertEquals(invGrid.getUlX() + invGrid.getStepX() / 2, ulX + res/2);
        assertEquals(invGrid.getUlY() + invGrid.getStepY() / 2, ulY - res/2);
        assertEquals(gridX.size(), 2442);
        assertEquals(gridY.size(), 2442);
        assertEquals(invGrid.getLrX(), 309797.5);
        assertEquals(invGrid.getLrY(), 3590202.5);

        // size grids


        // start grids
        assertEquals(gridX.get(0), ulX + res/2);
        assertEquals(gridY.get(0), ulY - res/2);

        // last grid pixel exceed input bb
        assertTrue(gridX.get(gridX.size()-1) - stepX/2 > lrX);
        assertTrue(gridY.get(gridY.size()-1) - stepY/2 < lrY);
        assertTrue(gridX.get(gridX.size()-1) > invGrid.getLrX());
        assertTrue(gridX.get(gridX.size()-2) < invGrid.getLrX());
        assertTrue(gridY.get(gridY.size()-1) < invGrid.getLrY());
        assertTrue(gridY.get(gridY.size()-2) > invGrid.getLrY());

        int epsg_int = Integer.valueOf(epsg.substring(5));
        Coordinates ul = new Coordinates(ulX, ulY, epsg_int);
        ul.transform();
        Coordinates lr = new Coordinates(lrX, lrY, epsg_int);
        lr.transform();
        System.out.println("UL XY(" + String.valueOf(ul.getLongitude()) + ", " + String.valueOf(ul.getLatitude()) + ")");
        System.out.println("LR XY(" + String.valueOf(lr.getLongitude()) + ", " + String.valueOf(lr.getLatitude()) + ")");



    }


    @Test
    public void testInverseLocGeoTransform()
    {

        String[] detectors = new String[]{"07"};
        String[] bands = new String[]{"B02"};
        int step = 6000;
        try
        {
            String nameTest = "testInverseLocGeoTransform";
            String outputDir = Config.createTestDir(nameTest, "inverse");
            String config = Config.config(configTmpInverse, outputDir, step, "inverse", false);
            String param = Config.changeParams(paramTmp, detectors, bands, outputDir);
            String[] args = {"-c", config, "-p", param};
            Sen2VM.main(args);
            // Utils.verifyInverseLoc(config, refDir + "/" + nameTest);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Sen2VMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}


