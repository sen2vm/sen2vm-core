package esa.sen2vm;

public enum BandS2 {
    B01(60), B02(10), B03(10), B04(10), B05(20), B06(20), B07(20),
    B08(10), B8A(20), B09(60), B10(60), B11(20), B12(20);

    private int resolution;

    public int getResolution() {
        return resolution;
    }

    BandS2(int resolution){
        this.resolution = resolution;
    }

};