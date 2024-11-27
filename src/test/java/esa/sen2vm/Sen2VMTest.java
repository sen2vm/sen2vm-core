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
import esa.sen2vm.input.ConfigurationFile;

/**
 * Unit test for Sen2VM.
 */
public class Sen2VMTest
{
    /**
     * Functional test
     */
    @Test
    public void readConfigurationFile ()
    {
        try {
            // Read configuration file
            ConfigurationFile configFile = new ConfigurationFile("src/test/resources/configuration_example.json");
            System.out.println("Datastrip file path: " + configFile.getDatastripFilePath() + "\nIERS bulletin path: "+ configFile.getIers() + "\nboolean refining: " + configFile.getDeactivateRefining());
        }
        catch (Sen2VMException e) {
            e.printStackTrace();
        }
    }
}
