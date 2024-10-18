package esa.sen2vm.utils;

import org.hipparchus.util.MathUtils;

public class Sen2VMConstants {

    /**
     * For bandInfo purpose
     */
    public static final int RESOLUTION_10M = 10;
    public static final double RESOLUTION_10M_DOUBLE = 10.0;
    public static final int RESOLUTION_20M = 20;
    public static final int RESOLUTION_60M = 60;

    /**
     * For GIPP purpose
     */
    public static final String GIPP_VIEWDIR_PAT = ".*GIP_VIEDIR.*";
    public static final String GIPP_BLINDP_PAT = ".*GIP_BLINDP.*";
    public static final String GIPP_SPAMOD_PAT = ".*GIP_SPAMOD.*";
    public static final int NB_GIPP_MAX_FILE = 13;

    /**
     * For product tree structure
     */
    public static final String DATASTRIP_MAIN_FOLDER = "DATASTRIP";
    public static final String DATASTRIP_MSI_TAG = "MSI";
    public static final String DATASTRIP_METADATA_TAG = "MTD";

    /**
     * File extensions possible
     */
    public static final String xml_extention_small = ".xml";
    public static final String xml_extention_big = ".XML";
    public static final String dbl_extention_small = ".dbl";
    public static final String dbl_extention_big = ".DBL";

    /**
     * For json schema
     */
    public static final String JSON_SCHEMA_CONFIG = "schema_config.json";
    public static final String JSON_SCHEMA_PARAMS = "schema_params.json";

    /**
     * For IERS purpose
     */
    public static final Boolean simpleEOP = true;
    public static final double lod = 0.0;
    public static final double ddPsi = 0.0;
    public static final double ddEps = 0.0;
    public static final double dx = 0.0;
    public static final double dy = 0.0;
    public static final int EOP_MARGIN = 5;
    // Conversion factor from arc seconds to radians: 2*PI/(360*60*60)
    public static final double ARC_SECONDS_TO_RADIANS = MathUtils.TWO_PI / 1296000;
    // Offset between julian day epoch and modified julian day epoch
    public static final double JD_TO_MJD = 2400000.5;
    // Duration of a mean solar day: 86400.0 s
    public static final double JULIAN_DAY = 86400.0;


    /**
     * Error management
     */
    public static final String ERROR_QUATERNION_NULL_GPS = "error.quaternion.null.gps";

    /**
     * Rugged Manager initialisation
     */
    // Granule line (for a 10m resolution band)
    public static final double GRANULE_NB_LINE_10_M = 2304.0;
    // Granule line (for a 60m resolution band) ??
    public static final double GRANULE_NB_LINE_60_M = 384.0;

    // The interval is split in 1/4 parts.
    public static final double MINMAX_LINES_INTERVAL_QUARTER = 10.0 * GRANULE_NB_LINE_10_M;
    // Number of lines between min/max line for inverse computation (for a 10m resolution band)
    public static final double MINMAX_LINES_INTERVAL = 4.0 * MINMAX_LINES_INTERVAL_QUARTER;

    // Compute margin according to the band pixel size
    public static final double BAND_PIXEL_SIZE = RESOLUTION_10M_DOUBLE;
    public static final double MARGIN = GRANULE_NB_LINE_60_M * RESOLUTION_10M_DOUBLE / BAND_PIXEL_SIZE;

}
