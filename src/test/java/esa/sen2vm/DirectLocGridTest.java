/** Copyright 2024-2025, CS GROUP, https://www.cs-soprasteria.com/
*
* This file is part of the Sen2VM Core project
*     https://gitlab.acri-cwa.fr/opt-mpc/s2_tools/sen2vm/sen2vm-core
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.*/

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
