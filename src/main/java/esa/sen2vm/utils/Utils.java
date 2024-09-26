package esa.sen2vm.utils;

import org.orekit.data.DataContext;
import org.orekit.data.LazyLoadedDataContext;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.EOPEntry;
import org.orekit.frames.EOPHistoryLoader;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.models.earth.weather.GlobalPressureTemperature2Model;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.propagation.semianalytical.dsst.utilities.JacobiPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.NewcombOperators;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.lang.Math;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;

import esa.sen2vm.utils.Sen2VMConstants;

/**
 * Manager for SAD file
 */

public class Utils {

    public static List<EOPEntry> buildEOPList(IERSConventions conventions, ITRFVersion itrfType,
                                              AbsoluteDate absDate, double dt, double x, double y) {
        TimeScale utc = DataContext.getDefault().getTimeScales().getUTC();
        double jd = absDate.getComponents(utc).offsetFrom(DateTimeComponents.JULIAN_EPOCH) / Constants.JULIAN_DAY;
        double mjd = jd - Sen2VMConstants.JD_TO_MJD;

        final List<EOPEntry> list = new ArrayList<EOPEntry>();
        for (int i = -5; i <= 5; i++) {
            double mjd_inc = mjd + i;
            list.add(new EOPEntry((int) mjd_inc, dt, Sen2VMConstants.lod,
                                  Sen2VMConstants.ARC_SECONDS_TO_RADIANS * x,
                                  Sen2VMConstants.ARC_SECONDS_TO_RADIANS * y,
                                  Sen2VMConstants.ddPsi, Sen2VMConstants.ddEps,
                                  Sen2VMConstants.dx, Sen2VMConstants.dy, itrfType,
                                  AbsoluteDate.createMJDDate((int) mjd_inc, 0.0, utc)));
        }
        return list;
    }

    public static void setLoaders(IERSConventions conventions, List<EOPEntry> eop) {

        FramesFactory.addEOPHistoryLoader(conventions, new EOPHistoryLoader() {
            public void fillHistory(IERSConventions.NutationCorrectionConverter converter,
                                    SortedSet<EOPEntry> history) {
                history.addAll(eop);
            }
        });
    }
}