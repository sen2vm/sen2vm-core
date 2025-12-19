[README](../../README.md)

* [HOWTO](../Usage/HOWTO.md)
* [Inputs description](../Input/input_description.md)

  * [How to Download L1B Data from CDSE](../Input/L1B_CDSE_Download.md)
  * [How to Download DEM Data from CDSE](../Input/DEM_CDSE_Download.md)

* Outputs description:

  * [Direct location grids](../Output/output_direct_loc.md)
  * [Inverse location grids](../Output/output_inverse_loc.md)
  * [Output grids usage](../Output/output_grids_usage.md)
  * [Notebooks](../../sen2vm-notebook/README_Notebooks.md)

# Inverse location grids

An inverse location grid is a grid which maps ground coordinates with sensor ones.
Inverse location grids are georeferenced in **geographic or cartographic reference frame**.
Inverse location grid is regular in **ground reference frame** (for one band and one detector).

To define the extend of the inverse location grid, parameters are described in §[Input Description](../Input/input_description.md), but it can be resumed at:

* A referential system,
* A square defined by:

    * One Upper Left point (UL),
    * One Lower Right point (LR),

* 3 steps, one per band resolution (10m, 20m, 60m) in the referential metrics,
* An output folder.

As output, a geolocation grid will be created **for each band and detector** to process.

## 1 Inverse location grids’ outputs

Outputs will be **written in the folder provided by the user**. For inverse location grids, granule level is not foreseen, since granule footprint can result in large margins in projected data.

Output grids’ convention will be:

* 13 grids (1 per Sentinel-2 band) per detector intersecting the area,
* Grids are in **geotiff format with float32 coding positions** which allow decimal information on **col/row** positions. JP2000 is not suitable, as it does not support Float32 encoding, resulting in insufficient precision.
* Grids naming conventions will respect the corresponding datastrip metadata convention with:

    * **INV** instead of **MTD**,
    * <strong>_DXX_BYY.tif</strong> instead of <strong>.xml</strong> extension.

As example, for a Datastrip metadata of the DATASTRIP folder:

* S2B_OPER_**MTD**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>.xml</strong>

Output will be named:

* S2B_OPER_**INV**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>_DXX_BYY.tif</strong>

Example:

![Output example of inverse location grids](/assets/images/README_OutputInv.PNG "Output example of inverse location grids.")

> [!NOTE]
> The configuration file used in input with the date/time will be added in with the vrt files

## 2 Inverse location grids’ specifications

Inverse location grids **will give the coordinates in the “detector” image** reference frame, as if the granules from the same detector were concatenated into a single image.

 * Grid metadata:

   * are written as GDAL GEO information (SRS and geotransform) and Metadata keys, containing ROW_BAND and COL_BAND to specify row col index and PIXEL_ORIGIN which specify granule first pixel center convention, and NoData value.  
   * Metadata keys are:

       * the SRS
       * NoData fill value
       * the grid footprint
       * the size of the grid,
       * the step of the grid,
       * margins,
         
To ensure input bounding box coverage:

* Center of the first grid point is aligned with UL of the bounding box,
* Center of the last grid pixel is computed to ensure that it covers at least LR of the input bounding box.

As for example:

![Inverse Convention](/assets/images/README_InverseConvention.png "Inverse Convention.")
 
## 3 Grid handling

Grids are intended to be used with bilinear interpolation operation. Inverse locations (i.e image position given a ground position) should be as follow:

 * Given an ground position (_lon_/_lat_) or (_x_, _y_) compute grid fractional position (grid row, grid col) using grid geotransform for GRID_ORIGIN and GRID_STEP:

     ![Inverse grid handling](/assets/images/README_InverseGridHandling.png "Inverse Grid Handling.")

 * Use bilinear interpolation on (_grid row_, _grid col_) to retrieve _row_/_col_.
 
> [!CAUTION]
> Beware of convention [PIXEL ORIGIN,PIXEL ORIGIN] is the first pixel of the grid..

## 4 Degraded cases

Grids should at least have 2x2 cells.

#

[README](../../README.md)

* [HOWTO](../Usage/HOWTO.md)
* [Inputs description](../Input/input_description.md)

  * [How to Download L1B Data from CDSE](../Input/L1B_CDSE_Download.md)
  * [How to Download DEM Data from CDSE](../Input/DEM_CDSE_Download.md)

* Outputs description:

  * [Direct location grids](../Output/output_direct_loc.md)
  * [Inverse location grids](../Output/output_inverse_loc.md)
  * [Output grids usage](../Output/output_grids_usage.md)
  * [Notebooks](../../sen2vm-notebook/README_Notebooks.md)