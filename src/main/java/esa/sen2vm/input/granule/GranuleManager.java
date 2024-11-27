package esa.sen2vm.input.granule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.rugged.linesensor.LineDatation;
import org.orekit.rugged.linesensor.LinearLineDatation;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.Sen2VMConstants;

import org.sxgeo.input.datamodels.DataSensingInfos;
import org.sxgeo.exception.SXGeoException;

import https.psd_15_sentinel2_eo_esa_int.psd.s2_pdi_level_1b_granule_metadata.Level1B_Granule;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_GEOMETRIC_INFO;

/**
 * Manager for SAD file
 */

public class GranuleManager {
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(GranuleManager.class.getName());

    /**
     * File path granule file
     */
    protected File granuleFile = null;

    /**
     * Pixel origin
     */
    protected int pixelOrigin;

    /**
     * Granule Start Position in DS
     */
    protected int granulePosition;

    /**
     * Size of the granule for band res = 10
     */
    protected int[] sizeRes10 = null;

    /**
     * Size of the granule for band res = 20
     */
    protected int[] sizeRes20 = null;

    /**
     * Size of the granule for band res = 60
     */
    protected int[] sizeRes60 = null;

    /**
     * Constructor from SAD XML file
     * @param granuleFilePath path to SAD XML file
     * @throws Sen2VMException
     */
    public GranuleManager(String granuleFilePath) throws Sen2VMException {
        this.granuleFile = new File(granuleFilePath);
        loadFile(granuleFilePath);
    }

    /**
     * Load file using given context
     * @param granuleFilePath path (name of the package) containing the class that will be used to load the XML file
     * @throws Sen2VMException
     */
    protected void loadFile(String granuleFilePath) throws Sen2VMException {
        // Load SAD xml file

        try {
            // Load SAD xml file
            JAXBContext jaxbContext = JAXBContext.newInstance(Level1B_Granule.class.getPackage().getName());
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            File granuleFile = new File(granuleFilePath);
            JAXBElement<Level1B_Granule> jaxbElement = (JAXBElement<Level1B_Granule>) jaxbUnmarshaller.unmarshal(granuleFile);
            Level1B_Granule l1B_granule = null;
            l1B_granule = jaxbElement.getValue();

            A_GEOMETRIC_INFO geometricInfo = l1B_granule.getGeometric_Info();
            pixelOrigin = geometricInfo.getGranule_Footprint().getPIXEL_ORIGIN();
            granulePosition = geometricInfo.getGranule_Position().getPOSITION();
            List<A_GEOMETRIC_INFO.Granule_Dimensions.Size> list_res = geometricInfo.getGranule_Dimensions().getSize();

            for (A_GEOMETRIC_INFO.Granule_Dimensions.Size res : list_res) {
                if (res.getResolution() == Sen2VMConstants.RESOLUTION_10M) {
                    sizeRes10 = new int[]{res.getNROWS(), res.getNCOLS()};
                } else if (res.getResolution() == Sen2VMConstants.RESOLUTION_20M) {
                    sizeRes20 = new int[]{res.getNROWS(), res.getNCOLS()};
                } else {
                    sizeRes60 = new int[]{res.getNROWS(), res.getNCOLS()};
                }
            }
        } catch (JAXBException e){
            Sen2VMException exception = new Sen2VMException(e);
            throw exception;
        }
    }


    public int getPixelOrigin() {
        return pixelOrigin;
    }

     public int getGranulePosition() {
        return granulePosition;
    }

     public int[] getSizeRes10() {
        return sizeRes10;
    }

    public int[] getSizeRes20() {
        return sizeRes20;
    }

    public int[] getSizeRes60() {
        return sizeRes60;
    }
    

}