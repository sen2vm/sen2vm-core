package esa.sen2vm.enums;

import java.util.ArrayList;
import java.util.List;


/**
 * Information on band
 * @author Guylaine Prat
 */
public enum BandInfo {
    BAND_1("1"),
    BAND_2("2"),
    BAND_3("3"),
    BAND_4("4"),
    BAND_5("5"),
    BAND_6("6"),
    BAND_7("7"),
    BAND_8("8"),
    BAND_8A("8A"),
    BAND_9("9"),
    BAND_10("10"),
    BAND_11("11"),
    BAND_12("12");

    /**
     * Band name
     */
    private String name = null;

    /**
     * Band index
     */
    private int index = 0;


    /**
     * Private constructor
     * @param name band name
     */
    private BandInfo(String name) {
        this.name = name;
    }

    /**
     * Get BandInfo from band name with B (value from "B01",.., "B08", "B8A", ..., "B12") 
     * @param bandName band name
     * @return the band having the given name. Null if not found
     */
    public static BandInfo getBandInfoFromNameWithB(String bandName) {
        for (BandInfo band : BandInfo.values()) {
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
//           LOGGER.error("Invalid band index " + bandIndex + ". Available range from 0 to " + nbBand);
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
    public String getName2Digit() {
        if (name.length() == 1) {
            return "0" + name;
        }
        return name;
    }

    /**
     * Get a List of all BandInfo
     * @return
     */
    public static List<BandInfo> getAllBandInfo() {
        List<BandInfo> bandInfoList = new ArrayList<>();
        for (BandInfo bandInfo : BandInfo.values()) {
            bandInfoList.add(bandInfo);
        }
        return bandInfoList;
    }
}
