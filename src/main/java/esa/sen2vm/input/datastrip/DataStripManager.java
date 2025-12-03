package esa.sen2vm.input.datastrip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
import org.orekit.frames.ITRFVersion;
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
import org.sxgeo.exception.SXGeoException;
import org.sxgeo.input.datamodels.DataSensingInfos;
import org.sxgeo.input.datamodels.RefiningInfo;
import org.sxgeo.input.datamodels.sensor.Sensor;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.exception.Sen2VMException;
import esa.sen2vm.utils.IERSutils;
import esa.sen2vm.utils.Sen2VMConstants;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_ATTITUDE_DATA_INV.Corrected_Attitudes.Values;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_AUXILIARY_DATA_INFO_DSL1B;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_EPHEMERIS_DATA_INV.GPS_Points_List.GPS_Point;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_IERS_BULLETIN;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.AN_IMAGE_DATA_INFO_DSL1B;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_ACQUISITION_CONFIGURATION.TDI_Configuration_List.TDI_CONFIGURATION;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_GENERAL_INFO_DS;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_GIPP_LIST;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_GIPP_LIST.GIPP_FILENAME;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_REFINED_CORRECTIONS;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_SENSOR_CONFIGURATION;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_SENSOR_CONFIGURATION.Time_Stamp;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_TIME_STAMP;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_TIME_STAMP.Band_Time_Stamp.Detector;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.sy.misc.AN_UNCERTAINTIES_XYZ_TYPE;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.sy.misc.A_DOUBLE_WITH_ARCSEC_UNIT_ATTR;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.sy.misc.A_POLYNOMIAL_MODEL;
import https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.sy.misc.A_ROTATION_TRANSLATION_HOMOTHETY_UNCERTAINTIES_TYPE_LOWER_CASE;
import https.psd_15_sentinel2_eo_esa_int.psd.s2_pdi_level_1b_datastrip_metadata.Level1B_DataStrip;

/**
 * Manager for Datastrip directory
 */
public class DataStripManager
{
    /**
     * Get sen2VM logger
     */
    private static final Logger LOGGER = Logger.getLogger(DataStripManager.class.getName());

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
     * List of granule positon by Detector in Datatrip (10m)
     */
    protected static Map[] positionGranuleByDetector;

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
     * Constructor from SAD XML file and necessary data files
     * @param dsFilePath path to SAD XML file
     * @param iersFilePath path to the IERS file
     * @param activateAvailableRefining if true use refining parameters present in the datastrip,
     *        else will ignore available refining
     * @throws Sen2VMException
     */
    public DataStripManager(String dsFilePath, String iersFilePath,
                            Boolean activateAvailableRefining) throws Sen2VMException
    {
        this.dsFile = new File(dsFilePath);
        gps = TimeScalesFactory.getGPS();
        loadFile(dsFilePath, iersFilePath, activateAvailableRefining);
    }

    public static void extractDirectoryFromJar(URI jarPath, String sourceDir, String targetDir) throws IOException {
        try (JarFile jarFile = new JarFile(new File(jarPath))) {
            jarFile.stream()
                   .filter(entry -> entry.getName().startsWith(sourceDir) && !entry.isDirectory())
                   .forEach(entry -> {
                       File outFile = new File(targetDir.toString(), entry.getName().substring(sourceDir.length()));
                       outFile.getParentFile().mkdirs();
                       try (InputStream is = jarFile.getInputStream(entry)) {
                           Files.copy(is, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                   });
        }
    }

    /**
     * Load SAD file and necessary data
     * @param dsFilePath path to SAD XML file
     * @param iersFilePath path to the IERS file
     * @param activateAvailableRefining if true use refining parameters present in the datastrip,
     *        else will ignore available refining
     * @throws Sen2VMException
     */
    protected void loadFile(String dsFilePath, String iersFilePath,
                            Boolean activateAvailableRefining) throws Sen2VMException
    {
        try
        {
            // Load SAD xml file
            JAXBContext jaxbContext = JAXBContext.newInstance(Level1B_DataStrip.class.getPackage().getName());
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            File datastripFile = new File(dsFilePath);
            JAXBElement<Level1B_DataStrip> jaxbElement = (JAXBElement<Level1B_DataStrip>) jaxbUnmarshaller.unmarshal(datastripFile);
            l1B_datastrip = jaxbElement.getValue();

            sensorConfiguration = l1B_datastrip.getImage_Data_Info().getSensor_Configuration();

            auxiliaryDataInfo = l1B_datastrip.getAuxiliary_Data_Info();
            String orekit_data_name = Sen2VMConstants.OREKIT_DATA_TEST_DIR;
            Path orekit_data_path = Paths.get(orekit_data_name);
            // get jar url to extract orekit data
            URL jarUrl = DataStripManager.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
            Path basePath = Paths.get(jarUrl.getPath());
            Path parentPath = basePath.getParent();
            Path targetPath = parentPath.resolve(Sen2VMConstants.OREKIT_DATA_DIR);
            if(!Files.isDirectory(basePath)) 
            {
                orekit_data_path=targetPath;
            }//otherwise is test case use src/main/resource
            // check orekit data dir exist, otherwise it will extracted from jar
            if(!Files.isDirectory(orekit_data_path))
            {
                // extract orekit-data from jar and save in  Sen2VMConstants.OREKIT_DATA_DIR_SAVE
                extractDirectoryFromJar(jarUrl.toURI(),Sen2VMConstants.OREKIT_DATA_DIR_IN_JAR,orekit_data_path.toString());
                LOGGER.info("Initializing: copy of the Orekit-data: "+orekit_data_path.toString());
            }
            LOGGER.info("Orekit-data: "+orekit_data_path);
            initOrekitRessources(orekit_data_path.toString(), iersFilePath, l1B_datastrip.getGeneral_Info().getDatastrip_Time_Info());

            // Test if we need to take refining data into account according to the flag
            if (activateAvailableRefining)
            {
                if (l1B_datastrip.getImage_Data_Info().getGeometric_Info().getImage_Refining() != null)
                {
                    String refinedType = l1B_datastrip.getImage_Data_Info().getGeometric_Info().getImage_Refining().getFlag();
                    if (refinedType.equalsIgnoreCase("REFINED"))
                    {
                        // get the list of corrections
                        refinedCorrectionsListL1B = l1B_datastrip.getImage_Data_Info().getGeometric_Info().getRefined_Corrections_List().getRefined_Corrections();

                        // get the datastrip start and stop time
                        A_GENERAL_INFO_DS.Datastrip_Time_Info dataStripTimeInfo = l1B_datastrip.getGeneral_Info().getDatastrip_Time_Info();

                        // update the refiningInfo
                        readRefinedCorrections(dataStripTimeInfo, refinedCorrectionsListL1B);
                    }
                }
            }

            // Get quaternions and positions/velocities list from XML file
            computeSatelliteQList();
            computeSatellitePVList(activateAvailableRefining);
            computePositionGranuleByDetector();

            // Instantiate dataSensingInfos that will be used for SimpleLocEngine
            dataSensingInfos = new DataSensingInfos(satelliteQList, satellitePVList, minLinePerSensor, maxLinePerSensor);

        } catch (JAXBException e) {
            LOGGER.warning("Error reading the file: " + dsFilePath);
            throw new Sen2VMException(e);
        } catch (OrekitException e) {
            throw new Sen2VMException(e);
        } catch (SXGeoException e) {
            throw new Sen2VMException(e);
        } catch(URISyntaxException e) {
            throw new Sen2VMException(e);
        } catch(IOException e) {
            throw new Sen2VMException(e);
        }
    }

    /**
     * Load granules name/position from datastrip XML into positionGranuleByDetector list at detector indice
     */
    private void computePositionGranuleByDetector()
    {
        positionGranuleByDetector = new Map[Sen2VMConstants.NB_DETS];

        List<AN_IMAGE_DATA_INFO_DSL1B.Granules_Information.Detector_List.Detector> det_list = l1B_datastrip.getImage_Data_Info().getGranules_Information().getDetector_List().getDetector();
        for (AN_IMAGE_DATA_INFO_DSL1B.Granules_Information.Detector_List.Detector det : det_list)
        {
            List<AN_IMAGE_DATA_INFO_DSL1B.Granules_Information.Detector_List.Detector.Granule_List.Granule> gr_list = det.getGranule_List().getGranule();
            Map<String, Integer> granulePosition = new HashMap<>();

            for (AN_IMAGE_DATA_INFO_DSL1B.Granules_Information.Detector_List.Detector.Granule_List.Granule gr : gr_list)
            {
                granulePosition.put(gr.getGranuleId(), gr.getPOSITION());
            }
            positionGranuleByDetector[Integer.valueOf(det.getDetectorId()) - 1] = granulePosition;
        }
    }

    /**
     * Return min and max granule for a band/detector combinaison
     * @param bandInfo band info
     * @param detectorInfo detector info
     * return {min granule name, max granule name}
     * @throws Sen2VMException
     */
    public String[] getMinMaxGranule(BandInfo bandInfo, DetectorInfo detectorInfo, List<String> granulesList)  throws Sen2VMException
    {
        Map<String, Integer> granulesDetector = positionGranuleByDetector[detectorInfo.getIndex()];
        Map<String, Integer> filteredGranulesDetector = granulesDetector.entrySet().stream()
                .filter(entry -> granulesList.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if(filteredGranulesDetector.isEmpty())
        {
            throw new Sen2VMException(
                    "No granule was found for Detector "+detectorInfo.getName()+" according to the Datastrip metadata granules list.\nPlease check the granules in the GRANULE folder and in the Datastrip metadata granules list.");
        }
        Map.Entry<String, Integer> min = Collections.min(filteredGranulesDetector.entrySet(),  Map.Entry.comparingByValue());
        Map.Entry<String, Integer> max = Collections.max(filteredGranulesDetector.entrySet(),  Map.Entry.comparingByValue());
        String[] minmax = { min.getKey(), max.getKey() };
        return minmax;
    }

    /**
     * Load orekit data and IERS file
     * @param orekitDataPath path to orekit data directory
     * @param iersFilePath path to IERS file (must be updated regularly)
     * @param dataStripTimeInfo datastrip time
     * @throws Sen2VMException
     */
    public void initOrekitRessources(String orekitDataPath, String iersFilePath,
                A_GENERAL_INFO_DS.Datastrip_Time_Info dataStripTimeInfo) throws Sen2VMException
    {
        try
        {
            // Set up default Orekit data
            File orekitDataDir = new File(orekitDataPath);
            if (orekitDataDir == null || (!orekitDataDir.exists()))
            {
                throw new Sen2VMException("Orekit-data dir not found" + orekitDataPath);
            }
            DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitDataDir));

            // Read IERS information from metadata
            if (iersFilePath.equals(""))
            {
                LOGGER.info("Reading IERS from Metadata");
                AN_IERS_BULLETIN iersBulletin = auxiliaryDataInfo.getIERS_Bulletin();
                AN_IERS_BULLETIN.UT1_UTC ut1tutc = iersBulletin.getUT1_UTC();
                A_DOUBLE_WITH_ARCSEC_UNIT_ATTR poleUAngle = iersBulletin.getPOLE_U_ANGLE();
                A_DOUBLE_WITH_ARCSEC_UNIT_ATTR poleVAngle = iersBulletin.getPOLE_V_ANGLE();

                XMLGregorianCalendar datastripStartDateGregorian = dataStripTimeInfo.getDATASTRIP_SENSING_START();
                AbsoluteDate datastripStartDateUTC = new AbsoluteDate(datastripStartDateGregorian.toString(), TimeScalesFactory.getUTC());
                int year = datastripStartDateUTC.getComponents(TimeScalesFactory.getUTC()).getDate().getYear();

                IERSutils.setLoaders(IERSConventions.IERS_2010,
                                 IERSutils.buildEOPList(
                                 getBestFitITRFVersion(year),
                                 datastripStartDateUTC,
                                 ut1tutc.getValue(),
                                 poleUAngle.getValue(),
                                 poleVAngle.getValue()));
            }
            // Or get IERS bulletin file and instantiate FramesFactory with it
            else
            {
                File iersFile = new File(iersFilePath);
                FramesFactory.addDefaultEOP2000HistoryLoaders(null, null, null, null, iersFile.getName());

                DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(iersFile.getParentFile()));

                // When using a single IERS A bulletin some gaps may arise : to allow the use of such bulletin,
                // we fix the EOP continuity threshold to one year instead of the normal gap ...
                FramesFactory.setEOPContinuityThreshold(Constants.JULIAN_YEAR);
            }
        }
        catch (Exception e)
        {
            throw new Sen2VMException("Something went wrong during initialization of IERS and orekit ressources ", e);
        }
    }

    /**
     * Get last supported ITRF version, the best fitted version for input year
     */
    public ITRFVersion getBestFitITRFVersion(int targetYear)
    {
        ITRFVersion closestYear = ITRFVersion.ITRF_1988;
        int minDifference = Integer.MAX_VALUE;
        for (final ITRFVersion iv : ITRFVersion.values())
        {
            int currentYear = iv.getYear();
            if (currentYear <= targetYear)
            {
                int difference = targetYear - currentYear;
                if (difference < minDifference)
                {
                    closestYear = iv;
                    minDifference = difference;
                }
            }
        }
        return closestYear;
    }

    /**
     * Fill
     * @param dataStripTimeInfo
     * @param refinedCorrectionsListL1
     */
    private void readRefinedCorrections(A_GENERAL_INFO_DS.Datastrip_Time_Info dataStripTimeInfo, List<A_REFINED_CORRECTIONS> refinedCorrectionsListL1) throws OrekitException, Sen2VMException
    {
        // refining corrections are computed related to compute acquisition center time
        AbsoluteDate acquisitionCenterTime = computeAcquisitionCenter(dataStripTimeInfo);

        if (refinedCorrectionsListL1 == null)
        {
            throw new Sen2VMException("refinedCorrectionsListL1 is null");
        }

        A_POLYNOMIAL_MODEL ephemerisXpolynom = null;
        A_POLYNOMIAL_MODEL ephemerisYpolynom = null;
        A_POLYNOMIAL_MODEL ephemerisZpolynom = null;

        // Fix the transformation angle signs
        // TODO see what to do in case there is another convention in the definition of the angles
        int[] refiningMSIstateAnglesSigns = {1, 1, 1};
        int[] refiningFocalPlaneStateAngleSigns = {1, 1, 1};

        A_POLYNOMIAL_MODEL spacecraftToMSIRotationX = null;
        A_POLYNOMIAL_MODEL spacecraftToMSIRotationY = null;
        A_POLYNOMIAL_MODEL spacecraftToMSIRotationZ = null;
        A_POLYNOMIAL_MODEL spacecraftToMSIhomothetyZ = null;

        // Init of the polynomial functions for each correction
        HashMap<Sensor, PolynomialFunction> msiToFocalPlaneRotationX = new HashMap<Sensor, PolynomialFunction>();
        HashMap<Sensor, PolynomialFunction> msiToFocalPlaneRotationY = new HashMap<Sensor, PolynomialFunction>();
        HashMap<Sensor, PolynomialFunction> msiToFocalPlaneRotationZ = new HashMap<Sensor, PolynomialFunction>();
        HashMap<Sensor, PolynomialFunction> msiToFocalPlaneHomothety = new HashMap<Sensor, PolynomialFunction>();

        for (https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_REFINED_CORRECTIONS refinedCorrections: refinedCorrectionsListL1)
        {
            // Spacecraft position
            AN_UNCERTAINTIES_XYZ_TYPE spacecraftPositionUncertainties =  refinedCorrections.getSpacecraft_Position();
            // Spacecraft/Piloting to MSI transformation
            A_ROTATION_TRANSLATION_HOMOTHETY_UNCERTAINTIES_TYPE_LOWER_CASE msiStateUncertainties = refinedCorrections.getMSI_State();
            // MSI to Focal plane transformation
            List<A_REFINED_CORRECTIONS.Focal_Plane_State> focalPlaneStateUncertaintiesList = refinedCorrections.getFocal_Plane_State();

            if (spacecraftPositionUncertainties != null)
            {
                // Spacecraft position (expressed in meters) in the local spacecraft reference frame (EVG Euclidium state)
                // Init of the polynomial functions for each correction
                ephemerisXpolynom = spacecraftPositionUncertainties.getX();
                ephemerisYpolynom = spacecraftPositionUncertainties.getY();
                ephemerisZpolynom = spacecraftPositionUncertainties.getZ();
            }

            // Spacecraft/Piloting to MSI transformation
            if (msiStateUncertainties != null)
            {
                // Init of the polynomial functions for each correction
                AN_UNCERTAINTIES_XYZ_TYPE spaceCraftToMSIRotation = msiStateUncertainties.getRotation();
                // rotation parts
                spacecraftToMSIRotationX = spaceCraftToMSIRotation.getX();
                spacecraftToMSIRotationY = spaceCraftToMSIRotation.getY();
                spacecraftToMSIRotationZ = spaceCraftToMSIRotation.getZ();

                // homothety part on Z axis only
                if (msiStateUncertainties.getHomothety() != null)
                {
                  spacecraftToMSIhomothetyZ = msiStateUncertainties.getHomothety().getZ();
                }
            }

            // MSI to Focal plane transformation
            if (focalPlaneStateUncertaintiesList != null)
            {
                for (https.psd_15_sentinel2_eo_esa_int.dico.pdi_v15.pdgs.dimap.A_REFINED_CORRECTIONS.Focal_Plane_State focalPlaneStateUncertainties : focalPlaneStateUncertaintiesList)
                {
                    if (focalPlaneStateUncertainties != null)
                    {
                        String focalPlaneName = focalPlaneStateUncertainties.getFocalPlaneId().value();
                        Sensor sensor = new Sensor(focalPlaneName, null, null, 0.0, null, null, null);

                        // rotation parts
                        AN_UNCERTAINTIES_XYZ_TYPE msiToFocalPlaneRotationXYZ = focalPlaneStateUncertainties.getRotation();

                        // Check if the node exist (different from null)
                        if (msiToFocalPlaneRotationXYZ != null)
                        {
                            msiToFocalPlaneRotationX.put(sensor, createAnglePolynomialFunction(msiToFocalPlaneRotationXYZ.getX(),refiningFocalPlaneStateAngleSigns[0]));
                            msiToFocalPlaneRotationY.put(sensor, createAnglePolynomialFunction(msiToFocalPlaneRotationXYZ.getY(),refiningFocalPlaneStateAngleSigns[1]));
                            msiToFocalPlaneRotationZ.put(sensor, createAnglePolynomialFunction(msiToFocalPlaneRotationXYZ.getZ(),refiningFocalPlaneStateAngleSigns[2]));
                        }

                        // homothety part
                        AN_UNCERTAINTIES_XYZ_TYPE msiToFocalPlaneHomothetyXYZ = focalPlaneStateUncertainties.getHomothety();

                        if (msiToFocalPlaneHomothetyXYZ != null)
                        {
                            // Only Z axis homothety
                            PolynomialFunction msiToFocalPlaneHomothetyZ = createPolynomialFunction(focalPlaneStateUncertainties.getHomothety().getZ());
                            if (msiToFocalPlaneHomothetyZ != null)
                            {
                                msiToFocalPlaneHomothety.put(sensor, msiToFocalPlaneHomothetyZ);
                            }
                        }
                    }
                }
            }
        }

        refiningInfo = new RefiningInfo(true, acquisitionCenterTime,
            createPolynomialFunction(ephemerisXpolynom),
            createPolynomialFunction(ephemerisYpolynom),
            createPolynomialFunction(ephemerisZpolynom),
            createAnglePolynomialFunction(spacecraftToMSIRotationX,refiningMSIstateAnglesSigns[0]),
            createAnglePolynomialFunction(spacecraftToMSIRotationY,refiningMSIstateAnglesSigns[1]),
            createAnglePolynomialFunction(spacecraftToMSIRotationZ,refiningMSIstateAnglesSigns[2]),
            createPolynomialFunction(spacecraftToMSIhomothetyZ),
            msiToFocalPlaneRotationX,
            msiToFocalPlaneRotationY,
            msiToFocalPlaneRotationZ,
            msiToFocalPlaneHomothety);
    }

    /**
     * Fill satelliteQList using values from SAD XML file
     * @throws Sen2VMException
     */
    protected void computeSatelliteQList() throws Sen2VMException
    {
        satelliteQList = new ArrayList<TimeStampedAngularCoordinates>();

        // This HashSet is used only to check a duplicate date in the quaternions
        HashSet<AbsoluteDate> dateSet = new HashSet<AbsoluteDate>();

        // Loop over Corrected Attitudes value list
        List<Values> correctedAttitudeValueList = l1B_datastrip.getSatellite_Ancillary_Data_Info().getAttitudes().getCorrected_Attitudes().getValues();
        for (Values values : correctedAttitudeValueList)
        {
            XMLGregorianCalendar gpsTime = values.getGPS_TIME();
            if (gpsTime == null)
            {
                throw new Sen2VMException(Sen2VMConstants.ERROR_QUATERNION_NULL_GPS);
            }
            // Extract Quaternion values from XML
            AbsoluteDate attitudeDate = new AbsoluteDate(gpsTime.toString(), gps);
            java.util.List<Double> quaternionValues = values.getQUATERNION_VALUES();
            double q1 = quaternionValues.get(0);
            double q2 = quaternionValues.get(1);
            double q3 = quaternionValues.get(2);
            double q0 = quaternionValues.get(3);

            Rotation rotation = new Rotation(q0, q1, q2, q3, true);
            TimeStampedAngularCoordinates pair = new TimeStampedAngularCoordinates(attitudeDate, rotation, Vector3D.ZERO, Vector3D.ZERO);

            if (dateSet.contains(attitudeDate))
            {
                // duplicate data for the current ephemeris date => we aldeady add this quaternion => we just ignore it
                LOGGER.warning("Duplicate quaternion with date : " + values.getGPS_TIME().toString());
            }
            else
            {
                dateSet.add(attitudeDate);
                satelliteQList.add(pair);
            }
        }
    }

    /**
     * Fill satellitePVList using values from SAD XML file
     * @throws Sen2VMException
     */
    protected void computeSatellitePVList(Boolean activateAvailableRefining) throws Sen2VMException
    {
        try
        {
            // Init of used frames
            Frame eme2000 = FramesFactory.getEME2000();
            Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, Sen2VMConstants.simpleEOP);

            satellitePVList = new ArrayList<TimeStampedPVCoordinates>();
            HashSet<AbsoluteDate> dateSet = new HashSet<AbsoluteDate>();

            // Loop over ephemeris value list
            List<GPS_Point> ephemerisGpsPointList = l1B_datastrip.getSatellite_Ancillary_Data_Info().getEphemeris().getGPS_Points_List().getGPS_Point();
            for (GPS_Point ephemeris : ephemerisGpsPointList)
            {
                XMLGregorianCalendar gpsTime = ephemeris.getGPS_TIME();
                if (gpsTime == null)
                {
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

                if (dateSet.contains(ephemerisDate))
                {
                    // duplicate data for the current ephemeris date => we aldeady add this pv coordinate => we just ignore it
                    LOGGER.warning("Duplicate PV !" + ephemeris.getGPS_TIME().toString());
                }
                else
                {
                    // Compute refining corrections before updating satellite PVlist
                    if (activateAvailableRefining)
                    {
                        // Test if the polynoms and the acquisition center time  are not null before applying them
                        if (refiningInfo.getEphemerisXpolyFunc() != null &&
                            refiningInfo.getEphemerisYpolyFunc() != null &&
                            refiningInfo.getEphemerisZpolyFunc() != null &&
                            refiningInfo.getAcquisitionCenterTime() != null)
                        {

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
        }
        catch (Exception e)
        {
            throw new Sen2VMException(e);
        }
    }

    /** Get the transform from an inertial frame defining position-velocity and the local orbital frame.
     * @param date current date
     * @param pv position-velocity of the spacecraft in inertial frame EME2000
     * @return transform from the frame where position-velocity are defined to local orbital frame
     */
    protected Transform transformFromEME2000toLOF(final AbsoluteDate date, final PVCoordinates pvEME2000)
    {
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
    private PolynomialFunction createPolynomialFunction(A_POLYNOMIAL_MODEL XMLpolynomialModel)
    {
        PolynomialFunction polyFunction = null;
        if (XMLpolynomialModel != null)
        {
           // coef will be non null
            int coefSize = XMLpolynomialModel.getCOEFFICIENTS().size();
            Double coef[] = new Double[coefSize];

            // fill-in the coef array from the List<Double>
            XMLpolynomialModel.getCOEFFICIENTS().toArray(coef);

            // convert the Double[] to double[] for PolynomialFunction creation
            double[] coefPoly = new double[coefSize];
            for (int i = 0; i < coefSize; i++)
            {
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
    private PolynomialFunction createAnglePolynomialFunction(A_POLYNOMIAL_MODEL XMLpolynomialModel, int angleSign)
    {
        PolynomialFunction polyFunction = null;

        if (XMLpolynomialModel != null)
        {
            polyFunction = createPolynomialFunction(XMLpolynomialModel);
            if (angleSign == -1)
            {
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
    private AbsoluteDate computeAcquisitionCenter(A_GENERAL_INFO_DS.Datastrip_Time_Info dataStripTimeInfo) throws OrekitException
    {
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

    /*
     * Get DataSensingInfos
     */
    public DataSensingInfos getDataSensingInfos()
    {
       return dataSensingInfos;
    }

    /**
     * get tdi configuration value for the given band
     * @param bandInfo the band we must find the corresponding tdi configuration for
     * @return tdi configuration for the given band
     */
    public String getTdiConfVal(BandInfo bandInfo)
    {
        String tdiConfVal = null;
        if (sensorConfiguration != null)
        {
            List<TDI_CONFIGURATION> tdiConfList = sensorConfiguration.getAcquisition_Configuration().getTDI_Configuration_List().getTDI_CONFIGURATION();
            for (TDI_CONFIGURATION tdiConf : tdiConfList)
            {
                if (tdiConf.getBandId() == Integer.parseInt(bandInfo.getName()))
                {
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
    public void checkGIPPVersion(String gippFilepath, String gippVersion) throws Sen2VMException
    {
        boolean compatibleVersion = false;
        String expectedVersion = null;
        if (gippVersion != null)
        {
            List<A_GIPP_LIST.GIPP_FILENAME> gippList = auxiliaryDataInfo.getGIPP_List().getGIPP_FILENAME();
            for (GIPP_FILENAME gipp_filename : gippList)
            {
                if (gippFilepath.contains(gipp_filename.getValue()))
                {
                    expectedVersion = gipp_filename.getVersion();
                    if (gippVersion.equals(gipp_filename.getVersion()))
                    {
                        compatibleVersion = true;
                    }
                }
            }
        }
        else
        {
            throw new Sen2VMException("GIPP version could not be find for " + gippFilepath);
        }

        if (!compatibleVersion)
        {
            String errorMessage = gippFilepath + " with version " + gippVersion + " is not supported by current datastrip " + dsFile + ".";
            if (expectedVersion != null)
            {
                throw new Sen2VMException(errorMessage + " Expected version is " + expectedVersion + ".");
            }
            throw new Sen2VMException(errorMessage);
        }
    }

    /**
     * Get line datation for given band and given detector
     * @param bandInfo the wanted band
     * @param detectorIndex the detector index
     * @return
     */
    public LineDatation getLineDatation(BandInfo bandInfo, DetectorInfo detectorInfo)
    {
        AbsoluteDate referenceDate = null;
        double referenceLineDouble = 1d;
        AbsoluteDate defaultReferenceDate = null;
        double defaultReferenceLineDouble = 1d;
        boolean found = false;
        // We get the value of a half line period for the given band resolution
        double linePeriod = getNewPositionFromResolution(getLinePeriod(), Sen2VMConstants.RESOLUTION_10M_DOUBLE, bandInfo.getPixelHeight());
        double halfLinePeriod = linePeriod / 2;
        if (sensorConfiguration != null)
        {
            Time_Stamp timeStampElement = sensorConfiguration.getTime_Stamp();
            List<A_TIME_STAMP.Band_Time_Stamp> bandList = timeStampElement.getBand_Time_Stamp();
            if (bandList != null)
            {
                for (A_TIME_STAMP.Band_Time_Stamp bandTimeStamp : bandList)
                {
                    int bandId = Integer.parseInt(bandTimeStamp.getBandId());
                    if (bandId == bandInfo.getIndex())
                    {
                        List<Detector> detectorList = bandTimeStamp.getDetector();
                        if (detectorList != null)
                        {
                            for (Detector detector : detectorList)
                            {
                                String detectorName = detector.getDetectorId();
                                if (detectorName.equals(detectorInfo.getName()))
                                {
                                    found = true;
                                    int refLineInt = detector.getREFERENCE_LINE();
                                    if (refLineInt != 0 && refLineInt != 1)
                                    {
                                        referenceLineDouble = getNewPositionFromSize((double) refLineInt, Sen2VMConstants.RESOLUTION_10M_DOUBLE, bandInfo.getPixelHeight());
                                    }
                                    XMLGregorianCalendar referenceDateXML = detector.getGPS_TIME();
                                    referenceDate = new AbsoluteDate(referenceDateXML.toString(), gps);
                                    // We shift the date of a half line period to be in the middle of the line
                                    referenceDate = referenceDate.shiftedBy(halfLinePeriod / 1000d);
                                }
                                else
                                {
                                    // If bypass is activated, we will use the last value found for missing detector
                                    int refLineInt = detector.getREFERENCE_LINE();
                                    if (refLineInt != 0 && refLineInt != 1)
                                    {
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
        if (found && referenceDate != null)
        {
            lineDatation = new LinearLineDatation(referenceDate, referenceLineDouble, 1000d / linePeriod);
        }
        else
        {
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
    public double getNewPositionFromSize(double position, double pixelSize, double wantedSize)
    {
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
    protected double getNewPositionFromResolution(double position, double currentResolution, double wantedResolution)
    {
        double returned = position / currentResolution * wantedResolution;
        return returned;
    }

    /**
     * Get line period from Data strip (in ms)
     * @return line period from Data strip (in ms)
     */
    public double getLinePeriod()
    {
        double linePeriod = 0d;
        if (sensorConfiguration != null)
        {
            Time_Stamp timeStampElement = sensorConfiguration.getTime_Stamp();
            linePeriod = timeStampElement.getLINE_PERIOD().getValue();
        }
        return linePeriod;
    }

    /**
     * @return the RefiningInfo
     */
    public RefiningInfo getRefiningInfo()
    {
        return refiningInfo;
    }
}
