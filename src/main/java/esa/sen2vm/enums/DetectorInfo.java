/** Copyright 2024-2025, CS GROUP, https://www.cs-soprasteria.com/
*
* This file is part of the Sen2VM Core project
*     https://gitlab.acri-cwa.fr/opt-mpc/s2_tools/sen2vm/sen2vm-core
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.*/

package esa.sen2vm.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Information on detector
 */
public enum DetectorInfo
{
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
     * @param name detector name, e.g. "01", "02", ... "12"
     * @param index detector index (from 0 to 11)
     */
    private DetectorInfo(String name, int index)
    {
        this.name = name;
        this.index = index;
    }

    /**
     * Get DetectorInfo from sensor name
     * @param sensorName sensor name in the shape of BXX/DXX, e.g. "B01/D01", "B01/D02", ... "B01/D12"
     * @return the detector having the given name. Null if not found
     */
    public static DetectorInfo getDetectorInfoFromSensorName(String sensorName)
    {
        int dIndex = sensorName.lastIndexOf('D');
        String detectorName = sensorName.substring(dIndex + 1, dIndex + 3);
        return getDetectorInfoFromName(detectorName);
    }

    /**
     * Get DetectorInfo from detector name
     * @param detectorName detector name, e.g. "01", "02", ... "12".
     * @return the detector having the given name. Null if not found
     */
    public static DetectorInfo getDetectorInfoFromName(String detectorName)
    {
        for (DetectorInfo detector: DetectorInfo.values())
        {
            if (detector.name.equals(detectorName))
            {
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
    public static DetectorInfo getDetectorInfoFromIndex(int detectorIndex)
    {
        int nbDetector = DetectorInfo.values().length;
        if (detectorIndex < 0 || detectorIndex >= nbDetector)
        {
            return null;
        }
        return DetectorInfo.values()[detectorIndex];
    }

    /**
     * @return the index
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the name
     */
    public String getNameWithD()
    {
        return "D" + name;
    }

    /**
     * Get a List of all DetectorInfo
     * @return
     */
    public static List<DetectorInfo> getAllDetectorInfo()
    {
        List<DetectorInfo> detectorInfoList = new ArrayList<>();
        for (DetectorInfo detectorInfo: DetectorInfo.values())
        {
            detectorInfoList.add(detectorInfo);
        }
        return detectorInfoList;
    }
}