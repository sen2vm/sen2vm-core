package esa.sen2vm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;


public class DirectLocGrid {

    protected int pixelOffset ;
    protected int lineOffset ;
    protected int pixelStep ;
    protected int lineStep ;
    protected ArrayList<Double> gridPixels ;
    protected ArrayList<Double> gridLines ;

    public DirectLocGrid(int pixelOffset, int lineOffset,
                         int pixelStep, int lineStep) {
        this.pixelOffset = pixelOffset;
        this.lineOffset = lineOffset;
        this.pixelStep = pixelStep;
        this.lineStep = lineStep;

        System.out.println("# DataStrip information");
        System.out.println("Offset: (" + String.valueOf(this.pixelOffset) + ", " + String.valueOf(this.lineOffset) + ") ; ");
        System.out.println("Step: (" + String.valueOf(this.pixelStep) + ", " + String.valueOf(this.lineStep) + ")");
        System.out.println();
    }

    public void initDirectGrid(Map<String, Object> infoGranuleImage) {

        int pixelOrigin = (Integer) infoGranuleImage.get("pixelOrigin") ;
        int lineOrigin = (Integer)  infoGranuleImage.get("granulePosition") ;
        int sizeLines = (Integer)  infoGranuleImage.get("granuleDimensions_nrows") ;
        int sizePixels = (Integer)  infoGranuleImage.get("granuleDimensions_ncols") ;

        System.out.print("Origin: (" + String.valueOf(pixelOrigin) + ", " + String.valueOf(lineOrigin) + ") ; ");
        System.out.println("Size: (" + String.valueOf(sizePixels) + ", " + String.valueOf(sizeLines) + ")");

        this.gridPixels = grid_1D(pixelOrigin, sizePixels, pixelOrigin, this.pixelOffset, this.pixelStep);
        this.gridLines = grid_1D(lineOrigin, sizeLines, pixelOrigin, this.lineOffset, this.lineStep);

        // ArrayList<Double> grids = mersh2D(this.gridPixels, this.gridPixels);

        System.out.println("Grid Pixel: " + this.gridPixels);
        System.out.println("Grid Line: " + this.gridLines);
        System.out.println();
    }

    private ArrayList<Double> grid_1D(int start, int size, double origin, double offset, double step) {
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

}