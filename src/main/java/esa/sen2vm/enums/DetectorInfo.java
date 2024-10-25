package esa.sen2vm.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Information on detector
 * @author Guylaine Prat
 */
public enum DetectorInfo {
    DETECTOR_1("01", 0),
    DETECTOR_2("02", 1),
    DETECTOR_3("03", 2),
    DETECTOR_4("04", 3),
    DETECTOR_5("05", 4),
    DETECTOR_6("06", 5),
    DETECTOR_7("07", 6),
    DETECTOR_8("08", 7),
    DETECTOR_9("09", 8),
    DETECTOR_10("10", 9),
    DETECTOR_11("11", 10),
    DETECTOR_12("12", 11);

    /**
     * Detector name
     */
    private String name = null;

    /**
     * Detector index
     */
    private int index = 0;

    /**
     * Private constructor
     * @param name detector name
     * @param resolution detector resolution
     */
    private DetectorInfo(String name, int index) {
        this.name = name;
        this.index = index;
    }

    /**
     * Get DetectorInfo from detector name (value from "01" to "12", with 2 digits)
     * @param detectorName detector name
     * @return the detector having the given name. Null if not found
     */
    public static DetectorInfo getDetectorInfoFromName(String detectorName) {
        for (DetectorInfo detector : DetectorInfo.values()) {
            if (detector.name.equals(detectorName)) {
                return detector;
            }
        }
        return null;
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
     * Get a List of all DetectorInfo
     * @return
     */
    public static List<DetectorInfo> getAllDetectorInfo() {
        List<DetectorInfo> detectorInfoList = new ArrayList<>();
        for (DetectorInfo detectorInfo : DetectorInfo.values()) {
            detectorInfoList.add(detectorInfo);
        }
        return detectorInfoList;
    }
}
