package esa.sen2vm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



public class DirectLocGrid {

    protected int pixelOffset ;
    protected int lineOffset ;
    protected Float step ;

    protected int pixelOrigin ;
    protected int lineOrigin ;
    protected int sizeLines ;
    protected int sizePixels ;

    protected ArrayList<Double> gridPixels ;
    protected ArrayList<Double> gridLines ;

    public DirectLocGrid(int pixelOffset, int lineOffset,
                         Float step,
                         int pixelOrigin, int lineOrigin,
                         int sizePixels, int sizeLines) {
        this.pixelOffset = pixelOffset;
        this.lineOffset = lineOffset;
        this.step = step;
        this.pixelOrigin = pixelOrigin;
        this.lineOrigin = lineOrigin;
        this.sizePixels = sizePixels;
        this.sizeLines = sizeLines;

        System.out.println("# Grid information");
        System.out.print("Offset: (" + String.valueOf(this.lineOffset) + ", " + String.valueOf(this.pixelOffset) + ") ; ");
        System.out.print("Step: (" + String.valueOf(this.step) + ", " + String.valueOf(this.step) + ") ; ");
        System.out.print("Start: (" + String.valueOf(this.lineOrigin) + ", " + String.valueOf(this.pixelOrigin) + ") ; ");
        System.out.println("Size: (" + String.valueOf(this.sizeLines) + ", " + String.valueOf(this.sizePixels) + ")");
        System.out.println();

        this.gridPixels = grid_1D(this.pixelOrigin, this.sizePixels, this.pixelOrigin, this.pixelOffset, this.step);
        this.gridLines = grid_1D(this.lineOrigin, this.sizeLines, this.pixelOrigin, this.lineOffset, this.step);

        // ArrayList<Double> grids = mersh2D(this.gridPixels, this.gridPixels);

        System.out.println("Grid Pixel: " + this.gridPixels);
        System.out.println("Grid Line: " + this.gridLines);
        System.out.println();

    }

    public double[][] get2Dgrid() {
        // @param pixels double[][] 2d pixel coordinate array as [[row0,col0],[row1,col1],...,[rown,coln]]

        int nbCols = this.gridPixels.size();
        int nbLines = this.gridLines.size();

        double[][] grid = new double[nbCols * nbLines][2] ;

        for (int l = 0 ; l < nbLines ; l ++){
            for (int c = 0 ; c < nbCols ; c ++){
                System.out.println();
                grid[l*nbCols + c][0] = this.gridLines.get(l);
                grid[l*nbCols + c][1] = this.gridPixels.get(c);
            }
        }

        return grid;
    }



    private ArrayList<Double> grid_1D(int start, int size, double origin, double offset, float step) {
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

    public double[][][] extractPointsDirectLoc(double[][] directLocGrid, int startGranule, int sizeGranule) {
        Boolean insideGranule = true;

        int i_grid = 0;

        int grid_row_start = (int) ((startGranule - 1) / this.step);
        int grid_row_end = (int) ((startGranule - 1 + sizeGranule) / this.step + 1);
        System.out.println("GRID ROW: " +String.valueOf(grid_row_start) + " -> " + String.valueOf(grid_row_end));


        int nbLines = grid_row_end - grid_row_start ;
        int nbCols = this.gridPixels.size();

        double[][][] subDirectLocGrid = new double[2][nbLines][nbCols];

        for (int l = grid_row_start; l < nbLines; l++) {
            for (int c = 0; c < nbCols; c++) {
                subDirectLocGrid[0][l][c] = directLocGrid[l*nbCols + c][0] ;
                subDirectLocGrid[1][l][c] = directLocGrid[l*nbCols + c][1] ;
            }
        }
        System.out.println("Granule: " +String.valueOf(startGranule) + " -> " + String.valueOf(sizeGranule));
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

        System.out.println("Grid Line: " + this.gridLines);
        System.out.println("Grid Pixel: " + this.gridPixels);
        System.out.println();
    }


}