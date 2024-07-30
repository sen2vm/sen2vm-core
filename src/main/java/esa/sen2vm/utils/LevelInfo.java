package esa.sen2vm;

/**
 * Information on detector
 */
public enum LevelInfo {
    L0("L0", 0),
    L1A("L1A", 1),
    L1B("L1B", 2),
    L1C("L1C", 3);

    /**
     * Level name
     */
    protected String name = null;

    /**
     * Level index
     */
    protected int index = 0;

    /**
     * Private constructor
     * @param name detector name
     * @param resolution detector resolution
     */
    private LevelInfo(String name, int index) {
        this.name = name;
        this.index = index;
    }

    /**
     * Get DetectorInfo from detector name
     * @param detectorName detector name
     * @return the detector having the given name. Null if not found
     */
    public static LevelInfo getLevelInfoFromName(String detectorName) {
        for (LevelInfo detector : LevelInfo.values()) {
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
    public static LevelInfo getLevelInfoFromIndex(int detectorIndex) {
        int nbDetector = LevelInfo.values().length;
        if (detectorIndex < 0 || detectorIndex >= nbDetector) {
            return null;
        }
        return LevelInfo.values()[detectorIndex];
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
}
