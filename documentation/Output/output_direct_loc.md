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


# Direct location grids

A direct location grid is a grid which maps sensor coordinates with ground ones in WGS84 coordinates (EPSG:4326). Direct location grid is regular and in sensor reference frame (for one  band/detector couple).

Sen2VM direct location grid computation takes as input the L1B product, the auxiliary information, GIPP, Altitude, IERS and the grid parametrization:

* Bands/detectors to process,
* 3 steps, one per band resolution (10m, 20m, 60m) in pixels (double)

For more details on these inputs (including the step value constraint for direct location grids), please refer to [Input Description](../Input/input_description.md).

As output:
* At granule level: geolocation grids will be written (per granules/bands).
* At datastrip level:
    * several .vrt (virtual dataset) will be written (per detectors/bands).
    * The configuration file used in input with the date/time will be added in with the vrt files

## 1 Direct locations grids' outputs

Output grids will be integrated directly in the input product.
>  [!CAUTION]
> Please note that writing permissions in the L1B input folder are **mandatory**.

Before processing, **a verification will be done** to determine whether direct location grids are already available in the input L1B product folder, for the detectors/bands selected. If at least one is present for one couple detector/band, Sen2VM **will raise an error and stop**. Both granules and datastrip folder will be inspected (see output grids format and location in the following sections).

### 1.1 Granule level

Grids’ location and naming is at granules level:

* 1 grid per couple “L1B granule”/”Sentinel-2 band”,
* Grids **include 2 (_optionally 3_) bands (Long/Lat/_alt_)**
* Grids are in **geotiff format with float32 coding positions** which allow approximately centimetre precision for lat/lon coordinates. JP2000 is not suitable, as it does not support Float32 encoding, resulting in insufficient precision.
* Grids location will be inside a **GEO_DATA folder** which will be inside each granules folders (at the same level than IMG_DATA and QI_DATA folders),
* Grids naming conventions will respect the corresponding image data inside the IMG_DATA folder with:

    * GEO instead of MSI
    * .tif instead of .jp2 extension as jp2 encoding is not possible for float32 data.

As example, for an image of the IMG_DATA folder, named:

* S2B_OPER_**MSI**_L1B_GR_DPRM_20140630T140000_S20230428T151505_D02_B01.<strong>jp2</strong>

The direct location grid will be generated in the GEO_DATA folder, and named:

* S2B_OPER_**GEO**_L1B_GR_DPRM_20140630T140000_S20230428T151505_D02_B01.<strong>tif</strong>

> [!NOTE]
> The configuration file used in input with the date/time will be added in with the vrt files

### 1.2 Datastrip level

At datastrip level grids’ location and naming is:

* 1 vrt per couple “detector”/”Sentinel-2 band”,
* Grids **include 2 (_optionally 3_) bands (Long/Lat/_alt_)**
* Grid location will be located inside a **GEO_DATA folder**, which resides within the **DATASTRIP** directory, at the same level as the QI_DATA folder.
* Grids naming conventions will respect the corresponding datastrip metadata convention with:

    * **GEO** instead of **MTD**
    * <strong>_DXX_BYY.vrt</strong> instead of <strong>.xml</strong> extension.

As example, for an datastrip metadata of the DATASTRIP folder, named:

* S2B_OPER_**MTD**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>.xml</strong>

A folder named GEO_DATA, beside the QI_DATA folder and datastrip metadata will contain 156 vrt files (12 detectors x 13 bands) named:

* S2B_OPER_**GEO**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>_DXX_BYY.vrt</strong>

Example of product with grid inside it:

![Output example of Datastrip vrt for direct location grids](/assets/images/README_OutputDatastrip.PNG "Output example of Datastrip vrt for direct location grids.")

## 2 Direct location grids’ specifications

> [!NOTE]
> To be consistent with granules convention with first pixel center, outputs grids have the same first pixel centres.

Grids characteristics are:
 
 * Convention: grid raster type is "point" with first grid cell centre at first pixel centre of the first granule (referred by PIXEL_ORIGIN in granule geometric info). Grid position given an image position (_row_, _col_), is

   ![Direct grid handling](/assets/images/README_DirectGridHandling.png "Direct Grid Handling.")
   
with (_grid row_,_grid col_) = (_Pixel Origin_, _Pixel Origin_) = (_1_,_1_) at center of first grid cell

Example:

 ![Direct convention](/assets/images/README_DirectConvention.png "Direct convention.")

 * Grid Metadata: Grids will contain Metadata information of [GDAL geolocation grids](https://gdal.org/development/rfc/rfc4_geolocate.html):

     * SRS: WGS84 WKT format
     * PIXEL_OFFSET
     * LINE_OFFSET
     * PIXEL_STEP: GRID STEP
     * LINE_STEP: GRID STEP
     * GEOREFERENCING_CONVENTION: PIXEL_CENTER

 * Overlap: to ensure grid granule continuity, one pixel overlap will be added. Taking advantage of this overlap, one granule can be individually resampled, or multiple granules (whole Datastrip for example) using the vrt grid concatenation without extra disk cost). Thus _PIXEL/LINE OFFSET_ of the Grid Metadata will vary for each grid.

  ![Direct overlpas](/assets/images/README_DirectOverlaps.png "Direct overlaps.")
 
 

## 3 Grid handling

Direct location grids are intended to be used with bilinear interpolation operation. Direct location (i.e lon/lat positions) should be as follow:

 * Given an image position (_row_/_col_) compute grid fractional position (grid row, grid col):

   ![Direct grid handling](/assets/images/README_DirectGridHandling.png "Direct Grid Handling.")

 * Use bilinear interpolation on (_grid row_, _grid col_) to retrieve lon/lat/(alt).

> [!CAUTION]
> Note that based on the used convention, the [1,1] coordinate is the first pixel of the grid

> [!TIP]
> If user wants to perform direct location of a position outside the granule footprint, a bilinear extrapolation is possible.

## 4 Degraded cases

Grids should at least have 2x2 cells

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