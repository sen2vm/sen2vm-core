//package esa.sen2vm.input;
//
//import java.util.List;
//
//
///**
// * Parameters given as arguments of the command line
// * @author Guylaine Prat
// */
//public class CommandLineParameter extends InputParameter {
//   
//    /**
//     * Tell if we must compute a direct (true) or inverse (false) location
//     */
//    private boolean directLoc = true;
//    
//    /**
//    * Name of the localization to perform (direct or inverse)
//    */
//    private String locOperationName;
//    
//    /**
//     * Tell if line and pixel number must be recompute with GDAL convention
//     */
//    private boolean conventionGDAL = true;
//
//    /**
//     * Longitude for inverseLoc (deg)
//     */
//    private double longitude = Double.NaN;
//    /**
//     * Latitude for inverseLoc (deg)
//     */
//    private double latitude = Double.NaN;
//
//    /**
//     * Pixel for directLoc
//     */
//    private double pixel = Double.NaN;
//    /**
//     * Line for directLoc
//     */
//    private double line = Double.NaN;
// 
//    /**
//     * SRS used by xVal and yVal
//     */
//    private String srs = null;
//    
//    /**
//    * Read s2geolib input interface file (XML)
//    */
//   private String readInputXmlFilePath = null;
//
//
//   /**
//   * Constructor specific for colocation only
//   * @param name
//   * @param readInputXmlFilePath
//   */
//  public CommandLineParameter(String name, String readInputXmlFilePath) {
//       super(name);
//       this.readInputXmlFilePath = readInputXmlFilePath;
//   }
//
////    /**
////    * Constructor
////    * @param name
////    * @param readInputXmlFilePath
////    * @param bandList
////    * @param detectorList
////    */
////   public CommandLineParameter(String name, String readInputXmlFilePath, List<BandInfo> bandList, List<DetectorInfo> detectorList) {
////        super(name, bandList, detectorList);
////        this.readInputXmlFilePath = readInputXmlFilePath;
////    }
//
//    
//
//    public String getLocOperationName() {
//      return locOperationName;
//   }
//
//   public double getLongitude() {
//      return longitude;
//   }
//
//   public double getLatitude() {
//      return latitude;
//   }
//   
//   public double getPixel() {
//      return pixel;
//   }
//
//   public double getLine() {
//      return line;
//   }
//
//   public String getInputXmlFilePath() {
//      return readInputXmlFilePath;
//   }
//
//   public void setLongitude(double longitude) {
//      this.longitude = longitude;
//   }
//
//   public void setLatitude(double latitude) {
//      this.latitude = latitude;
//   }
//
//   public void setPixel(double pixel) {
//      this.pixel = pixel;
//   }
//
//   public void setLine(double line) {
//      this.line = line;
//   }
//
//   
//   public void setLocOperationName(String locOperationName) {
//      this.locOperationName = locOperationName;
//   }
//
//   /**
//     * @return the directLoc flag
//     */
//    public boolean isDirectLoc() {
//        return directLoc;
//    }
//
//    /**
//     * @param directLoc the directLoc flag to set
//     */
//    public void setDirectLoc(boolean directLoc) {
//        this.directLoc = directLoc;
//    }
//    
//    /**
//     * @return the srs definition
//     */
//    public String getSrs() {
//        return srs;
//    }
//
//    /**
//     * @param srs the srs to set
//     */
//    public void setSrs(String srs) {
//        this.srs = srs;
//    }
//
//    /**
//     * @return the conventionGDAL
//     */
//    public boolean isGDALconvention() {
//        return conventionGDAL;
//    }
//
//    /**
//     * @param conventionGDAL the conventionGDAL to set
//     */
//    public void setGDALconvention(boolean conventionGDAL) {
//        this.conventionGDAL = conventionGDAL;
//    }
//
//}
