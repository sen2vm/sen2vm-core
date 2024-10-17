package esa.sen2vm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class InverseLocGrid {

    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(InverseLocGrid.class.getName());

    private Coordinates ul ;
    private Coordinates lr;
    private int epsg;

    protected Float stepX ;
    protected Float stepY ;

    protected int pixelOrigin ;
    protected int lineOrigin ;
    protected int sizeLines ;
    protected int sizePixels ;

    protected ArrayList<Float> gridX;
    protected ArrayList<Float> gridY ;

    /**
     * Constructor
     * @param ul_x in epsg referencial
     * @param ul_y in epsg referencial
     * @param lr_x in epsg referencial
     * @param lr_y in epsg referencial
     * @param epsg reference
     * @param step in epsg referencial
     */
    public InverseLocGrid(Float ul_x, Float ul_y, Float lr_x, Float lr_y,
                         String epsg, Float step) {

        this.epsg = Integer.valueOf(epsg.substring(5));
        this.ul = new Coordinates(ul_y, ul_x, this.epsg);
        this.ul.transform();
        this.lr = new Coordinates(lr_y, lr_x, this.epsg);
        this.lr.transform();
        stepY = step;
        stepX = step;

        if (ul_y > lr_y) {
            stepY = -step;
        }
        if (ul_x > lr_x) {
            stepX = -step;
        }

        LOGGER.info("# Grid information");
        String log = "Step: (" + String.valueOf(this.stepX) + ", " + String.valueOf(this.stepY) + ") ; ";
        log = log + "UL: (" + String.valueOf(this.ul.getX()) + ", " + String.valueOf(this.ul.getY()) + ") ";
        log = log + "(" + String.valueOf(this.ul.getLongitude()) + ", " + String.valueOf(this.ul.getLatitude()) + ") ; ";
        log = log + "LR: (" + String.valueOf(this.lr.getX()) + ", " + String.valueOf(this.lr.getY()) + ") ";
        log = log + "(" + String.valueOf(this.ul.getLongitude()) + ", " + String.valueOf(this.ul.getLatitude()) + ")";
        LOGGER.info(log);

        this.gridY = grid_1D(ul_y, lr_y - ul_y, stepY) ;
        this.gridX = grid_1D(ul_x, lr_x - ul_x, stepX) ;
        // LOGGER.info("Grid Pixel: " + this.gridX);
        // LOGGER.info("Grid Line: " + this.gridY);
    }


    /**
     * Create the list of geo grid values for a specific range
     * @param start of the grid
     * @param size of the grid
     * @return list 1D
     */
    private ArrayList<Float> grid_1D(Float start, Float size, Float signedStep) {
        ArrayList<Float> grid = new ArrayList<Float>();
        int nb = (int) Math.ceil(Math.abs(size / signedStep)) + 1 ;
        for (int i = 0 ; i < nb ; i++) {
            grid.add(start + signedStep * i);
        }
        LOGGER.info("1D : " + String.valueOf(start) + " -> " + String.valueOf(grid.get(nb-1)) +
            " (> " +  String.valueOf(start + size) + ")");
        return grid;
    }

    /**
     * Create 2D grid from gridX and gridY with altitude = 0 # TODO
     * @return grid [[lon0, lat0, alt0], [lon1, lat1, alt1], ..]
     */
    public double[][] get2DgridLatLon() {

        int nbCols = this.gridX.size();
        int nbLines = this.gridY.size();
        double[][] grid = new double[nbCols * nbLines][3] ;

        for (int l = 0 ; l < nbLines ; l ++){
            for (int c = 0 ; c < nbCols ; c ++){
                Coordinates coord = new Coordinates(this.gridY.get(l), this.gridX.get(c), this.epsg);
                coord.transform();
                grid[l*nbCols + c][0] = coord.getLongitude();
                grid[l*nbCols + c][1] = coord.getLatitude();
                grid[l*nbCols + c][2] = 0.0;
            }
        }
        return grid;
    }

     /**
     * Transform [[row0, col0], [row1, col1]..] to 3D grid before tiff saving
     * @return grid [[[row00, row01..], [row10, row11..] ..],[[col00, col01...], [col10, col11...], ...]
     */

     public double[][][] get3Dgrid(double[][] gridList) {

        int nbCols = this.gridX.size();
        int nbLines = this.gridY.size();
        int nbBands = 2;
        double[][][] grid = new double[nbBands][nbLines][nbCols];;

        for (int l = 0 ; l < nbLines ; l ++){
            for (int c = 0 ; c < nbCols ; c ++){
                for (int b = 0; b < nbBands; b++) {
                    grid[b][l][c] = gridList[l*nbCols + c][b];
                }
            }
        }
        return grid;
    }


    public Float getStepX() {
        return this.stepX;
    }


    public Float getStepY() {
        return this.stepY;
    }
}