//package esa.sen2vm.input;
//
//import java.util.List;
//
//import esa.sen2vm.enums.BandInfo;
//import esa.sen2vm.enums.DetectorInfo;
//
///**
// * Input parameters given to s2geolib in the XML file 
// * @author Guylaine Prat
// */
//public class InputParameter {
//
//    /**
//     * Parameter name
//     */
//    protected String name = null;
//    
//    /**
//     * Parameter value : true to take into account the parameter
//     */
//    protected boolean value = Boolean.FALSE;
//    
//    /**
//     * Parameter path (optional)
//     */
//    protected String path = null;
//    
//    /**
//     * Parameter type (optional)
//     */
//    protected String type = null;
//
//    /**
//     * Constant elevation over the geoid or the ellipsoid; unit : m  (optional)
//     */
//    protected double constantElevation = Double.NaN;
//
//    /**
//     * List of band associated to this parameter (optional)
//     */
//    protected List<BandInfo> bandList = null;
//
//    /**
//     * List of band associated to this parameter (optional)
//     */
//    protected List<DetectorInfo> detectorList = null;
//
//    /**
//     * Constructor
//     * @param name param name
//     */
//    public InputParameter(String name) {
//        this(name, "false", null, null, null, null, null);
//    }
//
//    /**
//     * Constructor
//     * @param name param name
//     * @param value param value
//     */
//    public InputParameter(String name, String value) {
//        this(name, value, null, null, null, null, null);
//    }
//
//    /**
//     * Constructor
//     * @param name
//     * @param value
//     * @param constantElevation
//     */
//   public InputParameter(String name, String value, Double constantElevation) {
//        this(name, value, null, null, null, null, constantElevation);
//    }
//
//    /**
//     * Constructor
//     * @param name
//     * @param bandList
//     * @param detectorList
//     */
//    public InputParameter(String name, List<BandInfo> bandList, List<DetectorInfo> detectorList) {
//        this(name, "true", null, null, bandList, detectorList, null);
//    }
//
//    /**
//     * @param name
//     * @param value
//     * @param path
//     * @param bandList
//     */
//    public InputParameter(String name, String value, String path, String type, List<BandInfo> bandList, List<DetectorInfo> detectorList, Double constantElevation) {
//        this.name = name;
//        this.value = Boolean.parseBoolean(value);
//        this.path = path;
//        this.type = type;
//        if (constantElevation != null) this.constantElevation = constantElevation;
//        this.bandList = bandList;
//        this.detectorList = detectorList;
//    }
//
//    /**
//     * @param name
//     * @param value
//     * @param path
//     * @param bandList
//     */
//    public InputParameter(String name, InputParameter param) {
//        this.name = name;
//        this.value = param.getValue();
//        this.path = param.getPath();
//        this.type = param.getType();
//        this.constantElevation = param.getConstantElevation();
//        this.bandList = param.getBandList();
//        this.detectorList = param.getDetectorList();
//    }
//
//    /**
//     * @return the name
//     */
//    public String getName() {
//        return name;
//    }
//
//    /**
//     * @return the value
//     */
//    public boolean getValue() {
//        return value;
//    }
//
//    /**
//     * @return the path
//     */
//    public String getPath() {
//        return path;
//    }
//
//    /**
//     * @param name the name to set
//     */
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    /**
//     * @param value the value to set
//     */
//    public void setValue(boolean value) {
//        this.value = value;
//    }
//
//    /**
//     * @param path the path to set
//     */
//    public void setPath(String path) {
//        this.path = path;
//    }
//
//    /**
//     * @return the bandList
//     */
//    public List<BandInfo> getBandList() {
//        return bandList;
//    }
//
//    /**
//     * @param bandList the bandList to set
//     */
//    public void setBandList(List<BandInfo> bandList) {
//        this.bandList = bandList;
//    }
//
//    /**
//     * @return the detectorList
//     */
//    public List<DetectorInfo> getDetectorList() {
//        return detectorList;
//    }
//
//    /**
//     * @param detectorList the detectorList to set
//     */
//    public void setDetectorList(List<DetectorInfo> detectorList) {
//        this.detectorList = detectorList;
//    }
//
//    /**
//     * @return the type
//     */
//    public String getType() {
//        return type;
//    }
//
//    /**
//     * @param type the type to set
//     */
//    public void setType(String type) {
//        this.type = type;
//    }
//
//   public Double getConstantElevation() {
//      return constantElevation;
//   }
//
//   public void setConstanElevation(Double constantElevation) {
//      this.constantElevation = constantElevation;
//   }
//}
