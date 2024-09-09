package esa.sen2vm.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DirectLocGrid {

    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(DirectLocGrid.class.getName());

    protected int pixelOffset ;
    protected int lineOffset ;
    protected int step ;

    protected int pixelOrigin ;
    protected int lineOrigin ;
    protected int sizeLines ;
    protected int sizePixels ;

    protected ArrayList<Double> gridPixels ;
    protected ArrayList<Double> gridLines ;

    public DirectLocGrid(int lineOffset, int pixelOffset,
                         int step,
                         int lineOrigin, int pixelOrigin,
                         int sizeLines, int sizePixels) {
        this.pixelOffset = pixelOffset;
        this.lineOffset = lineOffset;
        this.step = step;
        this.pixelOrigin = pixelOrigin;
        this.lineOrigin = lineOrigin;
        this.sizePixels = sizePixels;
        this.sizeLines = sizeLines;

        /*System.out.println("# Grid information");
        System.out.print("Offset: (" + String.valueOf(this.lineOffset) + ", " + String.valueOf(this.pixelOffset) + ") ; ");
        System.out.print("Step: (" + String.valueOf(this.step) + ", " + String.valueOf(this.step) + ") ; ");
        System.out.print("Start: (" + String.valueOf(this.lineOrigin) + ", " + String.valueOf(this.pixelOrigin) + ") ; ");
        System.out.println("Size: (" + String.valueOf(this.sizeLines) + ", " + String.valueOf(this.sizePixels) + ")");*/

        this.gridPixels = grid_1D(this.pixelOrigin, this.sizePixels, this.pixelOrigin, this.pixelOffset, this.step);
        this.gridLines = grid_1D(this.lineOrigin, this.sizeLines, this.pixelOrigin, this.lineOffset, this.step);

        // ArrayList<Double> grids = mersh2D(this.gridPixels, this.gridPixels);

        LOGGER.info("Grid Pixel: " + this.gridPixels);
        LOGGER.info("Grid Line: " + this.gridLines);

    }

    public double[][] get2Dgrid() {
        // @param pixels double[][] 2d pixel coordinate array as [[row0,col0],[row1,col1],...,[rown,coln]]

        int nbCols = this.gridPixels.size();
        int nbLines = this.gridLines.size();

        double[][] grid = new double[nbCols * nbLines][2] ;

        for (int l = 0 ; l < nbLines ; l ++){
            for (int c = 0 ; c < nbCols ; c ++){
                grid[l*nbCols + c][0] = this.gridLines.get(l);
                grid[l*nbCols + c][1] = this.gridPixels.get(c);
            }
        }

        return grid;
    }



    private ArrayList<Double> grid_1D(int start, int size, double origin, double offset, int step) {
        Boolean insideGranule = true;
        ArrayList<Double> grid = new ArrayList<Double>();
        int i_grid = 0;

        double value = offset + (i_grid - origin + 1) * step - step / 2 + start;
        grid.add(value);

        while (insideGranule) {
            value = value + step;
            grid.add(value);
            i_grid ++;
            if (value > start + size) {
                insideGranule = false ;
            }
        }
        return grid;
    }

    public int getStartRow(int startGranule) {
        double start = ((startGranule - 1 + this.step / 2 ) * 1.0 / this.step);
        return (int) Math.floor(start) ;
    }

    public int getEndRow(int startGranule, int sizeGranule) {
        double end = ((startGranule - 1 + sizeGranule + this.step / 2 ) * 1.0  / this.step);
        return (int) Math.ceil(end);
    }

    public double[][][] extractPointsDirectLoc(double[][] directLocGrid, int startGranule, int sizeGranule) {
        Boolean insideGranule = true;

        int grid_row_start = getStartRow(startGranule);
        int grid_row_end = getEndRow(startGranule, sizeGranule) ;

        int nbLines = grid_row_end - grid_row_start + 1;
        int nbCols = this.gridPixels.size();

        double[][][] subDirectLocGrid = new double[2][nbLines][nbCols];

        for (int l = 0; l < nbLines; l++) {
            for (int c = 0; c < nbCols; c++) {
                subDirectLocGrid[0][l][c] = directLocGrid[(l + grid_row_start)*nbCols + c][0] ;
                subDirectLocGrid[1][l][c] = directLocGrid[(l + grid_row_start)*nbCols + c][1] ;
            }
        }

        System.out.print("Granule: " + String.valueOf(startGranule) + " -> " + String.valueOf(startGranule + sizeGranule) + "     ");
        System.out.print("start: " + String.valueOf(grid_row_start) + " (");
        System.out.print(String.valueOf(this.gridLines.get(grid_row_start)) + ")");
        System.out.print(" -> end: " + String.valueOf(grid_row_end) + " (");
        System.out.println(String.valueOf(this.gridLines.get(grid_row_end)) + ")");
        return subDirectLocGrid;
    }


    public void initLittleDirectGrid(Map<String, Object> infoGranuleImage) {

        int pixelOrigin = (Integer) infoGranuleImage.get("pixelOrigin") ;
        int lineOrigin = (Integer)  infoGranuleImage.get("granulePosition") ;
        int sizeLines = (Integer)  infoGranuleImage.get("granuleDimensions_nrows") ;
        int sizePixels = (Integer)  infoGranuleImage.get("granuleDimensions_ncols") ;

        System.out.print("Origin: (" + String.valueOf(pixelOrigin) + ", " + String.valueOf(lineOrigin) + ") ; ");
        System.out.println("Size: (" + String.valueOf(sizePixels) + ", " + String.valueOf(sizeLines) + ")");

        this.gridLines = grid_1D(lineOrigin, sizeLines, pixelOrigin, this.lineOffset, this.step);
        this.gridPixels = grid_1D(pixelOrigin, sizePixels, pixelOrigin, this.pixelOffset, this.step);

        // ArrayList<Double> grids = mersh2D(this.gridPixels, this.gridPixels);

        LOGGER.info("Grid Line: " + this.gridLines);
        LOGGER.info("Grid Pixel: " + this.gridPixels);
        System.out.println();
    }


}