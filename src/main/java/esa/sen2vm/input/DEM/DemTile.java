package esa.sen2vm.input.DEM;

public class DemTile
{
    public double minX;
    public double maxX;
    public double minY;
    public double maxY;
    public String filePath;

    public DemTile(double a_minX, double a_maxX, double a_minY, double a_maxY, String filePathString)
    {
        minX = a_minX;
        maxX = a_maxX;
        minY = a_minY;
        maxY = a_maxY;
        filePath = filePathString;
    }

    public boolean containPoint(double x, double y)
    {
        return x >= minX && x <= maxX &&
            y >= minY && y <= maxY;
    }

    @Override
    public String toString()
    {
        return "DemTile (" + minX + ";" + maxX + "),(" + minY + ";" + maxY + ")" + filePath;
    }
}