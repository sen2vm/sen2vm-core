package esa.sen2vm;

import java.util.ArrayList;
import java.util.List;

/**
 * Information on detector
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
    protected String name = null;

    /**
     * Detector index
     */
    protected int index = 0;

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
     * Get DetectorInfo from sensor name
     * @param sensorName sensor name
     * @return the detector having the given name. Null if not found
     */
    public static DetectorInfo getDetectorInfoFromSensorName(String sensorName) {
        int dIndex = sensorName.lastIndexOf('D');
        String detectorName = sensorName.substring(dIndex + 1, dIndex + 3);
        return getDetectorInfoFromName(detectorName);
    }

    /**
     * Get DetectorInfo from detector name
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
     * Get DetectorInfo from detector index
     * @param detectorIndex detector index (from 0 to 11)
     * @return the detector having the given index (from 0 to 11). Null if not found
     */
    public static DetectorInfo getDetectorInfoFromIndex(int detectorIndex) {
        int nbDetector = DetectorInfo.values().length;
        if (detectorIndex < 0 || detectorIndex >= nbDetector) {
            return null;
        }
        return DetectorInfo.values()[detectorIndex];
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
