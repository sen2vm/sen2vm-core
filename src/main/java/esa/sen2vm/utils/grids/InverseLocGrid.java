package esa.sen2vm.utils.grids;

import java.util.ArrayList;

import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import java.text.DecimalFormat;

import java.util.logging.Logger;


public class InverseLocGrid
{
    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(InverseLocGrid.class.getName());

    private Coordinates ul;
    private Coordinates lr;
    private int epsg;

    protected float stepX;
    protected float stepY;

    protected int pixelOrigin;
    protected int lineOrigin;
    protected int sizeLines;
    protected int sizePixels;

    protected ArrayList<Float> gridX;
    protected ArrayList<Float> gridY;

    /**
     * Constructor
     * @param ul_x in epsg referencial
     * @param ul_y in epsg referencial
     * @param lr_x in epsg referencial
     * @param lr_y in epsg referencial
     * @param epsg reference
     * @param step in epsg referencial
     */
    public InverseLocGrid(float ul_x, float ul_y, float lr_x, float lr_y,
                         String epsg, float step)
    {
        this.epsg = Integer.valueOf(epsg.substring(5));
        this.ul = new Coordinates(ul_y, ul_x, this.epsg);
        this.ul.transform();
        this.lr = new Coordinates(lr_y, lr_x, this.epsg);
        this.lr.transform();
        stepY = step;
        stepX = step;

        if (ul_y > lr_y)
        {
            stepY = -step;
        }
        if (ul_x > lr_x)
        {
            stepX = -step;
        }

        LOGGER.info("# Grid information");
        String log = "Step: (" + String.valueOf(this.stepX) + ", " + String.valueOf(this.stepY) + "); ";
        log = log + "UL: (" + String.valueOf(this.ul.getX()) + ", " + String.valueOf(this.ul.getY()) + ") ";
        log = log + "(" + String.valueOf(this.ul.getLongitude()) + ", " + String.valueOf(this.ul.getLatitude()) + "); ";
        log = log + "LR: (" + String.valueOf(this.lr.getX()) + ", " + String.valueOf(this.lr.getY()) + ") ";
        log = log + "(" + String.valueOf(this.ul.getLongitude()) + ", " + String.valueOf(this.ul.getLatitude()) + ")";
        LOGGER.info(log);

        this.gridY = grid_1D(ul_y + stepY / 2, lr_y - ul_y, stepY);
        this.gridX = grid_1D(ul_x + stepX / 2, lr_x - ul_x, stepX);
        // LOGGER.info("Grid Pixel: " + this.gridX);
        // LOGGER.info("Grid Line: " + this.gridY);
    }


    /**
     * Create the list of geo grid values for a specific range
     * @param star of the grid
     * @param size of the grid
     * @return list 1D
     */
    private ArrayList<Float> grid_1D(float start, float size, float signedStep)
    {
        ArrayList<Float> grid = new ArrayList<Float>();
        int nb = (int) Math.ceil(Math.abs(size / signedStep)) + 1;
        for (int i = 0; i < nb; i++)
        {
            grid.add(start + signedStep * i);
        }
        return grid;
    }

    /**
     * Create 2D grid from gridX and gridY
     * @return grid [[lon0, lat0, alt0], [lon1, lat1, alt1], ..]
     */
    public double[][] get2DgridLatLon()
    {
        int nbCols = this.gridX.size();
        int nbLines = this.gridY.size();
        double[][] grid = new double[nbCols * nbLines][2];

        // Init source/target SpatialReference and transformation
        SpatialReference sourceSRS = new SpatialReference();
        sourceSRS.ImportFromEPSG(this.epsg);
        SpatialReference targetSRS = new SpatialReference();
        targetSRS.ImportFromEPSG(4326);
        CoordinateTransformation transformer = new CoordinateTransformation(sourceSRS, targetSRS);

        for (int l = 0; l < nbLines; l ++)
        {
            for (int c = 0; c < nbCols; c ++)
            {
                double[] res = transformer.TransformPoint(this.gridX.get(c), this.gridY.get(l));
                grid[l*nbCols + c][0] = res[1];
                grid[l*nbCols + c][1] = res[0];
            }
        }
        return grid;
    }

     /**
     * Transform [[row0, col0], [row1, col1]..] to 3D grid before tiff saving
     * @return grid [[[col00, col01...], [col10, col11...], [[row00, row01..], [row10, row11..] ..]]
     */
     public double[][][] get3Dgrid(double[][] gridList, float pixelOffest, float lineOffest)
     {
        int nbCols = this.gridX.size();
        int nbLines = this.gridY.size();
        double[][][] grid = new double[2][nbLines][nbCols];;

        for (int l = 0; l < nbLines; l ++)
        {
            for (int c = 0; c < nbCols; c ++)
            {
                grid[0][l][c] = gridList[l*nbCols + c][1] + pixelOffest;
                grid[1][l][c] = gridList[l*nbCols + c][0] + lineOffest;
                if (Double.isNaN(grid[0][l][c])) {
                    grid[0][l][c] = -32768.0;
                }
                if (Double.isNaN(grid[1][l][c])) {
                    grid[1][l][c] = -32768.0;
                }

            }
        }
        return grid;
    }

    /**
     * Get StepX
     * @return stepX
     */
     public float getStepX()
    {
        return this.stepX;
    }

    /**
     * Get StepY
     * @return stepY
     */
    public float getStepY()
    {
        return this.stepY;
    }
}
