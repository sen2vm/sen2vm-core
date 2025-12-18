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

package esa.sen2vm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.orekit.data.DataContext;
import org.orekit.frames.EOPEntry;
import org.orekit.frames.EOPHistoryLoader;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * Class to manage IERS information
 */
public class IERSutils
{
    /**
     * Build the EOP list
     * @param itrfType ITRF version
     * @param absDate absolute date
     * @param dt UT1-UTC in seconds
     * @param x X component of pole motion
     * @param y Y component of pole motion
     * @return list of EOP
     */
    public static List<EOPEntry> buildEOPList(ITRFVersion itrfType,
                                              AbsoluteDate absDate, double dt, double x, double y)
    {
        TimeScale utc = DataContext.getDefault().getTimeScales().getUTC();
        double jd = absDate.getComponents(utc).offsetFrom(DateTimeComponents.JULIAN_EPOCH) / Constants.JULIAN_DAY;
        double mjd = jd - Sen2VMConstants.JD_TO_MJD;

        final List<EOPEntry> list = new ArrayList<EOPEntry>();
        for (int i = -Sen2VMConstants.EOP_MARGIN; i <= Sen2VMConstants.EOP_MARGIN; i++)
        {
            double mjd_inc = mjd + i;
            list.add(new EOPEntry((int) mjd_inc, dt, Sen2VMConstants.lod,
                                  org.orekit.utils.Constants.ARC_SECONDS_TO_RADIANS * x,
                                  org.orekit.utils.Constants.ARC_SECONDS_TO_RADIANS * y,
                                  Sen2VMConstants.ddPsi, Sen2VMConstants.ddEps,
                                  Sen2VMConstants.dx, Sen2VMConstants.dy, itrfType,
                                  AbsoluteDate.createMJDDate((int) mjd_inc, 0.0, utc)));
        }
        return list;
    }

    public static void setLoaders(IERSConventions conventions, List<EOPEntry> eop)
    {
        FramesFactory.addEOPHistoryLoader(conventions, new EOPHistoryLoader()
        {
            public void fillHistory(IERSConventions.NutationCorrectionConverter converter,
                                    SortedSet<EOPEntry> history)
            {
                history.addAll(eop);
            }
        });
    }
}