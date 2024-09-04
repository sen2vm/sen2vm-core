package esa.sen2vm;

import java.io.File;
import java.util.ArrayList;
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

import org.hipparchus.analysis.polynomials.PolynomialFunction;
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

import org.sxgeo.input.datamodels.DataSensingInfos;
import org.sxgeo.input.datamodels.RefiningInfo;
import org.sxgeo.input.datamodels.sensor.Sensor;
import org.sxgeo.exception.SXGeoException;

import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_AUXILIARY_DATA_INFO_DSL1B;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_GIPP_LIST;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_GIPP_LIST.GIPP_FILENAME;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_ATTITUDE_DATA_INV.Corrected_Attitudes.Values;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_EPHEMERIS_DATA_INV.GPS_Points_List.GPS_Point;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_IMAGE_DATA_INFO_DSL0;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_IMAGE_DATA_INFO_DSL1A;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_IMAGE_DATA_INFO_DSL1B;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_ACQUISITION_CONFIGURATION.TDI_Configuration_List.TDI_CONFIGURATION;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_GENERAL_INFO_DS;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_QUICKLOOK_DESCRIPTOR;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_REFINED_CORRECTIONS;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_SENSOR_CONFIGURATION;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_SENSOR_CONFIGURATION.Time_Stamp;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_TIME_STAMP;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_TIME_STAMP.Band_Time_Stamp.Detector;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.sy.misc.AN_UNCERTAINTIES_XYZ_TYPE;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.sy.misc.A_POLYNOMIAL_MODEL;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.sy.misc.A_ROTATION_TRANSLATION_HOMOTHETY_UNCERTAINTIES_TYPE_LOWER_CASE;
import https.psd_15_sentinel2_eo_esa_int.psd.s2_pdi_level_1b_datastrip_metadata.Level1B_DataStrip;

/**
 * Manager for SAD file
 */

public class DataStripManager {
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(Sen2VM.class.getName());

    /**
     * File path of datastrip file
     */
    protected File dsFile = null;

    /**
     * Datastrip for L1B
     */
    protected Level1B_DataStrip l1B_datastrip = null;

    /**
     * Sensor configuration
     */
    protected A_SENSOR_CONFIGURATION sensorConfiguration = null;

    /**
     * Info about auxiliary data like GIPP and IERS
     */
    protected AN_AUXILIARY_DATA_INFO_DSL1B auxiliaryDataInfo = null;

    /**
     * List of Pair (date, rotation)
     */
    protected List<TimeStampedAngularCoordinates> satelliteQList = null;
    /**
     * List of Pair (date, PV coords)
     */
    protected List<TimeStampedPVCoordinates> satellitePVList = null;

    /**
     * Min granule line found in Datastrip (10m)
     */
    protected Map<String, Double> minLinePerSensor = null;
    /**
     * Max granule line found in Datastrip (10m)
     */
    protected Map<String, Double> maxLinePerSensor = null;

    /**
     * Time reference
     */
    TimeScale gps = null;

    /**
     * Min date from Quaternions
     */
    protected AbsoluteDate qMinDate = null;
    /**
     * Max date from Quaternions
     */
    protected AbsoluteDate qMaxDate = null;

    /**
     * Min date from PV
     */
    protected AbsoluteDate pvMinDate = null;
    /**
     * Max date from PV
     */
    protected AbsoluteDate pvMaxDate = null;

    /**
     * DataSensingInfos
     */
    private DataSensingInfos dataSensingInfos = null;

    /**
     * Refining information (null is not present or not asked for)
     */
    private RefiningInfo refiningInfo = new RefiningInfo();

    /**
     * Refined Corrections List for L1B data
     */
    private List<A_REFINED_CORRECTIONS> refinedCorrectionsListL1B;

    /**
     * Constructor from SAD XML file
     * @param dsFilePath path to SAD XML file
     * @param iersDirectoryPath path to the IERS directory
     * @param activateAvailableRefining if true use refining parameters present in the datastrip, else will ignore available refining
     * @throws Sen2VMException
     */
    protected DataStripManager(String dsFilePath, String iersDirectoryPath, Boolean activateAvailableRefining) throws Sen2VMException {
        this.dsFile = new File(dsFilePath);
        gps = TimeScalesFactory.getGPS();
        loadFile(dsFilePath, iersDirectoryPath, activateAvailableRefining);
    }

    /**
     * Load datastrip file
     * @param dsFilePath path to SAD XML file
     * @param iersDirectoryPath path to the IERS directory
     * @param activateAvailableRefining if true use refining parameters present in the datastrip, else will ignore available refining
     * @throws Sen2VMException
     */
    protected void loadFile(String dsFilePath, String iersDirectoryPath, Boolean activateAvailableRefining) throws Sen2VMException {
        try {
            // Load SAD xml file
            JAXBContext jaxbContext = JAXBContext.newInstance(Level1B_DataStrip.class.getPackage().getName());
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            File datastripFile = new File(dsFilePath);
            JAXBElement<Level1B_DataStrip> jaxbElement = (JAXBElement<Level1B_DataStrip>) jaxbUnmarshaller.unmarshal(datastripFile);
            l1B_datastrip = jaxbElement.getValue();

            sensorConfiguration = l1B_datastrip.getImage_Data_Info().getSensor_Configuration();

            auxiliaryDataInfo = l1B_datastrip.getAuxiliary_Data_Info();

            initOrekitRessources(iersDirectoryPath);

            // Test if we need to take refining data into account according to the flag
            if (activateAvailableRefining) {
                if (l1B_datastrip.getImage_Data_Info().getGeometric_Info().getImage_Refining() != null)
                {
                    String refinedType = l1B_datastrip.getImage_Data_Info().getGeometric_Info().getImage_Refining().getFlag();
                    if (refinedType.equalsIgnoreCase("REFINED")) {
                        // get the list of corrections
                        refinedCorrectionsListL1B = l1B_datastrip.getImage_Data_Info().getGeometric_Info().getRefined_Corrections_List().getRefined_Corrections();

                        // get the datastrip start and stop time
                        A_GENERAL_INFO_DS.Datastrip_Time_Info dataStripTimeInfo = l1B_datastrip.getGeneral_Info().getDatastrip_Time_Info();

                        // update the refiningInfo
                        readRefinedCorrections(dataStripTimeInfo, refinedCorrectionsListL1B);
                    }
                }
            }

            // Get quaternions and positions/velocities list from xml file
            computeSatelliteQList();
            computeSatellitePVList(activateAvailableRefining);
            computeMinMaxLinePerSensor();

            // Instanciate dataSensingInfos that will be use for SimpleLocEngine
            dataSensingInfos = new DataSensingInfos(satelliteQList, satellitePVList, minLinePerSensor, maxLinePerSensor);

        } catch (JAXBException e) {
            throw new Sen2VMException(e);
        }  catch (OrekitException e) {
            throw new Sen2VMException(e);
        } catch (SXGeoException e) {
            throw new Sen2VMException(e);
        }
    }

    /**
     * Load IERS file
     * @param iersDirectoryPath path to the IERS directory
     * @throws Sen2VMException
     */
    public static synchronized void initOrekitRessources(String iersFilePath) throws Sen2VMException {
		try {
		    // Get IERS file and instantiate FramesFactory with it
			File iersFile = new File(iersFilePath);
			FramesFactory.addDefaultEOP2000HistoryLoaders(null, null, null, null, iersFile.getName());

			// When using a single IERS A bulletin some gaps may arise : to allow the use of such bulletin,
			// we fix the EOP continuity threshold to one year instead of the normal gap ...
			FramesFactory.setEOPContinuityThreshold(Constants.JULIAN_YEAR);

			// set up default Orekit data
			File orekitDataDir = new File(System.getProperty("user.dir") + "/" + Sen2VMConstants.OREKIT_DATA_DIR);
			if (orekitDataDir == null || (!orekitDataDir.exists())) {
			    throw new Sen2VMException("Orekit data not found");
			}
			DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitDataDir));
		} catch (Exception e) {
			throw new Sen2VMException("Something went wrong during initialization of IERS and orekit ressources ", e);
		}
	}


	/**
	 * Fill
	 * @param dataStripTimeInfo
	 * @param refinedCorrectionsListL1
	 */
	private void readRefinedCorrections(A_GENERAL_INFO_DS.Datastrip_Time_Info dataStripTimeInfo, List<A_REFINED_CORRECTIONS> refinedCorrectionsListL1) throws OrekitException {

        // compute acquisition center time:
        // the refining corrections are computed related to this time
        AbsoluteDate acquisitionCenterTime = computeAcquisitionCenter(dataStripTimeInfo);
        this.refiningInfo.setAcquisitionCenterTime(acquisitionCenterTime);

        // Read the refining polynoms
        // --------------------------
        // test if the whole list is not empty !
        if (refinedCorrectionsListL1 != null){
            // TBN: the list will always contain only one item although the XSD structure (no node name="focal_plane_id_unique")
            for (https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_REFINED_CORRECTIONS refinedCorrections: refinedCorrectionsListL1){

                // The only corrections to be taken into account are defined in GEOREF-DPM par 4.4.4 Issue 3.2
                // -------------------------------------------------------------------------------------------
                // Spacecraft position
                AN_UNCERTAINTIES_XYZ_TYPE spacecraftPositionUncertainties =  refinedCorrections.getSpacecraft_Position();
                // Spacecraft/Piloting to MSI transformation
                A_ROTATION_TRANSLATION_HOMOTHETY_UNCERTAINTIES_TYPE_LOWER_CASE msiStateUncertainties = refinedCorrections.getMSI_State();
                // MSI to Focal plane transformation
                List<A_REFINED_CORRECTIONS.Focal_Plane_State> focalPlaneStateUncertaintiesList = refinedCorrections.getFocal_Plane_State();
                // Focal plane to detector
                // in the current XSD:
                // no node SENSOR (for focal plane to detector transformations)
                //      => impossible to code this part without XSD definition

                if (spacecraftPositionUncertainties != null){
                    // Spacecraft position (expressed in meters) in the local spacecraft reference frame (EVG Euclidium state)

                    // Init of the polynomial functions for each correction
                    A_POLYNOMIAL_MODEL ephemerisXpolynom = spacecraftPositionUncertainties.getX();
                    this.refiningInfo.setEphemerisXpolyFunc(createPolynomialFunction(ephemerisXpolynom));

                    A_POLYNOMIAL_MODEL ephemerisYpolynom = spacecraftPositionUncertainties.getY();
                    this.refiningInfo.setEphemerisYpolyFunc(createPolynomialFunction(ephemerisYpolynom));

                    A_POLYNOMIAL_MODEL ephemerisZpolynom = spacecraftPositionUncertainties.getZ();
                    this.refiningInfo.setEphemerisZpolyFunc(createPolynomialFunction(ephemerisZpolynom));
                }

                // Fix the transformation angle signs
                // TODO see what to do in case there is another convention in the definition of the angles
                int[] refiningMSIstateAnglesSigns = {1, 1, 1};
                int[] refiningFocalPlaneStateAngleSigns = {1, 1, 1};

                if (msiStateUncertainties != null){
                    // Spacecraft/Piloting to MSI transformation

                    // Init of the polynomial functions for each correction
                    AN_UNCERTAINTIES_XYZ_TYPE spaceCraftToMSIRotation = msiStateUncertainties.getRotation();
                    // rotation parts
                    A_POLYNOMIAL_MODEL spacecraftToMSIRotationX = spaceCraftToMSIRotation.getX();
                    this.refiningInfo.setSpacecraftToMSITransfoMatrixXFunc(createAnglePolynomialFunction(spacecraftToMSIRotationX,refiningMSIstateAnglesSigns[0]));
                    A_POLYNOMIAL_MODEL spacecraftToMSIRotationY = spaceCraftToMSIRotation.getY();
                    this.refiningInfo.setSpacecraftToMSITransfoMatrixYFunc(createAnglePolynomialFunction(spacecraftToMSIRotationY,refiningMSIstateAnglesSigns[1]));
                    A_POLYNOMIAL_MODEL spacecraftToMSIRotationZ = spaceCraftToMSIRotation.getZ();
                    this.refiningInfo.setSpacecraftToMSITransfoMatrixZFunc(createAnglePolynomialFunction(spacecraftToMSIRotationZ,refiningMSIstateAnglesSigns[2]));

                    // homothety part
                    // Only Z axis homothety
                    if (msiStateUncertainties.getHomothety() != null)
                    {
                      A_POLYNOMIAL_MODEL spacecraftToMSIhomothetyZ = msiStateUncertainties.getHomothety().getZ();
                      this.refiningInfo.setSpacecraftToMSIHomothetyZFunc(createPolynomialFunction(spacecraftToMSIhomothetyZ));
                    }
                }

                if (focalPlaneStateUncertaintiesList != null){
                    // MSI to Focal plane transformation
                    // There will between 0 and 2 Focal_Plane_State corrections

                    // Init of the polynomial functions for each correction
                    HashMap<Sensor, PolynomialFunction> msiToFocalPlaneRotationX = new HashMap<Sensor, PolynomialFunction>();
                    HashMap<Sensor, PolynomialFunction> msiToFocalPlaneRotationY = new HashMap<Sensor, PolynomialFunction>();
                    HashMap<Sensor, PolynomialFunction> msiToFocalPlaneRotationZ = new HashMap<Sensor, PolynomialFunction>();
                    HashMap<Sensor, PolynomialFunction> msiToFocalPlaneHomothety = new HashMap<Sensor, PolynomialFunction>();

                    for (https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_REFINED_CORRECTIONS.Focal_Plane_State focalPlaneStateUncertainties : focalPlaneStateUncertaintiesList) {
                        if (focalPlaneStateUncertainties != null){
                            String focalPlaneName = focalPlaneStateUncertainties.getFocalPlaneId().value();
                            Sensor sensor = new Sensor(focalPlaneName, null, null, 0.0, null, null, null);

                            // rotation parts
                            AN_UNCERTAINTIES_XYZ_TYPE msiToFocalPlaneRotationXYZ = focalPlaneStateUncertainties.getRotation();

                            // Check if the node exist (different from null)
                            if (msiToFocalPlaneRotationXYZ != null) {
                                msiToFocalPlaneRotationX.put(sensor, createAnglePolynomialFunction(msiToFocalPlaneRotationXYZ.getX(),refiningFocalPlaneStateAngleSigns[0]));
                                msiToFocalPlaneRotationY.put(sensor, createAnglePolynomialFunction(msiToFocalPlaneRotationXYZ.getY(),refiningFocalPlaneStateAngleSigns[1]));
                                msiToFocalPlaneRotationZ.put(sensor, createAnglePolynomialFunction(msiToFocalPlaneRotationXYZ.getZ(),refiningFocalPlaneStateAngleSigns[2]));
                            }

                            // homothety part
                            AN_UNCERTAINTIES_XYZ_TYPE msiToFocalPlaneHomothetyXYZ = focalPlaneStateUncertainties.getHomothety();

                            // Check if the node exist (different from null)
                            if (msiToFocalPlaneHomothetyXYZ != null){
                                // Only Z axis homothety
                                PolynomialFunction msiToFocalPlaneHomothetyZ = createPolynomialFunction(focalPlaneStateUncertainties.getHomothety().getZ());

                                // Check if the node exist (different from null)
                                if (msiToFocalPlaneHomothetyZ != null){
                                    // ... for current focal plane
                                    msiToFocalPlaneHomothety.put(sensor, msiToFocalPlaneHomothetyZ);
                                }
                            }

                            // no translation to be taken into account
                        }
                    }

                    // fill in the refining info for all the focal plane (SWIR and VNIR)
                    this.refiningInfo.setMsiToFocalPlaneTransfoMatrixXFunc(msiToFocalPlaneRotationX);
                    this.refiningInfo.setMsiToFocalPlaneTransfoMatrixYFunc(msiToFocalPlaneRotationY);
                    this.refiningInfo.setMsiToFocalPlaneTransfoMatrixZFunc(msiToFocalPlaneRotationZ);
                    this.refiningInfo.setMsiToFocalPlaneHomothetyZFunc(msiToFocalPlaneHomothety);
                }
            }
        }
    }

    /**
     * Fill satelliteQList using values from SAD XML file
     * @throws Sen2VMException
     */
    protected void computeSatelliteQList() throws Sen2VMException {
        satelliteQList = new ArrayList<TimeStampedAngularCoordinates>();

        // This HashSet is used only to check a duplicate date in the quaternions
        HashSet<AbsoluteDate> dateSet = new HashSet<AbsoluteDate>();

        // Loop over Corrected Attitudes value list
        List<Values> correctedAttitudeValueList = l1B_datastrip.getSatellite_Ancillary_Data_Info().getAttitudes().getCorrected_Attitudes().getValues();
        for (Values values : correctedAttitudeValueList) {
            XMLGregorianCalendar gpsTime = values.getGPS_TIME();
            if (gpsTime == null) {
                throw new Sen2VMException(Sen2VMConstants.ERROR_QUATERNION_NULL_GPS);
            }
            // Extract Quaternion values from XML
            AbsoluteDate attitudeDate = new AbsoluteDate(gpsTime.toString(), gps);
            List<Double> quaternionValues = values.getQUATERNION_VALUES();
            Double q1 = quaternionValues.get(0);
            Double q2 = quaternionValues.get(1);
            Double q3 = quaternionValues.get(2);
            Double q0 = quaternionValues.get(3);

            Rotation rotation = new Rotation(q0, q1, q2, q3, true);
            TimeStampedAngularCoordinates pair = new TimeStampedAngularCoordinates(attitudeDate, rotation, Vector3D.ZERO, Vector3D.ZERO);

            if (dateSet.contains(attitudeDate)) {
                // duplicate data for the current ephemeris date => we aldeady add this quaternion => we just ignore it
                LOGGER.warning("Duplicate quaternion with date : " + values.getGPS_TIME().toString());
            } else {
                dateSet.add(attitudeDate);
                satelliteQList.add(pair);
            }
        }
    }

    /**
     * Fill satellitePVList using values from SAD XML file
     * @throws Sen2VMException
     */
    protected void computeSatellitePVList(Boolean activateAvailableRefining) throws Sen2VMException {
        try {
            // Init of used frames
            Frame eme2000 = FramesFactory.getEME2000();
            Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            satellitePVList = new ArrayList<TimeStampedPVCoordinates>();
            HashSet<AbsoluteDate> dateSet = new HashSet<AbsoluteDate>();

            // Loop over ephemeris value list
            List<GPS_Point> ephemerisGpsPointList = l1B_datastrip.getSatellite_Ancillary_Data_Info().getEphemeris().getGPS_Points_List().getGPS_Point();
            for (GPS_Point ephemeris : ephemerisGpsPointList) {
                XMLGregorianCalendar gpsTime = ephemeris.getGPS_TIME();
                if (gpsTime == null) {
                    continue;
                }
                // extract PV from XML objects
                AbsoluteDate ephemerisDate = new AbsoluteDate(gpsTime.toString(), gps);
                List<Long> positionValues = ephemeris.getPOSITION_VALUES().getValue();
                List<Long> velocityValues = ephemeris.getVELOCITY_VALUES().getValue();

                // Get position in ITRF (defined in mm)
                Double px = positionValues.get(0).doubleValue() / 1000d;
                Double py = positionValues.get(1).doubleValue() / 1000d;
                Double pz = positionValues.get(2).doubleValue() / 1000d;
                Vector3D position = new Vector3D(px, py, pz);

                // Get velocity in ITRF (defined in mm/s)
                Double vx = velocityValues.get(0).doubleValue() / 1000d;
                Double vy = velocityValues.get(1).doubleValue() / 1000d;
                Double vz = velocityValues.get(2).doubleValue() / 1000d;
                Vector3D velocity = new Vector3D(vx, vy, vz);
                PVCoordinates pvITRF = new PVCoordinates(position, velocity);

                // Compute the transformation from ITRF to EME2000 at the ephemerisDate
                Transform transform = itrf.getTransformTo(eme2000, ephemerisDate);

                PVCoordinates pvEME2000 = transform.transformPVCoordinates(pvITRF);

                // Convert PV from ITRF to EME2000
                TimeStampedPVCoordinates pair = new TimeStampedPVCoordinates(ephemerisDate, pvEME2000.getPosition(), pvEME2000.getVelocity(), Vector3D.ZERO);

                if (dateSet.contains(ephemerisDate)) {
                    // duplicate data for the current ephemeris date => we aldeady add this pv coordinate => we just ignore it
                    System.out.println("Duplicate PV !" + ephemeris.getGPS_TIME().toString());
                } else {
                    // Compute refining corrections before updating satellite PVlist
                    if (activateAvailableRefining) {
                        // Test if the polynoms and the acquisition center time  are not null before applying them
                        if (refiningInfo.getEphemerisXpolyFunc() != null &&
                            refiningInfo.getEphemerisYpolyFunc() != null &&
                            refiningInfo.getEphemerisZpolyFunc() != null &&
                            refiningInfo.getAcquisitionCenterTime() != null) {

                            // Simple way to compute the transformation from EME2000 to LOF,
                            // as we need only to perform the transform for the current point
                            Transform EME2000ToLOF = transformFromEME2000toLOF(ephemerisDate, pair);

                            // compute in seconds the delta t wrt acquisition time center
                            double timeFromAcquisitionCenterTime = ephemerisDate.durationFrom(refiningInfo.getAcquisitionCenterTime());

                            // compute the XYZ corrections in LOF (unit : m)
                            double XcorrectionInLOF = refiningInfo.getEphemerisXpolyFunc().value(timeFromAcquisitionCenterTime);
                            double YcorrectionInLOF = refiningInfo.getEphemerisYpolyFunc().value(timeFromAcquisitionCenterTime);
                            double ZcorrectionInLOF = refiningInfo.getEphemerisZpolyFunc().value(timeFromAcquisitionCenterTime);
                            Vector3D posCorrections = new Vector3D(XcorrectionInLOF, YcorrectionInLOF, ZcorrectionInLOF);
                            TimeStampedPVCoordinates pvInLOFCorrections = new TimeStampedPVCoordinates(ephemerisDate, posCorrections, Vector3D.ZERO);
                            TimeStampedPVCoordinates pairCorrected = EME2000ToLOF.getInverse().transformPVCoordinates(pvInLOFCorrections);

                            // update the current pair of PV in EME2000
                            pair = pairCorrected;
                        }
                    }

                    dateSet.add(ephemerisDate);
                    satellitePVList.add(pair);
                }
            }
        } catch (Exception e) {
            throw new Sen2VMException(e);
        }
    }

    /** Get the transform from an inertial frame defining position-velocity and the local orbital frame.
     * @param date current date
     * @param pv position-velocity of the spacecraft in inertial frame EME2000
     * @return transform from the frame where position-velocity are defined to local orbital frame
     */
    protected Transform transformFromEME2000toLOF(final AbsoluteDate date, final PVCoordinates pvEME2000) {

        // compute the translation part of the transform
        final Transform translation = new Transform(date, pvEME2000.negate());

        // compute the rotation from inertial to LOF
        // where LOF is defined by:
        // Z axis aligned with opposite of position, X axis aligned with orbital momentum [cross product of speed vector ^ Z axis]
        Rotation rotationFromEME2000toLOF = new Rotation(pvEME2000.getPosition(), pvEME2000.getMomentum(),
                                                         Vector3D.MINUS_K, Vector3D.PLUS_I);

        // compute the rotation part of the transform
        Vector3D p = pvEME2000.getPosition();
        Vector3D momentum = pvEME2000.getMomentum();
        Transform rotation = new Transform(date, rotationFromEME2000toLOF,
                             new Vector3D(1.0 / p.getNormSq(), rotationFromEME2000toLOF.applyTo(momentum)));

        return new Transform(date, translation, rotation);

    }

    /** Create a polynomial function for refined corrections
     *  from coefficients read in the DIMAP XML file
     * @param XMLpolynomialModel
     * @return
     */
    private PolynomialFunction createPolynomialFunction(A_POLYNOMIAL_MODEL XMLpolynomialModel) {

        PolynomialFunction polyFunction = null;
        if (XMLpolynomialModel != null){

           // coef will be non null
            int coefSize = XMLpolynomialModel.getCOEFFICIENTS().size();
            Double coef[] = new Double[coefSize];

            // fill-in the coef array from the List<Double>
            XMLpolynomialModel.getCOEFFICIENTS().toArray(coef);

            // convert the Double[] to double[] for PolynomialFunction creation
            double[] coefPoly = new double[coefSize];
            for (int i = 0; i < coefSize; i++) {
                coefPoly[i] = coef[i].doubleValue();
             }

            // create the associated polynomial function (any degree is possible !!!)
            polyFunction = new PolynomialFunction(coefPoly);

        }
        return polyFunction;

    }

    /** Create a polynomial function for angle refined corrections
     *  from coefficients read in the DIMAP XML file
     * @param XMLpolynomialModel
     * @param angleSign
     * @return PolynomialFunction
     */
    private PolynomialFunction createAnglePolynomialFunction(A_POLYNOMIAL_MODEL XMLpolynomialModel, int angleSign) {

        PolynomialFunction polyFunction = null;

        if (XMLpolynomialModel != null){
            polyFunction = createPolynomialFunction(XMLpolynomialModel);
            if (angleSign == -1){
                // at this stage polyFunction is not null
                return polyFunction.negate();
            }
        }
        return polyFunction;

    }

    /** Compute the acquisition time center (acquisition mean's time)
     *  between datastrip start time and datastrip end time
     * @param dataStripTimeInfo
     * @throws OrekitException
     */
    private AbsoluteDate computeAcquisitionCenter(A_GENERAL_INFO_DS.Datastrip_Time_Info dataStripTimeInfo) throws OrekitException {

        // get the datastrip acquisition start and stop
        XMLGregorianCalendar datastripStartDateGregorian = dataStripTimeInfo.getDATASTRIP_SENSING_START();
        XMLGregorianCalendar datastripStopDateGregorian = dataStripTimeInfo.getDATASTRIP_SENSING_STOP();
        // convert in AbsoluteDate (with UTC scale)
        AbsoluteDate datastripStartDateUTC = new AbsoluteDate(datastripStartDateGregorian.toString(), TimeScalesFactory.getUTC());
        AbsoluteDate datastripStopDateUTC = new AbsoluteDate(datastripStopDateGregorian.toString(), TimeScalesFactory.getUTC());
        // half datastrip duration (no test performed if the value is > 0)
        double halfDatastripDuration = datastripStopDateUTC.durationFrom(datastripStartDateUTC) / 2.;

        // acquisition center time
        // Polynomial model of refining corrections are computed with that the time centered on this value;
        // i.e. this time is 0 for the polynoms
        return new AbsoluteDate(datastripStartDateUTC, halfDatastripDuration);
    }

    /**
     * Compute min and max date line
     */
    private void computeMinMaxLinePerSensor() {
        minLinePerSensor = new HashMap<String, Double>();
        maxLinePerSensor = new HashMap<String, Double>();
//        for (DetectorInfo detectorInfo: DetectorInfo.getAllDetectorInfo()) {
//            for (BandInfo bandInfo: BandInfo.getAllBandInfo()) {
//                String sensor = bandInfo.getNameWithB() + "/" + detectorInfo.getNameWithD();
//                minLinePerSensor.put(sensor, 1.0);
//                maxLinePerSensor.put(sensor, 1.0);
//            }
//        }
    }

    /*
     * Get DataSensingInfos
     */
    public DataSensingInfos getDataSensingInfos() {
       return dataSensingInfos;
    }

    /**
     * get tdi configuration value for the given band
     * @param bandInfo the band we must find the corresponding tdi configuration for
     * @return tdi configuration for the given band
     */
    public String getTdiConfVal(BandInfo bandInfo) {
        String tdiConfVal = null;
        if (sensorConfiguration != null) {
            List<TDI_CONFIGURATION> tdiConfList = sensorConfiguration.getAcquisition_Configuration().getTDI_Configuration_List().getTDI_CONFIGURATION();
            for (TDI_CONFIGURATION tdiConf : tdiConfList) {
                if (tdiConf.getBandId() == Integer.parseInt(bandInfo.getName())) {
                    // we found the band = get TDI_CONFIGURATION value
                    tdiConfVal = tdiConf.getValue().value();
                }
            }
        }
        return tdiConfVal;
    }

    /**
     * Check if the GIPP version is supported
     * @param gippType is the type of GIPP, can be GIP_SPAMOD or GIP_BLINDP
     * @param gippVersion is the version of the input GIPP
     * @throws Sen2VMException
     */
    public void checkGIPPVersion(String gippType, String gippVersion) throws Sen2VMException {
        boolean compatibleVersion = false;
        if (gippVersion != null) {
            List<A_GIPP_LIST.GIPP_FILENAME> gippList = auxiliaryDataInfo.getGIPP_List().getGIPP_FILENAME();
            for (GIPP_FILENAME gipp_filename : gippList) {
                if (gippType.equals(gipp_filename.getType()) && gippVersion.equals(gipp_filename.getVersion())) {
                    compatibleVersion = true;
                }
            }
        } else {
            throw new Sen2VMException("GIPP version could not be find for " + gippType);
        }

        if (!compatibleVersion) {
            throw new Sen2VMException("GIPP of type " + gippType + " with version " + gippVersion + " is not supported by current datastrip " + dsFile);
        }
    }

    /**
     * Function dedicated to viewing direction GIPPs to check if the GIPP version is supported
     * @param gippFilepath is the GIPP filepath
     * @param gippVersion is the version of the input GIPP
     * @throws Sen2VMException
     */
    public void checkGIPPVersionViewDirection(String gippFilepath, String gippVersion) throws Sen2VMException {
        boolean compatibleVersion = false;
        if (gippVersion != null) {
            List<A_GIPP_LIST.GIPP_FILENAME> gippList = auxiliaryDataInfo.getGIPP_List().getGIPP_FILENAME();
            for (GIPP_FILENAME gipp_filename : gippList) {
                if (gippFilepath.contains(gipp_filename.getValue()) && gippVersion.equals(gipp_filename.getVersion())) {
                    compatibleVersion = true;
                }
            }
        } else {
            throw new Sen2VMException("GIPP version could not be find for " + gippFilepath);
        }

        if (!compatibleVersion) {
            throw new Sen2VMException("GIPP " + gippFilepath + " with version " + gippVersion + " is not supported by current datastrip " + dsFile);
        }
    }

    /**
     * Get line datation for given band and given detector
     * @param bandInfo the wanted band
     * @param detectorIndex the detector index
     * @return
     */
    public LineDatation getLineDatation(BandInfo bandInfo, DetectorInfo detectorInfo) {
        AbsoluteDate referenceDate = null;
        double referenceLineDouble = 1d;
        AbsoluteDate defaultReferenceDate = null;
        double defaultReferenceLineDouble = 1d;
        boolean found = false;
        // We get the value of a half line period for the given band resolution
        double linePeriod = getNewPositionFromResolution(getLinePeriod(), Sen2VMConstants.RESOLUTION_10M_DOUBLE, bandInfo.getPixelHeight());
        double halfLinePeriod = linePeriod / 2;
        if (sensorConfiguration != null) {
            Time_Stamp timeStampElement = sensorConfiguration.getTime_Stamp();
            List<A_TIME_STAMP.Band_Time_Stamp> bandList = timeStampElement.getBand_Time_Stamp();
            if (bandList != null) {
                for (A_TIME_STAMP.Band_Time_Stamp bandTimeStamp : bandList) {
                    int bandId = Integer.parseInt(bandTimeStamp.getBandId());
                    if (bandId == bandInfo.getIndex()) {
                        List<Detector> detectorList = bandTimeStamp.getDetector();
                        if (detectorList != null) {
                            for (Detector detector : detectorList) {
                                String detectorName = detector.getDetectorId();
                                if (detectorName.equals(detectorInfo.getName())) {
                                    found = true;
                                    int refLineInt = detector.getREFERENCE_LINE();
                                    if (refLineInt != 0 && refLineInt != 1) {
                                        referenceLineDouble = getNewPositionFromSize((double) refLineInt, Sen2VMConstants.RESOLUTION_10M_DOUBLE, bandInfo.getPixelHeight());
                                    }
                                    XMLGregorianCalendar referenceDateXML = detector.getGPS_TIME();
                                    referenceDate = new AbsoluteDate(referenceDateXML.toString(), gps);
                                    // We shift the date of a half line period to be in the middle of the line
                                    referenceDate = referenceDate.shiftedBy(halfLinePeriod / 1000d);
                                } else {
                                    // If bypass is activated, we will use the last value found for missing detector
                                    int refLineInt = detector.getREFERENCE_LINE();
                                    if (refLineInt != 0 && refLineInt != 1) {
                                        defaultReferenceLineDouble = getNewPositionFromSize((double) refLineInt, Sen2VMConstants.RESOLUTION_10M_DOUBLE,
                                                bandInfo.getPixelHeight());
                                    }
                                    XMLGregorianCalendar referenceDateXML = detector.getGPS_TIME();
                                    defaultReferenceDate = new AbsoluteDate(referenceDateXML.toString(), gps);
                                    // We shift the date of a half line period to be in the middle of the line
                                    defaultReferenceDate = defaultReferenceDate.shiftedBy(halfLinePeriod / 1000d);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1000d / ( linePeriod * bandPixelHeight / 10)
        LineDatation lineDatation = null;
        if (found && referenceDate != null) {
            lineDatation = new LinearLineDatation(referenceDate, referenceLineDouble, 1000d / linePeriod);
        } else {
            lineDatation = new LinearLineDatation(defaultReferenceDate, defaultReferenceLineDouble,
                    1000d / linePeriod);
        }

        return lineDatation;
    }

    /**
     * Return the position in wanted pixel size
     * @param position current position
     * @param pixelSize size of pixelNum
     * @param wantedSize wanted size
     * @return
     */
    public static double getNewPositionFromSize(double position, double pixelSize, double wantedSize) {
        double returned = position * pixelSize / wantedSize;
        return returned;
    }

    /**
     * Return the position in wanted resolution
     * @param position current position
     * @param currentResolution resolution of pixelNum
     * @param wantedResolution wanted resolution
     * @return
     */
    protected double getNewPositionFromResolution(double position, double currentResolution, double wantedResolution) {
        double returned = position / currentResolution * wantedResolution;
        return returned;
    }

    /**
     * Get line period from Data strip (in ms)
     * @return line period from Data strip (in ms)
     */
    public double getLinePeriod() {
        double linePeriod = 0d;
        if (sensorConfiguration != null) {
            Time_Stamp timeStampElement = sensorConfiguration.getTime_Stamp();
            linePeriod = timeStampElement.getLINE_PERIOD().getValue();
        }
        return linePeriod;
    }

    /**
     * @return the RefiningInfo
     */
    public RefiningInfo getRefiningInfo() {
        return refiningInfo;
    }
}
