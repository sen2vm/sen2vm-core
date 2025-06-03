package esa.sen2vm.utils.grids;

import java.util.ArrayList;

import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import java.text.DecimalFormat;
import esa.sen2vm.utils.Sen2VMConstants;

import java.util.logging.Logger;


public class InverseLocGrid
{
    // Get sen2VM logger
    private static final Logger LOGGER = Logger.getLogger(InverseLocGrid.class.getName());

    private int epsg;

    protected float stepX;
    protected float stepY;

    protected float ulX;
    protected float ulY;
    protected float lrX;
    protected float lrY;

    protected ArrayList<Float> gridX;
    protected ArrayList<Float> gridY;

    /**
     * Constructor
     * @param ulX in epsg referencial
     * @param ulY in epsg referencial
     * @param lrX in epsg referencial
     * @param lrY in epsg referencial
     * @param epsg reference
     * @param step in epsg referencial
     * @param res of the band in meters
     */
    public InverseLocGrid(float ulX, float ulY, float lrX, float lrY,
                         String epsg, float res, float step)
    {
        this.epsg = Integer.valueOf(epsg.substring(5));
        float resX = res;
        float resY = res;

        this.stepX = step;
        this.stepY = step;

        // test if upper and bottom are reversed
        if (ulY > lrY)
        {
            resY = -resY;
            this.stepY = -this.stepY;
        }

        // test if left and right are reversed
        if (ulX > lrX)
        {
            resX = -resX;
            this.stepX = -this.stepX;
       }

        // Synchro first grid point (center of a grid pixel) with the center of the first
        // image pixel (band resolution) of the area
        this.ulY = ulY - this.stepY / 2 + resY / 2;
        this.ulX = ulX - this.stepX / 2 + resX / 2;

        // Compute grid with center pixel convention
        // start to the pixel center
        // The LowerRight is englobing the last pixel, hence to cover the last pixel, the center of last pixel of the grid shall go over the LR - res / 2
        this.gridY = grid_1D(this.ulY + this.stepY / 2, lrY - resY / 2, this.stepY); 
        this.gridX = grid_1D(this.ulX + this.stepX / 2, lrX - resX / 2, this.stepX);

        // Compute englobing lower right of the grid after computation
        this.lrY = gridY.get(gridY.size()-1) + this.stepY / 2;
        this.lrX = gridX.get(gridX.size()-1) + this.stepX / 2;

        LOGGER.info("# Grid information");
        String log = "Step: (" + String.valueOf(this.stepY) + ", " + String.valueOf(this.stepX) + "); ";
        log = log + "UL: (" + String.valueOf(this.ulY) + ", " + String.valueOf(this.ulX) + ") ";
        log = log + "LR: (" + String.valueOf(this.lrY) + ", " + String.valueOf(this.lrX) + ") ";
        LOGGER.info(log);
    }


    /**
     * Test if value + step is above end
     * @param value to test
     * @param end
     * @param signedStep of the grid
     * @return true/false
     */
    private boolean testEnd(float value, float end, float signedStep)
    {
        if (signedStep > 0)
        {
            return (value + signedStep < end);
        }
        else
        {
            return (value + signedStep > end);
        }
    }


    /**
     * Create the list of geo grid values for a specific range
     * @param star of the grid
     * @param end min of the grid
     * @param signedStep of the grid
     * @return list 1D
     */
    private ArrayList<Float> grid_1D(float start, float end, float signedStep)
    {
        ArrayList<Float> grid = new ArrayList<Float>();

        float value = start;
        grid.add(value);

        System.out.println(value);

        while(testEnd(value, end, signedStep))
        {
            value = value + signedStep;
            grid.add(value);
        }

        grid.add(value + signedStep);
        System.out.println(String.valueOf(grid.size()));
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
                grid[l*nbCols + c][0] = res[0];
                grid[l*nbCols + c][1] = res[1];
            }
        }
        System.out.println("tl");

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
                    grid[0][l][c] = Sen2VMConstants.noDataRasterValue;
                }
                if (Double.isNaN(grid[1][l][c])) {
                    grid[1][l][c] = Sen2VMConstants.noDataRasterValue;
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

     /**
     * Get ulX
     * @return ulX
     */
     public float getUlX()
    {
        return this.ulX;
    }

     /**
     * Get ulY
     * @return ulY
     */
     public float getUlY()
    {
        return this.ulY;
    }

     /**
     * Get lrX
     * @return lrX
     */
     public float getLrX()
    {
        return this.lrX;
    }

     /**
     * Get lrY
     * @return lrY
     */
     public float getLrY()
    {
        return this.lrY;
    }

     /**
     * Get gridX
     * @return gridX
     */
     public ArrayList<Float> getGridX()
    {
        return this.gridX;
    }

     /**
     * Get gridY
     * @return gridY
     */
     public ArrayList<Float> getGridY()
    {
        return this.gridY;
    }
}