package esa.sen2vm;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.sxgeo.input.datamodels.sensor.SensorViewingDirection;
import org.sxgeo.input.datamodels.sensor.SpaceCraftModelTransformation;

import generated.GS2_BLIND_PIXELS;
import generated.GS2_SPACECRAFT_MODEL_PARAMETERS;
import generated.GS2_VIEWING_DIRECTIONS;
import generated.GS2_VIEWING_DIRECTIONS.DATA;
import generated.GS2_VIEWING_DIRECTIONS.DATA.VIEWING_DIRECTIONS_LIST;
import generated.GS2_VIEWING_DIRECTIONS.DATA.VIEWING_DIRECTIONS_LIST.VIEWING_DIRECTIONS;


/**
 * Manager for GIPP
 */
public class GIPPManager {

    /*
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(GIPPManager.class.getName());

    /**
     * Gipp file manager
     */
    protected GIPPFileManager gippFileManager = null;

    /**
     * Map containing viewing direction for each band
     */
    protected HashMap<BandInfo, GS2_VIEWING_DIRECTIONS> viewingDirectionMap = null;

    /**
     * Blind pixel info
     */
    protected GS2_BLIND_PIXELS blindPixelInfo = null;

    /**
     * SPAMOD
     */
    protected SpaModManager spaModMgr = null;

    /**
     * Jaxb unmarshaller
     */
    private Unmarshaller jaxbUnmarshaller;

    /**
     * Unique GIPPManager instance
     */
    protected static GIPPManager singleton = null;

    /**
     * Load GIPP from XML folder
     * @throws Sen2VMException
     */
    protected GIPPManager() throws Sen2VMException {
        try {
            // All GIPP have same package ("generated")
            JAXBContext jaxbContext = JAXBContext.newInstance(GS2_VIEWING_DIRECTIONS.class.getPackage().getName());

            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            gippFileManager = new GIPPFileManager();
            viewingDirectionMap = new HashMap<>();
        } catch (Exception e) {
            Sen2VMException exception = new Sen2VMException(e.getMessage(), e);
            throw exception;
        }
    }

    /**
     * Get instance
     * @return instance
     * @throws Sen2VMException
     */
    public static synchronized GIPPManager getInstance() throws Sen2VMException {
        if (singleton == null) {
            singleton = new GIPPManager();
        }
        return singleton;
    }

    /**
     * To use when all GIPP are in the same folder
     * Used only by Junit tests
     * @param gippFolder a folder that is supposed to contain all wanted GIPP
     * @throws Sen2VMException
     */
    public void setGippFolderPath(String gippFolder, List<BandInfo> bands) throws Sen2VMException {
        gippFileManager = new GIPPFileManager(gippFolder);
        viewingDirectionMap = new HashMap<BandInfo, GS2_VIEWING_DIRECTIONS>();
        LOGGER.info("Get through GIPP folder: "+ gippFolder);
        loadAllGIPP(bands);
    }

    /*
     * Function to load all GIPP
     * @param list of bands we actually want to process, the list comes from parameter file
     * @throws Sen2VMException
     */
    protected void loadAllGIPP(List<BandInfo> bands) throws Sen2VMException {
        // Load blind pixel gipp
        File fileBlindPixel = null;
        try {
            fileBlindPixel = gippFileManager.getBlindPixelFile();
            if (fileBlindPixel != null) {
                LOGGER.info("Read blind pixel file: "+ fileBlindPixel);
                blindPixelInfo = (GS2_BLIND_PIXELS) jaxbUnmarshaller.unmarshal(fileBlindPixel);
            }
        } catch (Exception e) {
            LOGGER.severe("Error when reading the blind pixel GIPP file: " + fileBlindPixel + " with message: " + e.getMessage());
        }

        // Load spacecraft model gipp
        File fileSpaMod = null;
        try {
            fileSpaMod = gippFileManager.getSpaModFile();
            if (fileSpaMod != null) {
                LOGGER.info("Read spacecraft model file: "+ fileSpaMod);
                GS2_SPACECRAFT_MODEL_PARAMETERS spaModInfo = (GS2_SPACECRAFT_MODEL_PARAMETERS) jaxbUnmarshaller.unmarshal(fileSpaMod);
                spaModMgr = new SpaModManager(spaModInfo);
            }
        } catch (Exception e) {
            LOGGER.severe("Error when reading spacecraft model GIPP file: " + fileSpaMod + " with message: " + e.getMessage());
        }

        // Load viewing directions gipp
        try {
            List<File> gippFilePathList = gippFileManager.getViewingDirectionFileList();
            for (int i = 0; i < bands.size(); i++) {
                File file = gippFilePathFromIndexBand(bands.get(i), gippFilePathList);
                if (file == null) {
                    throw new Sen2VMException("Viewing directions GIPP file missing for band "+ bands.get(i));
                }

                // Load GIPP DATA
                LOGGER.info("Read viewingDirection file: "+ file);
                GS2_VIEWING_DIRECTIONS viewingDirection = (GS2_VIEWING_DIRECTIONS) jaxbUnmarshaller.unmarshal(file);
                int bandId = viewingDirection.getDATA().getBAND_ID();
                BandInfo bandInfo = BandInfo.getBandInfoFromIndex(bandId);
                viewingDirectionMap.put(bandInfo, viewingDirection);
            }
        } catch (Exception e) {
            LOGGER.severe("Error when reading viewing directions GIPP files with message: " + e.getMessage());
        }
    }

    /**
     * Get viewing directions for the given band
     * @return the viewingDirections
     * @throws Sen2VMException
     */
    public GS2_VIEWING_DIRECTIONS getViewingDirections(BandInfo bandInfo) throws Sen2VMException {
        GS2_VIEWING_DIRECTIONS returned = null;
        if (viewingDirectionMap.containsKey(bandInfo)) {
            returned = viewingDirectionMap.get(bandInfo);
        }
        return returned;
    }

    /**
     * Get the wanted viewing direction depending of the given bandInfo
     * @param data data containing the viewing direction list
     * @param bandInfo the band we are working on
     * @return The needed viewing direction depending of the given bandInfo
     */
    protected VIEWING_DIRECTIONS_LIST getViewingDirectionList(DATA data, BandInfo bandInfo) {
        // Take the first (and only) viewing direction list for all other bands
        VIEWING_DIRECTIONS_LIST returned = data.getVIEWING_DIRECTIONS_LIST().get(0);
        switch (bandInfo) {
        case BAND_2:
        case BAND_3:
        case BAND_11:
        case BAND_12:
            // for band 2 3 11 and 12 we get the VIEWING_DIRECTIONS_LIST having TDI_CONFIGURATION with same value than in SAD sensor config element

            // Get TDI_CONFIGURATION value from SAD for the given band
            String tdiConfVal = DataStripManager.getInstance().getTdiConfVal(bandInfo);

            // Get viewingDirection corresponding to tdi configuration value from SAD
            for (VIEWING_DIRECTIONS_LIST viewingDirectionList : data.getVIEWING_DIRECTIONS_LIST()) {
                if (viewingDirectionList.getTdi_Config().value().equals(tdiConfVal)) {
                    // we found the good VIEWING_DIRECTIONS_LIST
                    return viewingDirectionList;
                }
            }
            break;

        default:
            break;
        }
        return returned;
    }

    /**
     * Get sensor viewing directions for the given band
     * @return the viewingDirections
     * @throws Sen2VMException
     */
    public SensorViewingDirection getSensorViewingDirections(BandInfo bandInfo, DetectorInfo detectorInfo) throws Sen2VMException {
        DATA data = getViewingDirections(bandInfo).getDATA();
        VIEWING_DIRECTIONS_LIST viewingDirectionsList = getViewingDirectionList(data, bandInfo);
        VIEWING_DIRECTIONS viewingDirections = getViewingDirection(detectorInfo, viewingDirectionsList.getVIEWING_DIRECTIONS());
        int nbPix = viewingDirections.getNB_OF_PIXELS();
        List<Double> tanPsiXList = viewingDirections.getTAN_PSI_X_LIST();
        List<Double> tanPsiYList = viewingDirections.getTAN_PSI_Y_LIST();
        return new SensorViewingDirection(nbPix, tanPsiXList, tanPsiYList);
    }

    /**
     * Get viewing directions for given detector info
     * @param detectorInfo detector info
     * @param viewingDirectionList list of viewing directions
     * @return viewing directions for given detector info
     */
    private VIEWING_DIRECTIONS getViewingDirection(DetectorInfo detectorInfo, List<VIEWING_DIRECTIONS> viewingDirectionList) {
        for (VIEWING_DIRECTIONS viewingDirections : viewingDirectionList) {
            if (viewingDirections.getDetector_Id().equals(detectorInfo.getName())) {
                return viewingDirections;
            }
        }
        return null;
    }

    /**
     * Verify if viewing direction GIPP file associated to a certain band exists
     * @param the band we are looking for
     * @param list of all viewing directions GIPP files find in GIPP folder
     * @return the GIPP file if it is present in the GIPP list, or null if not present
     */
     protected File gippFilePathFromIndexBand(BandInfo bandInfo, List<File> gippFilePathList) {
        String searchedBand = "B" + bandInfo.getName2Digit();
        for(int i=0; i < gippFilePathList.size(); i++) {
            if (gippFilePathList.get(i).toString().contains(searchedBand)) {
                return gippFilePathList.get(i);
            }
        }
        return null;
     }

    /**
     *
     * @return
     */
     public SpaceCraftModelTransformation getPilotingToMsiTransformation() {
        SpaceCraftModelTransformation returned = null;
        if (spaModMgr != null) {
            returned = spaModMgr.getPilotingToMsiTransformation();
        }
        return returned;
    }

    /**
     *
     * @return
     */
    public SpaceCraftModelTransformation getMsiToFocalPlaneTransformation(BandInfo bandInfo) {
        SpaceCraftModelTransformation returned = null;
        if (spaModMgr != null) {
            returned = spaModMgr.getMsiToFocalPlaneTransformation(bandInfo);
        }
        return returned;
    }

    /**
     *
     * @return
     */
    public SpaceCraftModelTransformation getFocalPlaneToDetectorTransformation(BandInfo bandInfo, DetectorInfo detectorInfo) {
        SpaceCraftModelTransformation returned = null;
        if (spaModMgr != null) {
            returned = spaModMgr.getFocalPlaneToDetectorTransformation(bandInfo, detectorInfo);
        }
        return returned;
    }
}
