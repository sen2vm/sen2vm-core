package esa.sen2vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.logging.Logger;

import esa.sen2vm.utils.grids.InverseLocGrid;
import esa.sen2vm.utils.grids.Coordinates;

/**
 * Unit test for InverseLocGridTest.
 */
public class InverseLocGridTest
{
    private static final Logger LOGGER = Logger.getLogger(InverseLocGridTest.class.getName());

    String configTmpInverse = "src/test/resources/tests/input/TDS1/configuration_TDS1_inverse.json";
    String paramTmp = "src/test/resources/params_base.json";
    String refDir = "src/test/resources/tests/ref";

    /**
     * Functional test
     */
    @Test
    public void createGrid()
    {
        double ulX = 199980.0f;
        double ulY = 3700020.0f;
        double lrX = 309780.0f;
        double lrY = 3590220.0f;
        double step = 45f;
        String epsg = "EPSG:32628";

        // new bounding box
        InverseLocGrid invGrid = new InverseLocGrid(ulX, ulY, lrX, lrY, epsg, step);
        ArrayList<Double> gridX = invGrid.getGridX();
        ArrayList<Double> gridY = invGrid.getGridY();

        LOGGER.info("SIZE_GRIDX = " + String.valueOf(gridX.size()));
        LOGGER.info("UL_X_input= " + String.valueOf(ulX));
        LOGGER.info("UL_X_grid =" + String.valueOf(invGrid.getUlX()));
        LOGGER.info("Start_X_sans_conv = " + String.valueOf(gridX.get(0) - invGrid.getStepX() / 2));
        LOGGER.info("Start_X_avec_conv = " + String.valueOf(gridX.get(0)));
        LOGGER.info("END_X_avec_conv = " + String.valueOf(gridX.get(gridX.size()-1)));
        LOGGER.info("END_X_sans_conv = " + String.valueOf(gridX.get(gridX.size()-1) - invGrid.getStepX() / 2));
        LOGGER.info("LR_X_input = " + String.valueOf(lrX));
        LOGGER.info("");

        LOGGER.info("SIZE_GRIDY = " + String.valueOf(gridY.size()));
        LOGGER.info("UL_Y_input = " + String.valueOf(ulY));
        LOGGER.info("UL_Y_grid = " + String.valueOf(invGrid.getUlY()));
        LOGGER.info("Start_Y_sans_conv = " + String.valueOf(gridY.get(0) - invGrid.getStepY() / 2));
        LOGGER.info("Start_Y_avec_conv = " + String.valueOf(gridY.get(0)));
        LOGGER.info("END_Y_avec_conv = " + String.valueOf(gridY.get(gridY.size()-1)));
        LOGGER.info("END_Y_sans_conv = " + String.valueOf(gridY.get(gridY.size()-1) - invGrid.getStepY() / 2));
        LOGGER.info("LR_Y_input = " + String.valueOf(lrY));

        double stepX = invGrid.getStepX();
        double stepY = invGrid.getStepY();
        assertEquals(stepX, step);
        assertEquals(stepY, - step);
        assertEquals(invGrid.getUlX() + invGrid.getStepX() / 2, ulX);
        assertEquals(invGrid.getUlY() + invGrid.getStepY() / 2, ulY);

        // size grids
        assertEquals(gridX.size(), 2441);
        assertEquals(gridY.size(), 2441);

        // start grids
        assertEquals(gridX.get(0), ulX);
        assertEquals(gridY.get(0), ulY);

        // last grid pixel equal or exceed last the center of the last
        // image pixel (band resolution) of the area
        assertTrue(gridX.get(gridX.size()-1) >= ulX);
        assertTrue(gridY.get(gridY.size()-1) <= ulY);;
        assertEquals(invGrid.getLrX(), gridX.get(gridX.size()-1) + stepX / 2);
        assertEquals(invGrid.getLrX(), 309802.5);
        assertEquals(invGrid.getLrY(), gridY.get(gridY.size()-1) + stepY / 2);
        assertEquals(invGrid.getLrY(), 3590197.5);

        int epsg_int = Integer.valueOf(epsg.substring(5));
        Coordinates ul = new Coordinates(ulX, ulY, epsg_int);
        ul.transform();
        Coordinates lr = new Coordinates(lrX, lrY, epsg_int);
        lr.transform();
        LOGGER.info("UL XY(" + String.valueOf(ul.getLongitude()) + ", " + String.valueOf(ul.getLatitude()) + ")");
        LOGGER.info("LR XY(" + String.valueOf(lr.getLongitude()) + ", " + String.valueOf(lr.getLatitude()) + ")");
    }

    @Test
    public void endGrid()
    {

        double ulX = 0f;
        double ulY = 0f;
        double lrX = 100f;
        double lrY = 100f;
        double step = 10f;
        String epsg = "EPSG:32628";
        InverseLocGrid invGrid = new InverseLocGrid(ulX, ulY, lrX, lrY, epsg, step);
        ArrayList<Double> gridX = invGrid.getGridX();
        assertEquals(gridX.get(0), 0);
        assertEquals(gridX.get(gridX.size()-1), 100);
        LOGGER.info(String.valueOf(gridX.get(0)) + " => " + String.valueOf(gridX.get(gridX.size()-1)));

        step = 20f;
        invGrid = new InverseLocGrid(ulX, ulY, lrX, lrY, epsg, step);
        gridX = invGrid.getGridX();
        assertEquals(gridX.get(0), 0);
        assertEquals(gridX.get(gridX.size()-1), 100);
        LOGGER.info(String.valueOf(gridX.get(0)) + " => " + String.valueOf(gridX.get(gridX.size()-1)));

        step = 80f;
        invGrid = new InverseLocGrid(ulX, ulY, lrX, lrY, epsg, step);
        gridX = invGrid.getGridX();
        assertEquals(gridX.get(0), 0);
        assertEquals(gridX.get(gridX.size()-1), 160);
        LOGGER.info(String.valueOf(gridX.get(0)) + " => " + String.valueOf(gridX.get(gridX.size()-1)));

        step = 3.0f;
        invGrid = new InverseLocGrid(ulX, ulY, lrX, lrY, epsg, step);
        gridX = invGrid.getGridX();
        assertEquals(gridX.get(0), 0);
        assertEquals(gridX.get(gridX.size()-1), 102);
        LOGGER.info(String.valueOf(gridX.get(0)) + " => " + String.valueOf(gridX.get(gridX.size()-1)));
    }
}