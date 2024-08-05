package esa.sen2vm;

public class Sen2VMConstants {
    public static final int NB_LINES_PER_PACKET = 16;
    public static final int RESOLUTION_10M = 10;
    public static final int RESOLUTION_20M = 20;
    public static final int RESOLUTION_60M = 60;
    public static final int RESOLUTION_320M = 320;
    public static final int NB_LINES_10M = 2304;    //PSD: 144 packets * 16 lines
    public static final int NB_LINES_20M = 1152;    //PSD: 72 packets * 16 lines
    public static final int NB_LINES_60M = 384;     //PSD: 24 packets * 16 lines
    public static final int NB_COLUMNS_10M = 2592;
    public static final int NB_COLUMNS_20_60M = 1296;
    public static final double BCO_INTERPOLATION_PARAMETER = -0.5;
    public static final double NO_DATA = -32767;
    public static final double EPSILON = 1.0e-10;
    public static final double LR_TO_FR = 8.0;
    public static final int N_DETECTORS = 12;
    public static final int N_BANDS = 13;
    public static final int S2_NUMBER_OF_FOCAL_PLANES = 2;
    public static final int INDICATOR_TIMER_SECONDS = 10;
    public static final int INDICATOR_TIMER_MAX_ITERATIONS = 12;
    public static final int NB_GIPP_MAX_FILE = 13;

    /**
     * For GIPP purpose
     */
     public static final String GIPP_VIEWDIR_NAME = ".*GIP_VIEDIR.*";
     public static final String GIPP_BLINDP_NAME = ".*GIP_BLINDP.*";
     public static final String GIPP_SPAMOD_NAME = ".*GIP_SPAMOD.*";

    /**
     * Granule nb line (for band having resolution of 10m)
     */
    public static final double GRANULE_NB_LINE_10_M = 2304d;
}