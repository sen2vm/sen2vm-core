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

    protected int step ;

    protected int pixelOrigin ;
    protected int lineOrigin ;
    protected int sizeLines ;
    protected int sizePixels ;

    protected ArrayList<Integer> gridX;
    protected ArrayList<Integer> gridY ;

    public InverseLocGrid(int ul_x, int ul_y, int lr_x, int lr_y,
                         String epsg, String OutputFolder,
                         int step,
                         int pixelOrigin, int lineOrigin,
                         int sizePixels, int sizeLines) {

        this.epsg = Integer.valueOf(epsg.substring(5));
        this.ul = new Coordinates(ul_y, ul_x, this.epsg);
        // this.ul.transform();
        this.lr = new Coordinates(lr_y, lr_x, this.epsg);
        // this.lr.transform();

        this.step = step;
        this.pixelOrigin = pixelOrigin;
        this.lineOrigin = lineOrigin;
        this.sizePixels = sizePixels;
        this.sizeLines = sizeLines;

        System.out.println("# Grid information");
        System.out.print("Step: (" + String.valueOf(this.step) + ", " + String.valueOf(this.step) + ") ; ");
        System.out.print("UL: (" + String.valueOf(this.ul.getX()) + ", " + String.valueOf(this.ul.getY()) + ") ; ");
        System.out.print("LR: (" + String.valueOf(this.lr.getX()) + ", " + String.valueOf(this.lr.getY()) + ") ; ");
        System.out.print("Start: (" + String.valueOf(this.pixelOrigin) + ", " + String.valueOf(this.lineOrigin) + ") ; ");
        System.out.println("Size: (" + String.valueOf(this.sizePixels) + ", " + String.valueOf(this.sizeLines) + ")");
        System.out.println();

        this.gridX = grid_1D(this.lr.getX(), this.lr.getX() - this.ul.getX(), this.step) ;
        this.gridY = grid_1D(this.ul.getY(), this.ul.getY() - this.lr.getY(), this.step) ;
        LOGGER.info("Grid Pixel: " + this.gridX);
        LOGGER.info("Grid Line: " + this.gridY);
    }

    private ArrayList<Integer> grid_1D(int start, int size, int step) {
        System.out.println("grid_1D");
        System.out.println(start);
        System.out.println(size);
        ArrayList<Integer> grid = new ArrayList<Integer>();
        int nb = (int) size / this.step ;
        System.out.println(nb);
        LOGGER.info("start: " + start);
        LOGGER.info("size: " + size);

        for (int i = 0 ; i < nb ; i++) {
            grid.add(start + step * i);
        }
        return grid;
    }


    public double[][] get2DgridLatLon() {
        // @param pixels double[][] 2d pixel coordinate array as [[row0,col0],[row1,col1],...,[rown,coln]]

        int nbCols = this.gridX.size();
        int nbLines = this.gridY.size();

        double[][] grid = new double[nbCols * nbLines][2] ;

        for (int l = 0 ; l < nbLines ; l ++){
            for (int c = 0 ; c < nbCols ; c ++){
                // Coordinates coord = new Coordinates(this.gridY.get(l), this.gridX.get(c), this.epsg);
                // coord.transform();
                grid[l*nbCols + c][0] = coord.getY();
                grid[l*nbCols + c][1] = coord.getX();
            }
        }
        System.out.println("Fin grid");


        return grid;
    }

}