package esa.sen2vm;

import java.util.ArrayList;
import java.util.List;

/**
 * Information on band
 */
public enum BandInfo {
    BAND_1("1", 0, Sen2VMConstants.RESOLUTION_60M),
    BAND_2("2", 1, Sen2VMConstants.RESOLUTION_10M),
    BAND_3("3", 2, Sen2VMConstants.RESOLUTION_10M),
    BAND_4("4", 3, Sen2VMConstants.RESOLUTION_10M),
    BAND_5("5", 4, Sen2VMConstants.RESOLUTION_20M),
    BAND_6("6", 5, Sen2VMConstants.RESOLUTION_20M),
    BAND_7("7", 6, Sen2VMConstants.RESOLUTION_20M),
    BAND_8("8", 7, Sen2VMConstants.RESOLUTION_10M),
    BAND_8A("8A", 8, Sen2VMConstants.RESOLUTION_20M),
    BAND_9("9", 9, Sen2VMConstants.RESOLUTION_60M),
    BAND_10("10", 10, Sen2VMConstants.RESOLUTION_60M),
    BAND_11("11", 11, Sen2VMConstants.RESOLUTION_20M),
    BAND_12("12", 12, Sen2VMConstants.RESOLUTION_20M);

    /**
     * Band name
     */
    protected String name = null;

    /**
     * Band index
     */
    protected int index = 0;

    /**
     * Band pixel height
     */
    protected int pixelHeight = 0;

    /**
     * Private constructor
     * @param name band name
     * @param pixelHeight band pixel size in meter)
     */
    private BandInfo(String name, int index, int pixelHeight) {
        this.name = name;
        this.index = index;
        this.pixelHeight = pixelHeight;
    }

    /**
     * Get BandInfo from band name
     * @param bandName band name
     * @return the band having the given name. Null if not found
     */
    public static BandInfo getBandInfoFromName(String bandName) {
        for (BandInfo band: BandInfo.values()) {
            if (band.name.equals(bandName)) {
                return band;
            }
        }
        return null;
    }

    /**
     * Get BandInfo from band name
     * @param bandName band name
     * @return the band having the given name. Null if not found
     */
    public static BandInfo getBandInfoFromNameWithB(String bandName) {
        for (BandInfo band: BandInfo.values()) {
            String name = "B" + band.getName2Digit();
            if (name.equals(bandName)) {
                return band;
            }
        }
        return null;
    }

    /**
     * Get BandInfo from band index
     * @param bandIndex band index (from 0 to 12)
     * @return the band having the given index (from 0 to 12). Null if not found
     */
    public static BandInfo getBandInfoFromIndex(int bandIndex) {
        int nbBand = BandInfo.values().length;
        if (bandIndex < 0 || bandIndex >= nbBand) {
            return null;
        }
        return BandInfo.values()[bandIndex];
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the name
     */
    public String getNameWithB() {
        return "B" + getName2Digit();
    }

    /**
     * @return the name
     */
    public String getName2Digit() {
        if (name.length() == 1) {
            return "0" + name;
        }
        return name;
    }

    /**
     * @return the pixel height
     */
    public int getPixelHeight() {
        return pixelHeight;
    }

    /**
     * Return VNIR or SWIR depending on the band
     * @return VNIR or SWIR depending on the band
     */
    public String getSpaMod() {
        String returned = "VNIR";
        switch (this) {
        case BAND_10:
        case BAND_11:
        case BAND_12:
            returned = "SWIR";
            break;

        default:
            break;
        }
        return returned;
    }

    /**
     * Get a List of all BandInfo
     * @return
     */
    public static List<BandInfo> getAllBandInfo() {
        List<BandInfo> bandInfoList = new ArrayList<>();
        for (BandInfo bandInfo: BandInfo.values()) {
            bandInfoList.add(bandInfo);
        }
        return bandInfoList;
    }
}
