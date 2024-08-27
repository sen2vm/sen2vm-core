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
import org.sxgeo.exception.SXGeoException;

import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_ATTITUDE_DATA_INV.Corrected_Attitudes.Values;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_EPHEMERIS_DATA_INV.GPS_Points_List.GPS_Point;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_ACQUISITION_CONFIGURATION.TDI_Configuration_List.TDI_CONFIGURATION;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_SENSOR_CONFIGURATION;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_SENSOR_CONFIGURATION.Time_Stamp;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_TIME_STAMP;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_TIME_STAMP.Band_Time_Stamp.Detector;
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
     * Unique DataStripManager instance
     */
    protected static DataStripManager singleton = null;

    /**
     * Get instance
     * @return instance
     */
    public static synchronized DataStripManager getInstance() {
        return singleton;
    }

    /**
     * Init new DataStripManager instance
     * @param sadXmlFilePath path to SAD XML file
     * @throws Sen2VMException
     */
    public static synchronized DataStripManager initDataStripManager(String dsFilePath, String iersDirectoryPath) throws Sen2VMException {
        singleton = new DataStripManager(dsFilePath, iersDirectoryPath);
        return singleton;
    }

    /**
     * Constructor from SAD XML file
     * @param dsFilePath path to SAD XML file
     * @throws Sen2VMException
     */
    protected DataStripManager(String dsFilePath, String iersDirectoryPath) throws Sen2VMException {
        this.dsFile = new File(dsFilePath);
        gps = TimeScalesFactory.getGPS();
        loadFile(dsFilePath, iersDirectoryPath);
    }

    /**
     * Load file using given context
     * @param dsFilePath path (name of the package) containing the class that will be used to load the XML file
     * @throws Sen2VMException
     */
    protected void loadFile(String dsFilePath, String iersDirectoryPath) throws Sen2VMException {
        try {
            // Load SAD xml file
            JAXBContext jaxbContext = JAXBContext.newInstance(Level1B_DataStrip.class.getPackage().getName());
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            File datastripFile = new File(dsFilePath);
            JAXBElement<Level1B_DataStrip> jaxbElement = (JAXBElement<Level1B_DataStrip>) jaxbUnmarshaller.unmarshal(datastripFile);
            l1B_datastrip = jaxbElement.getValue();

            sensorConfiguration = l1B_datastrip.getImage_Data_Info().getSensor_Configuration();

            initOrekitRessources(iersDirectoryPath);

            // Get quaternions and positions/velocities list from xml file
            computeSatelliteQList();
            computeSatellitePVList();
            computeMinMaxLinePerSensor();

            // Instanciate dataSensingInfos that will be use for SimpleLocEngine
            dataSensingInfos = new DataSensingInfos(satelliteQList, satellitePVList, minLinePerSensor, maxLinePerSensor);
            LOGGER.info("dataSensingInfos="+dataSensingInfos);

        } catch (JAXBException e){
            Sen2VMException exception = new Sen2VMException(e);
            throw exception;
        }  catch (OrekitException oe){
            Sen2VMException exception = new Sen2VMException(oe);
            throw exception;
        } catch (SXGeoException e){
            Sen2VMException exception = new Sen2VMException(e);
            throw exception;
        }
    }

    public static synchronized void initOrekitRessources(String iersDirectoryPath) throws Sen2VMException {
		try {
			if (iersDirectoryPath != null && !iersDirectoryPath.equals("")) {
				File iersDir = new File(iersDirectoryPath);
				if (!iersDir.exists()) {
					throw new Sen2VMException("Can't read IERS directory " + iersDirectoryPath);
				}
				File[] files = iersDir.listFiles();
				if (files != null) {
					for (File file : files) {
						FramesFactory.addDefaultEOP2000HistoryLoaders(null, null, null, null, file.getName());
					}

					if (files.length > 0) {
						DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(iersDir));
					}
				}
			}
			// set up default Orekit data
			File orekitDataDir = new File(System.getProperty("user.dir") + "/" + Sen2VMConstants.OREKIT_DATA_DIR);
			if (orekitDataDir == null || (!orekitDataDir.exists())) {
			    throw new Sen2VMException("Orekit data not found");
			}
			DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitDataDir));

			// When using a single IERS A bulletin some gaps may arise : to allow the use of such bulletin,
			// we fix the EOP continuity threshold to one year instead of the normal gap ...
			FramesFactory.setEOPContinuityThreshold(Constants.JULIAN_YEAR);
		} catch (Exception e) {
			throw new Sen2VMException("Something went wrong during initialization of orekit ressources");
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
                Sen2VMException se = new Sen2VMException(Sen2VMConstants.ERROR_QUATERNION_NULL_GPS);
                throw se;
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
    protected void computeSatellitePVList() throws Sen2VMException {
        try {
            // Init of used frames
            Frame eme2000 = FramesFactory.getEME2000();
            Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            System.out.println("itrf="+itrf);

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
//                    if (refiningInfo.isRefined()){
                }

                dateSet.add(ephemerisDate);
                satellitePVList.add(pair);
            }
        } catch (Exception e) {
            Sen2VMException exception = new Sen2VMException(e);
            throw exception;
        }
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
}
