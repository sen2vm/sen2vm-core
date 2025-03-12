package esa.sen2vm.utils.grids;

import java.util.ArrayList;

import java.util.logging.Logger;

public class DirectLocGrid
{
    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(DirectLocGrid.class.getName());

    protected float georefConventionOffsetPixel;
    protected float georefConventionOffsetLine;
    protected float step;
    protected int pixelOrigin;
    protected int lineOrigin;
    protected int sizeLines;
    protected int sizePixels;

    protected ArrayList<Float> gridPixels;
    protected ArrayList<Float> gridLines;

    /**
     * Constructor
     * @param georefConventionOffset
     * @param step
     * @param lineOrigin
     * @param pixelOrigin
     * @param sizeLines
     * @param sizePixels
     */
    public DirectLocGrid(float georefConventionOffsetLine, float georefConventionOffsetPixel,
                         float step,
                         int lineOrigin, int pixelOrigin,
                         int sizeLines, int sizePixels)
    {
        this.georefConventionOffsetPixel = georefConventionOffsetPixel;
        this.georefConventionOffsetLine = georefConventionOffsetLine;
        this.step = step;
        this.pixelOrigin = pixelOrigin;
        this.lineOrigin = lineOrigin;
        this.sizePixels = sizePixels;
        this.sizeLines = sizeLines;

        this.gridPixels = grid_1D(this.pixelOrigin, this.sizePixels, this.pixelOrigin, this.step, this.georefConventionOffsetPixel);
        this.gridLines = grid_1D(this.lineOrigin, this.sizeLines, this.pixelOrigin, this.step, this.georefConventionOffsetLine);

        String info = "ConvOffset: (" + String.valueOf(this.georefConventionOffsetLine) + ", " + String.valueOf(this.georefConventionOffsetPixel) + ");";
        info = info + " Step: (" + String.valueOf(this.step) + ", " + String.valueOf(this.step) + ");";
        info = info + " Start: (" + String.valueOf(this.lineOrigin) + ", " + String.valueOf(this.pixelOrigin) + ");";
        info = info + " Size: (" + String.valueOf(this.sizeLines) + ", " + String.valueOf(this.sizePixels) + ")";
        LOGGER.info("Grid information: " + info);

    }

    /**
     * Create 2D grid from gridPixels and gridLines [len(gridPixels) * 2]
     * [[row0+lineOffset,col0+pixelOffest],[row1+lineOffset,col1+pixelOffest],...]
     * @param pixelOffest for s2geo convention
     * @param lineOffest for s2geo convention
     */
    public double[][] get2Dgrid(float pixelOffest, float lineOffest)
    {

        int nbCols = this.gridPixels.size();
        int nbLines = this.gridLines.size();

        double[][] grid = new double[nbCols * nbLines][2];
        for (int l = 0; l < nbLines; l ++)
        {
            for (int c = 0; c < nbCols; c ++)
            {
                grid[l*nbCols + c][0] = this.gridLines.get(l) + lineOffest + georefConventionOffsetLine;
                grid[l*nbCols + c][1] = this.gridPixels.get(c) + pixelOffest + georefConventionOffsetPixel;
            }
        }

        return grid;
    }

    /**
     * Create the list of geo grid values for a specific range
     * @param start of the image
     * @param size of the image
     * @param pixelOrigin of the grid
     * @param step of the grid
     * @return list 1D
     */
    private ArrayList<Float> grid_1D(int start, int size, int pixelOrigin, float step, float offset)
    {
        Boolean insideGranule = true;
        ArrayList<Float> grid = new ArrayList<Float>();

        int i_grid = 0;
        float value = offset + (i_grid -  pixelOrigin) * step - step / 2;
        grid.add(value);

        while (insideGranule)
        {
            value = value + step;
            if (value > start + size)
            {
                insideGranule = false;
            }
            else
            {
                grid.add(value);
                i_grid ++;
            }
        }

        return grid;
    }

    /**
     * Create corresponding value pixel in the grid (upper)
     * @param pixel value in image
     * @rturn pixel value in grid
     */
    public int getRowInGrid(int pixel)
    {
        double start = ((pixel + this.step / 2) * 1.0 / this.step);
        return (int) Math.floor(start);
    }

    /**
     * Select sub grid between startGranule and startGranule + sizeGranule
     * @param directLocGrid 2d ground coordinate array as [[lon0,lat0,alt0],[lon1,lat1,alt1],...,[lonn,latn,altn]] in (deg,deg,m)
     * @param startGranule
     * @param sizeGranule
     * @return subDirectLocGrid of directLocGrid specific to granule
     * 3d ground coordinate array as [[[lon00,lon01,...],[...]],[[lat00,lat01,,...], [...]],[[alt00,alt01,,...], [...]]] in deg, deg, m
     */
    public double[][][] extractPointsDirectLoc(double[][] directLocGrid, int startGranule, int sizeGranule, boolean exportAlt)
    {
        LOGGER.info("Granule: " + String.valueOf(startGranule) + " -> " + String.valueOf(startGranule + sizeGranule));

        int grid_row_start = getRowInGrid(startGranule);
        int grid_row_end = getRowInGrid(startGranule + sizeGranule) ;

        int nbLines = grid_row_end - grid_row_start + 1;
        int nbCols = this.gridPixels.size();

        String log = "Grid start: " + String.valueOf(grid_row_start) + " (";
        log = log + String.valueOf(this.gridLines.get(grid_row_start)) + ")";
        log = log + " -> Grid end: " + String.valueOf(grid_row_end) + " (";
        log = log + String.valueOf(this.gridLines.get(grid_row_end)) + ")";
        log = log + " // Line Offset: " + String.valueOf(this.gridLines.get(grid_row_start) - startGranule);
        log = log + " Pixel Offset: " + String.valueOf(this.gridPixels.get(0));
        LOGGER.info(log);

        int nbBands = 3;
        if (exportAlt == false)
        {
            nbBands = 2;
        }

        double[][][] subDirectLocGrid = new double[nbBands][nbLines][nbCols];
        for (int l = 0; l < nbLines; l++)
        {
            for (int c = 0; c < nbCols; c++)
            {
                for (int b = 0; b < nbBands; b++)
                {
                    subDirectLocGrid[b][l][c] = directLocGrid[(l + grid_row_start)*nbCols + c][b];
                }
            }
        }

        return subDirectLocGrid;
    }

     /**
     * Compute offset of a granule by the ds start
     * @return lineOffset
     */
    public float getLineOffsetGranule(int startGranule)
    {
        int grid_row_start = getRowInGrid(startGranule);
        float lineOffset = this.gridLines.get(grid_row_start) - startGranule;
        return lineOffset;
    }

     /**
     * Compute offset of a granule by the ds start
     * @return pixelOffset
     */
    public float getPixelOffsetGranule()
    {
        return this.gridPixels.get(getRowInGrid(0));
    }

    /**
     * Get grid Pixels
     * @return gridPixels
     */
    public ArrayList<Float> getGridPixels()
    {
        return this.gridPixels;
    }

    /**
     * Get grid lines
     * @return gridLines
     */
    public ArrayList<Float> getGridLines()
    {
        return this.gridLines;
    }
}
