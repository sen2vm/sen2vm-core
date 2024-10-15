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

    protected Float step ;

    protected int pixelOrigin ;
    protected int lineOrigin ;
    protected int sizeLines ;
    protected int sizePixels ;

    protected ArrayList<Float> gridX;
    protected ArrayList<Float> gridY ;

    public InverseLocGrid(Float ul_x, Float ul_y, Float lr_x, Float lr_y,
                         String epsg, String OutputFolder,
                         Float step,
                         int pixelOrigin, int lineOrigin,
                         int sizePixels, int sizeLines) {

        this.epsg = Integer.valueOf(epsg.substring(5));
        this.ul = new Coordinates(ul_y, ul_x, this.epsg);
        this.ul.transform();
        this.lr = new Coordinates(lr_y, lr_x, this.epsg);
        this.lr.transform();

        this.step = step;
        this.pixelOrigin = pixelOrigin;
        this.lineOrigin = lineOrigin;
        this.sizePixels = sizePixels;
        this.sizeLines = sizeLines;

        System.out.println("# Grid information");
        System.out.print("Step: (" + String.valueOf(this.step) + ", " + String.valueOf(this.step) + ") ; ");
        System.out.print("UL: (" + String.valueOf(this.ul.getX()) + ", " + String.valueOf(this.ul.getY()) + ") ");
        System.out.print("(" + String.valueOf(this.ul.getLongitude()) + ", " + String.valueOf(this.ul.getLatitude()) + ") ; ");
        System.out.print("LR: (" + String.valueOf(this.lr.getX()) + ", " + String.valueOf(this.lr.getY()) + ") ");
        System.out.print("(" + String.valueOf(this.ul.getLongitude()) + ", " + String.valueOf(this.ul.getLatitude()) + ") ; ");
        System.out.print("Start: (" + String.valueOf(this.pixelOrigin) + ", " + String.valueOf(this.lineOrigin) + ") ; ");
        System.out.println("Size: (" + String.valueOf(this.sizePixels) + ", " + String.valueOf(this.sizeLines) + ")");
        System.out.println();

        this.gridX = grid_1D(this.lr.getX(), this.lr.getX() - this.ul.getX()) ;
        this.gridY = grid_1D(this.ul.getY(), this.ul.getY() - this.lr.getY()) ;
        //LOGGER.info("Grid Pixel: " + this.gridX);
        //LOGGER.info("Grid Line: " + this.gridY);
    }

    private ArrayList<Float> grid_1D(Float start, Float size) {


        ArrayList<Float> grid = new ArrayList<Float>();
        int nb = (int) Math.ceil(size / this.step) + 1 ; // TO ASK
        for (int i = 0 ; i < nb ; i++) {
            grid.add(start + step * i);
        }
        System.out.print("End BB");
        System.out.print(start + size);
        System.out.print("; End grid ");
        System.out.print(grid.get(nb-1));
        System.out.print("; Diff ");
        System.out.println(grid.get(nb-1) - start - size);

        return grid;
    }


    public double[][] get2DgridLatLon() {
        // @param pixels double[][] 2d pixel coordinate array as [[row0,col0],[row1,col1],...,[rown,coln]]

        int nbCols = this.gridX.size();
        int nbLines = this.gridY.size();

        double[][] grid = new double[nbCols * nbLines][3] ;

        for (int l = 0 ; l < nbLines ; l ++){
            for (int c = 0 ; c < nbCols ; c ++){
                Coordinates coord = new Coordinates(this.gridY.get(l), this.gridX.get(c), this.epsg);
                coord.transform();
                System.out.print(this.gridY.get(l));
                System.out.print(" ");
                System.out.println(this.gridX.get(l));

                grid[l*nbCols + c][0] = coord.getLongitude();
                grid[l*nbCols + c][1] = coord.getLatitude();
                grid[l*nbCols + c][2] = 0.0;
            }
        }
        System.out.println(nbCols);
        System.out.println(nbLines);
        System.out.println("Fin grid");

        return grid;
    }

}