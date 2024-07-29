package esa.sen2vm;

import generated.GS2_BLIND_PIXELS;
import generated.GS2_SPACECRAFT_MODEL_PARAMETERS;
import generated.GS2_VIEWING_DIRECTIONS;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

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
     * Load GIPP from XML filder
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
    public void setGippFolderPath(String gippFolder) throws Sen2VMException {
        gippFileManager = new GIPPFileManager(gippFolder);
        viewingDirectionMap = new HashMap<BandInfo, GS2_VIEWING_DIRECTIONS>();
        LOGGER.info("Get through GIPP folder : "+ gippFolder);
        loadAllGIPP();
    }

    /*
     * Function to load all GIPP
     */
    protected void loadAllGIPP() throws Sen2VMException {
        // Load blind pixel gipp
        File fileBlindPixel = null;
        try {
            fileBlindPixel = gippFileManager.getBlindPixelFile();
            if (fileBlindPixel != null) {
                LOGGER.info("Read blind pixel file : "+ fileBlindPixel);
                blindPixelInfo = (GS2_BLIND_PIXELS) jaxbUnmarshaller.unmarshal(fileBlindPixel);
            }
        } catch (Exception e) {
            LOGGER.severe("Error when reading the blind pixel GIPP file : " + fileBlindPixel + " with message : " + e.getMessage());
        }

        // Load spacecraft model gipp
        File fileSpaMod = null;
        try {
            fileSpaMod = gippFileManager.getSpaModFile();
            if (fileSpaMod != null) {
                LOGGER.info("Read spacecraft model file : "+ fileSpaMod);
                GS2_SPACECRAFT_MODEL_PARAMETERS spaModInfo = (GS2_SPACECRAFT_MODEL_PARAMETERS) jaxbUnmarshaller.unmarshal(fileSpaMod);
                spaModMgr = new SpaModManager(spaModInfo);
            }
        } catch (Exception e) {
            LOGGER.severe("Error when reading spacecraft model GIPP file : " + fileSpaMod + " with message : " + e.getMessage());
        }

        // Load viewing directions gipp
        File file = null;
        try {
            List<File> gippFilePathList = gippFileManager.getViewingDirectionFileList();
            if (gippFilePathList != null) {
                for (int i = 0; i < gippFilePathList.size(); i++) {
                    file = gippFilePathList.get(i);
                    LOGGER.info("Read viewingDirection file : "+ file);

                    // Load GIPP DATA
                    GS2_VIEWING_DIRECTIONS viewingDirection = (GS2_VIEWING_DIRECTIONS) jaxbUnmarshaller.unmarshal(file);
                    int bandId = viewingDirection.getDATA().getBAND_ID();
                    BandInfo bandInfo = BandInfo.getBandInfoFromIndex(bandId);
                    viewingDirectionMap.put(bandInfo, viewingDirection);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error when reading viewing directions GIPP files with message : " + e.getMessage());
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

}
