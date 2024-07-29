package esa.sen2vm;

public class Sen2VMFilesConstants {

    // Extentions
    public static final String tif_extention = ".tif";
    public static final String jp2_extention = ".jp2";
    public static final String vrt_extention = ".vrt";
    public static final String xml_extention = ".xml";
    public static final String raw_extention = ".raw";
    public static final String txt_extention = ".txt";
    public static final String srtm_dt1_extention = ".dt1";
    public static final String srtm_dt2_extention = ".dt2";

    // Satellites
    public static final String S2A = "S2A_";
    public static final String S2B = "S2B_";
    public static final String S2C = "S2C_";
    public static final String S2D = "S2D_";

    // Directories
    public static final String IMG_DATA = "IMG_DATA";
    public static final String QI_DATA = "QI_DATA";
    public static final String IMAGES = "IMAGES";
    public static final String MASKS = "MASKS";
    public static final String REPORT = "REPORT";
    public static final String AUX_DATA = "AUX_DATA";

    // FileNames
    public static final String ATF_PART_FILENAME = "_ATF";
    public static final String DD_PART_FILENAME = "_D";
    public static final String GR_PART_FILENAME = "_GR";
    public static final String B_PART_FILENAME = "_B";
    public static final String MSI = "_MSI";
    public static final String MTD = "_MTD";
    public static final String GEO = "_GEO";
    public static final String DETFOO_MASKID = "DETFOO";

    /**
     * Default footprint output file name
     */
    public static final String DEFAULT_OUTPUT_FILE_NAME = "footprint";
    /**
     * Default footprint output file name
     */
    public static final String DEFAULT_DETECTOR_OUTPUT_FILE_TEMPLATE_NAME = "detector_footprint_%02d";
    /**
     * Default footprint output file name
     */
    public static final String DEFAULT_OUTPUT_QL_FILE_NAME = "footprint_ql";
    /**
     * Default footprint output file name
     */
    public static final String DEFAULT_OUTPUT_QL_DATATION_MODEL_FILE_NAME = "datation_model.xml";
    /**
     * Default global footprint output file name
     */
    public static final String DEFAULT_GLOBAL_FOOTPRINT_OUTPUT_FILE_NAME = "global_footprint.gml";
    /**
     * Default quicklook global footprint output file name
     */
    public static final String DEFAULT_QUICK_LOOK_GLOBAL_FOOTPRINT_FILE_NAME = "quicklook_footprint";
    /**
     * Default geometric header list output file name
     */
    public static final String DEFAULT_GEOMETRIC_HEADER_LIST_OUTPUT_FILE_NAME = "geometric_header_list.xml";
    /**
     * Default ouput directory for granule xml files
     */
    public static final String DEFAULT_GRANULE_DIR_NAME = "GRANULE";
    /**
     * Default ouput directory for collocation grid
     */
    public static final String DEFAULT_COLLOCATION_GRID_DIR_NAME = "COLLOCATION_GRID";
    /**
     * Default ouput directory for direct grid
     */
    public static final String DEFAULT_DIRECT_LOCATION_GRID_DIR_NAME = "DIRECT_LOCATION_GRID";
    /**
     * Default ouput directory for direct grid
     */
    public static final String DEFAULT_INVERSE_LOCATION_GRID_DIR_NAME = "INVERSE_LOCATION_GRID";
    /**
     * Default OGR Feature layer name
     */
    public static final String DEFAULT_OGR_FEATURE_LAYER_NAME = "Feature";
    /**
     * Default OGR detector footprint layer name
     */
    public static final String DEFAULT_OGR_DETECTOR_FOOTPRINT_LAYER_NAME = "DETECTOR_FOOTPRINT";
    /**
     * Default OGR cloud inv layer name
     */
    public static final String DEFAULT_OGR_CLOUD_INV_LAYER_NAME = "CLOUD_INV";

    /**
     * Default detector chosen for sun grid in tile L1C
     */
    public static final String DEFAULT_DETECTOR_BAND_SUN_GRID_TILE_L1C = "B2_D01";
    
    /**
     * Default pattern to be added to S2GEO_input_interface XML filename
     * in the case of Cloud Mask Projection
     * The S2GEO_input_interface filename must contains the pattern DEFAULT_HCLOUD_PATTERN
     */
    public static final String DEFAULT_HCLOUD_PATTERN = "HCLOUD";



}
