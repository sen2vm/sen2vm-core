package esa.sen2vm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class InverseLocGrid {

    private Coordinates ul ;
    private Coordinates lr;

    protected Float step ;

    protected int pixelOrigin ;
    protected int lineOrigin ;
    protected int sizeLines ;
    protected int sizePixels ;

    protected ArrayList<Double> gridLats ;
    protected ArrayList<Double> gridLongs ;

    public InverseLocGrid(Float ul_x, Float ul_y, Float lr_x, Float lr_y,
                         int epsg, String OutputFolder,
                         Float step,
                         int pixelOrigin, int lineOrigin,
                         int sizePixels, int sizeLines) {
        this.ul = new Coordinates(ul_x, ul_y, epsg);
        this.ul.transform();
        this.lr = new Coordinates(lr_x, lr_y, epsg);
        this.lr.transform();

        this.step = step;
        this.pixelOrigin = pixelOrigin;
        this.lineOrigin = lineOrigin;
        this.sizePixels = sizePixels;
        this.sizeLines = sizeLines;

        System.out.println("# Grid information");
        System.out.print("Step: (" + String.valueOf(this.step) + ", " + String.valueOf(this.step) + ") ; ");
        System.out.print("Start: (" + String.valueOf(this.pixelOrigin) + ", " + String.valueOf(this.lineOrigin) + ") ; ");
        System.out.println("Size: (" + String.valueOf(this.sizePixels) + ", " + String.valueOf(this.sizeLines) + ")");
        System.out.println();
    }
}