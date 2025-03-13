package esa.sen2vm.input.gipp;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.sxgeo.input.datamodels.sensor.SpaceCraftModelTransformation;
import org.sxgeo.input.datamodels.sensor.SpacecraftModelTransformationEnum;
import org.sxgeo.utils.ARotationAroundAnAxis;
import org.sxgeo.utils.CombinationOrder;

import generated.GS2_SPACECRAFT_MODEL_PARAMETERS;
import generated.GS2_SPACECRAFT_MODEL_PARAMETERS.DATA.FOCAL_PLANE_TO_DETECTOR;
import generated.GS2_SPACECRAFT_MODEL_PARAMETERS.DATA.FOCAL_PLANE_TO_DETECTOR.FOCAL_PLANE_TO_DETECTOR_SWIR;
import generated.GS2_SPACECRAFT_MODEL_PARAMETERS.DATA.FOCAL_PLANE_TO_DETECTOR.FOCAL_PLANE_TO_DETECTOR_VNIR;
import generated.GS2_SPACECRAFT_MODEL_PARAMETERS.DATA.MSI_TO_FOCAL_PLANE;

import _int.esa.gs2.sy._1_0.misc.A_ROTATION_AROUND_THREE_AXIS_AND_SCALE;
import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.exception.Sen2VMException;

/**
 * Class used to manage SPAMOD GIPP data
 */
public class SpaModManager
{
    private static final Logger LOGGER = Logger.getLogger(GIPPManager.class.getName());
    
    protected SpaceCraftModelTransformation pilotingToMsiTransformation = null;
    protected HashMap<String, SpaceCraftModelTransformation> msiToFocalPlaneTransformation = new HashMap<String, SpaceCraftModelTransformation>();
    protected HashMap<String, HashMap<DetectorInfo, SpaceCraftModelTransformation>> focalPlaneToDetectorTransformation = new HashMap<String, HashMap<DetectorInfo, SpaceCraftModelTransformation>> ();

    protected static int[] pilotingToMsiAngleSigns = {1, 1, 1};
    protected static int[] msiToFocalPlaneAngleSigns = {1, 1, 1};
    protected static int[] focalPlaneToDetectorAngleSigns = {1, 1, 1};

    /*
     * SpaModManager constructor
     * @throws Sen2VMException
     */
    public SpaModManager(GS2_SPACECRAFT_MODEL_PARAMETERS spaModData) throws Sen2VMException
    {
        if (spaModData != null)
        {
            // Get PILOTING_TO_MSI_FRAME rotations
            A_ROTATION_AROUND_THREE_AXIS_AND_SCALE pilotingToMSI = spaModData.getDATA().getPILOTING_TO_MSI_FRAME();
            if (pilotingToMSI != null)
            {
                // Get PILOTING_TO_MSI_FRAME transformation
                pilotingToMsiTransformation =
                        new SpaceCraftModelTransformation(
                            SpacecraftModelTransformationEnum.PILOTING_TO_MSI,
                            CombinationOrder.valueOf(pilotingToMSI.getCOMBINATION_ORDER()),
                            pilotingToMSI.getSCALE_FACTOR(),
                            new ARotationAroundAnAxis(
                                pilotingToMSI.getR1().getValue(),
                                pilotingToMSI.getR1().getAxis(),
                                pilotingToMSI.getR1().getUnit().value()
                                ),
                            new ARotationAroundAnAxis(
                                pilotingToMSI.getR2().getValue(),
                                pilotingToMSI.getR2().getAxis(),
                                pilotingToMSI.getR2().getUnit().value()
                                ),
                            new ARotationAroundAnAxis(
                                pilotingToMSI.getR3().getValue(),
                                pilotingToMSI.getR3().getAxis(),
                                pilotingToMSI.getR3().getUnit().value()
                                ),
                            pilotingToMsiAngleSigns);
            }

            // Get MSI_TO_FOCAL_PLANE rotations
            MSI_TO_FOCAL_PLANE msiToFocalPlaneElement = spaModData.getDATA().getMSI_TO_FOCAL_PLANE();
            A_ROTATION_AROUND_THREE_AXIS_AND_SCALE rotations = null;
            if (msiToFocalPlaneElement != null)
            {
                // VNIR case
                rotations = msiToFocalPlaneElement.getMSI_TO_VNIR();
                SpaceCraftModelTransformation smt = new SpaceCraftModelTransformation(
                    SpacecraftModelTransformationEnum.MSI_TO_FOCALPLANE,
                    CombinationOrder.valueOf(rotations.getCOMBINATION_ORDER()),
                    rotations.getSCALE_FACTOR(),
                    new ARotationAroundAnAxis(
                        rotations.getR1().getValue(),
                        rotations.getR1().getAxis(),
                        rotations.getR1().getUnit().value()
                        ),
                    new ARotationAroundAnAxis(
                        rotations.getR2().getValue(),
                        rotations.getR2().getAxis(),
                        rotations.getR2().getUnit().value()
                        ),
                    new ARotationAroundAnAxis(
                        rotations.getR3().getValue(),
                        rotations.getR3().getAxis(),
                        rotations.getR3().getUnit().value()
                        ),
                    msiToFocalPlaneAngleSigns);
                msiToFocalPlaneTransformation.put("VNIR", smt);

                // SWIR case
                rotations = msiToFocalPlaneElement.getMSI_TO_SWIR();
                smt = new SpaceCraftModelTransformation(
                    SpacecraftModelTransformationEnum.MSI_TO_FOCALPLANE,
                    CombinationOrder.valueOf(rotations.getCOMBINATION_ORDER()),
                    rotations.getSCALE_FACTOR(),
                    new ARotationAroundAnAxis(
                        rotations.getR1().getValue(),
                        rotations.getR1().getAxis(),
                        rotations.getR1().getUnit().value()
                        ),
                    new ARotationAroundAnAxis(
                        rotations.getR2().getValue(),
                        rotations.getR2().getAxis(),
                        rotations.getR2().getUnit().value()
                        ),
                    new ARotationAroundAnAxis(
                        rotations.getR3().getValue(),
                        rotations.getR3().getAxis(),
                        rotations.getR3().getUnit().value()
                        ),
                    msiToFocalPlaneAngleSigns);
                msiToFocalPlaneTransformation.put("SWIR", smt);
            }

            // get FOCAL_PLANE_TO_DETECTOR rotations
            FOCAL_PLANE_TO_DETECTOR fpd = spaModData.getDATA().getFOCAL_PLANE_TO_DETECTOR();
            if (fpd != null)
            {
                // get VNIR transformations
                HashMap<DetectorInfo, SpaceCraftModelTransformation> vnirTransfoMap = new HashMap<DetectorInfo, SpaceCraftModelTransformation>();
                List<FOCAL_PLANE_TO_DETECTOR_VNIR> vnirList = fpd.getFOCAL_PLANE_TO_DETECTOR_VNIR();
                for (FOCAL_PLANE_TO_DETECTOR_VNIR currentRotations : vnirList)
                {
                    String detectorId = currentRotations.getDetector_Id();
                    DetectorInfo detector = DetectorInfo.getDetectorInfoFromName(detectorId);
                    SpaceCraftModelTransformation smt =
                            new SpaceCraftModelTransformation(
                                    SpacecraftModelTransformationEnum.FOCALPLANE_TO_SENSOR,
                                    new ARotationAroundAnAxis(
                                        currentRotations.getR1().getValue(),
                                        currentRotations.getR1().getAxis(),
                                        currentRotations.getR1().getUnit().value()
                                        ),
                                    new ARotationAroundAnAxis(
                                        currentRotations.getR2().getValue(),
                                        currentRotations.getR2().getAxis(),
                                        currentRotations.getR2().getUnit().value()
                                        ),
                                    new ARotationAroundAnAxis(
                                        currentRotations.getR3().getValue(),
                                        currentRotations.getR3().getAxis(),
                                        currentRotations.getR3().getUnit().value()
                                        ),
                                    focalPlaneToDetectorAngleSigns);
                    vnirTransfoMap.put(detector, smt);
                    focalPlaneToDetectorTransformation.put("VNIR", vnirTransfoMap);
                }

                // get SWIR transformations
                HashMap<DetectorInfo, SpaceCraftModelTransformation> swirTransfoMap = new HashMap<DetectorInfo, SpaceCraftModelTransformation>();
                List<FOCAL_PLANE_TO_DETECTOR_SWIR> swirList = fpd.getFOCAL_PLANE_TO_DETECTOR_SWIR();
                for (FOCAL_PLANE_TO_DETECTOR_SWIR currentRotations : swirList)
                {
                    String detectorId = currentRotations.getDetector_Id();
                    DetectorInfo detector = DetectorInfo.getDetectorInfoFromName(detectorId);
                    SpaceCraftModelTransformation smt =
                            new SpaceCraftModelTransformation(
                                    SpacecraftModelTransformationEnum.FOCALPLANE_TO_SENSOR,
                                    new ARotationAroundAnAxis(
                                        currentRotations.getR1().getValue(),
                                        currentRotations.getR1().getAxis(),
                                        currentRotations.getR1().getUnit().value()
                                        ),
                                    new ARotationAroundAnAxis(
                                        currentRotations.getR2().getValue(),
                                        currentRotations.getR2().getAxis(),
                                        currentRotations.getR2().getUnit().value()
                                        ),
                                    new ARotationAroundAnAxis(
                                        currentRotations.getR3().getValue(),
                                        currentRotations.getR3().getAxis(),
                                        currentRotations.getR3().getUnit().value()
                                        ),
                                    focalPlaneToDetectorAngleSigns);
                    swirTransfoMap.put(detector, smt);
                    focalPlaneToDetectorTransformation.put("SWIR", swirTransfoMap);
                }
            }
        }
    }

    public SpaceCraftModelTransformation getPilotingToMsiTransformation()
    {
        return pilotingToMsiTransformation;
    }

    public SpaceCraftModelTransformation getMsiToFocalPlaneTransformation(BandInfo bandInfo)
    {
        // Get MSI to focal plane transfo according to focal plane type
        return msiToFocalPlaneTransformation.get(bandInfo.getSpaMod());
    }

    public SpaceCraftModelTransformation getFocalPlaneToDetectorTransformation(BandInfo bandInfo, DetectorInfo detectorInfo)
    {
        // Get focal plane to detector transfo according to focal plane type and detector number
        HashMap<DetectorInfo, SpaceCraftModelTransformation> detectorMap = focalPlaneToDetectorTransformation.get(bandInfo.getSpaMod());
        SpaceCraftModelTransformation transfo = null;
        if (detectorMap != null)
        {
            transfo = detectorMap.get(detectorInfo);
        }
        return transfo;
    }
}