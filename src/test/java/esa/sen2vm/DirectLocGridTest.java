package esa.sen2vm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import esa.sen2vm.utils.grids.DirectLocGrid;

/**
 * Unit test for DirectLocGridTest.
 */
public class DirectLocGridTest
{

    /**
     * Functional test
     */
    @Test
    public void createGrid()
    {
        double step = 4.5f;
        int startLine = 0;
        int startPixel = 0;
        int sizeLine = 2304;
        int sizePixel = 425;

        DirectLocGrid dirGrid = new DirectLocGrid(0.5f, 0.5f,
                        step, startPixel, startLine, sizeLine, sizePixel);
        assertEquals(dirGrid.getPixelOffsetGranule(), -1.75);
        assertEquals(dirGrid.getLineOffsetGranule(0), -1.75);
        assertEquals(dirGrid.getLineOffsetGranule(200), -3.75);
        assertEquals(dirGrid.getLineOffsetGranule(210), -0.25);

        ArrayList<Double> pixels = dirGrid.getGridPixels();
        assertEquals(pixels.size(), 95.0);
        assertEquals(pixels.get(0), -1.75f);
        assertEquals(pixels.get(1), pixels.get(0) + step);
        assertEquals(pixels.get(dirGrid.getGridPixels().size() - 1), 421.25f);

        ArrayList<Double> lines = dirGrid.getGridLines();
        assertEquals(lines.size(), 513);
        assertEquals(lines.get(0), -1.75f);
        assertEquals(lines.get(1), lines.get(0) + step);
        assertEquals(lines.get(dirGrid.getGridLines().size() - 1), 2302.25f);
    }

}
