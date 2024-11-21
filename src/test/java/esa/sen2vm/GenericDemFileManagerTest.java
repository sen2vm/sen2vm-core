package esa.sen2vm;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.input.GenericDemFileManager;

/**
 * Unit test for GenericFileManagerTest.
 */
public class GenericDemFileManagerTest
{
    /**
     * Functional test
     */
    @Test
    public void readSelectDemTile()
    {
        try {

            // Test DEM
            String dem_tests_path = "/Sen2vm/sen2vm-core/tests/resources/DEM_TESTS/";

            String[] tests_name = {"DEM_e002_n02.dt1", "DEM_e002_n02.dt2", "DEM_e002_n02_x.dt1", "DEM_e002n02.dt1", "DEM_n02_e000.dt1", "DEM_n02e02.dt1"};
            for (String test_name : tests_name) {
                String dem_path = dem_tests_path + test_name ;
                GenericDemFileManager demFileManager = new GenericDemFileManager(dem_path);
                // assertEquals(true, demFileManager.findRasterFile(dem_path));
                demFileManager.buildMap(dem_path);
                Double latitude = Math.toRadians(6.5000000);
                Double longitude = Math.toRadians(0.5000000);
                assertEquals("0/6", demFileManager.getLonLatFromFile(demFileManager.getRasterFilePath(latitude, longitude)));
            }

        }
        catch (Sen2VMException e) {
            e.printStackTrace();
        }
    }

}
