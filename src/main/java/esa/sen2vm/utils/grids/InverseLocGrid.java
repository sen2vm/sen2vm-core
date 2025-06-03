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

    protected double stepX;
    protected double stepY;

    protected double ulX;
    protected double ulY;
    protected double lrX;
    protected double lrY;

    protected ArrayList<Double> gridX;
    protected ArrayList<Double> gridY;

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
    public InverseLocGrid(double ulX, double ulY, double lrX, double lrY,
                         String epsg, double res, double step)
    {
        this.epsg = Integer.valueOf(epsg.substring(5));
        double resX = res;
        double resY = res;

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
    private boolean testEnd(double value, double end, double signedStep)
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
    private ArrayList<Double> grid_1D(double start, double end, double signedStep)
    {
        ArrayList<Double> grid = new ArrayList<Double>();

        double value = start;
        grid.add(value);


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
     * @return grid [[lon0, lat0], [lon1, lat1], ..]
     */
    public double[][] get2DgridLatLon()
    {
        if (this.epsg == 4326)
        {
            return get2Dgrid();
        }
        else
        {
            return get2DgridWithConvLatLon();
        }
    }

    /**
     * Create 2D grid with lat lon from gridX and gridY
     * @return grid [[lon0, lat0], [lon1, lat1], ..]
     */
    public double[][] get2DgridWithConvLatLon()
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
        System.out.println("tl");

        return grid;
    }

    /**
     * Create 2D grid from gridX and gridY
     * @return grid [[lon0, lat0], [lon1, lat1], ..]
     */
    public double[][] get2Dgrid()
    {
        int nbCols = this.gridX.size();
        int nbLines = this.gridY.size();
        double[][] grid = new double[nbCols * nbLines][2];

        for (int l = 0; l < nbLines; l ++)
        {
            for (int c = 0; c < nbCols; c ++)
            {
                grid[l*nbCols + c][0] = (double) this.gridX.get(c);
                grid[l*nbCols + c][1] = (double) this.gridY.get(l);
            }
        }
        return grid;
    }

     /**
     * Transform [[row0, col0], [row1, col1]..] to 3D grid before tiff saving
     * @return grid [[[col00, col01...], [col10, col11...], [[row00, row01..], [row10, row11..] ..]]
     */
     public double[][][] get3Dgrid(double[][] gridList, double pixelOffest, double lineOffest)
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
     public double getStepX()
    {
        return this.stepX;
    }

    /**
     * Get StepY
     * @return stepY
     */
    public double getStepY()
    {
        return this.stepY;
    }

     /**
     * Get ulX
     * @return ulX
     */
     public double getUlX()
    {
        return this.ulX;
    }

     /**
     * Get ulY
     * @return ulY
     */
     public double getUlY()
    {
        return this.ulY;
    }

     /**
     * Get lrX
     * @return lrX
     */
     public double getLrX()
    {
        return this.lrX;
    }

     /**
     * Get lrY
     * @return lrY
     */
     public double getLrY()
    {
        return this.lrY;
    }

     /**
     * Get gridX
     * @return gridX
     */
     public ArrayList<Double> getGridX()
    {
        return this.gridX;
    }

     /**
     * Get gridY
     * @return gridY
     */
     public ArrayList<Double> getGridY()
    {
        return this.gridY;
    }
}