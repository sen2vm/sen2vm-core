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



    public static final double RUGGED_FULL_RES_STEP = 0.001d;

    public static final double RUGGED_LOW_RES_STEP = 0.1d;
    /**
     * Pixel height for band (in meters)
     */
    public static final double PIXEL_HEIGHT_320 = 320d;




    /**
     * Pixel size for band (in meters)
     */
    public static final double DEFAULT_GEOMETRIC_HEADER_STEP = 78d;
    //public static final double DEFAULT_GEOMETRIC_HEADER_STEP = 74d;
    /**
     * Default no data value
     */
    public static final double NO_DATA_VALUE = -999999d;
    
    
    /**
     * Nb time rugged ask exactly same mnt tile (means that infinite loop due to mnt tiles not overlaping)
     */
    public static final int NB_TIME_ASK_SAME_TILE = 1000;
    /**
     * Nb cached tiles send in parameter to init rugged object
     */
    public static final int NB_CACHE_TILE = 12;
    /**
     * margin to use on date limit given to rugged
     */
    public static final int OVERSHOOT_TOLERANCE = 10;
    /**
     * quaternion interpolation order send in parameter to init rugged object
     */
    public static final int A_INTERPOLATION_ORDER = 8;
    /**
     * pv interpolation order send in parameter to init rugged object
     */
    public static final int PV_INTERPOLATION_ORDER = 6;
    /**
     * Granule nb line (for band having resolution of 10m)
     */
    public static final double GRANULE_NB_LINE_10_M = 2304d;
    
    /**
     * Number of line between min/max line for inverse computation (for band having resolution of 10m)
     * The interval is split in 1/4 parts
     */
    public static final double MINMAX_LINES_INTERVAL_QUARTER = 10d*GRANULE_NB_LINE_10_M;
//    public static final double MINMAX_LINES_INTERVAL_HALF = 2d*MINMAX_LINES_INTERVAL_QUARTER;
    public static final double MINMAX_LINES_INTERVAL = 4d*MINMAX_LINES_INTERVAL_QUARTER;
    
    /**
     * For all computations except ANGLES : 
     * A computed approximated line is a rough approximation and must be surrounded by an interval
     * (- APPROXIMATED_LINE_FACTOR * granule nb line , +  APPROXIMATED_LINE_FACTOR * granule nb line) around the found value
     * Be careful that the size of this interval must be less than 2 * MINMAX_LINES_INTERVAL_QUARTER
     */
    public static final double APPROXIMATED_LINE_FACTOR = 5d; 

    /**
     * For computation ANGLES : 
     * A computed approximated line is a rough approximation and must be surrounded by an interval
     * (- APPROXIMATED_LINE_FACTOR * granule nb line , +  APPROXIMATED_LINE_FACTOR * granule nb line) around the found value
     */
    public static final double APPROXIMATED_LINE_FACTOR_FOR_ANGLES = 8d; 

    /**
     * Nb max dump file that may be created if an error occurs during direct/inverse location
     */
    public static final int NB_MAX_DUMP_FILE = 1;
    /**
     * Nb max dump message that may be writen when NB_MAX_DUMP_FILE is reached
     */
    public static final int NB_MAX_DUMP_MESSAGE = 1;
    /**
     * Physical Pixel number
     */
    public static final double PHYSICAL_PIXEL_NUMBER = 2592d;
    /**
     * Earth radius in cms
     */
    public static final double EARTH_RADIUS = 637100000d;
    /**
     * One degree value in radian
     */
    // public static final double ONE_DEGREE_IN_RADIAN = FastMath.toRadians(1d);
    /**
     * Quicklook sensor name
     */
    public static final String QUICK_LOOK_SENSOR_NAME = "QUICK_LOOK";
    /**
     * Default Quicklook reference band
     */
    // public static final BandInfo DEFAULT_QL_REFERENCE_BAND = BandInfo.BAND_10;



    public static final String ERROR_CREATING_DATASOURCE = "error.creating.datasource";
    public static final String ERROR_EXPORTING_FILE = "error.exporting.file";
    public static final String ERROR_INIT_FOOTPRINT_TASK = "error.init.footprint.task";
    public static final String ERROR_INIT_RUGGED = "error.init.rugged";
    public static final String ERROR_LOADING_FILE = "error.loading.file";
    public static final String ERROR_OPENING_DATASOURCE = "error.opening.datasource";
    public static final String ERROR_PV_NULL_GPS = "error.pv.null.gps";
    public static final String ERROR_QUATERNION_NULL_GPS = "error.quaternion.null.gps";
    public static final String ERROR_UNABLE_TO_READ_FROM = "error.unable.to.read.from";
    public static final String INFINITE_DEM_LOOP = "infinite.dem.loop";
    public static final String INPUT_BAND_ID = "input.band.id";
    public static final String INPUT_DETECTOR_ID = "input.detector.id";
    public static final String INPUT_FILE_PATH = "input.file.path";
    public static final String INPUT_GDAL_NUMBERING = "input.gdal.numbering";
    public static final String INPUT_OPERATION = "input.operation";
    public static final String INPUT_SRS = "input.srs";
    public static final String INPUT_X_VALUE = "input.x.value";
    public static final String INPUT_Y_VALUE = "input.y.value";
    public static final String NO_RASTER_FILE_FOUND_IN_DEM = "no.raster.file.found.in.dem";
    public static final String OREKIT_DATA_NOT_FOUND = "orekit.data.not.found";
    public static final String DEM_TILE_CACHE_SIZE = "input.demCacheSize";


}
