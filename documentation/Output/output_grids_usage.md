[README](../../README.md)
* [HOWTO](../Usage/HOWTO.md)
* [Inputs](..//Input/input_description.md)
* Outputs:

  * [direct location grids](../Output/output_direct_loc.md)
  * [inverse location grids](../Output/output_inverse_loc.md)
  * [output grids usage](../Output/output_grids_usage.md)

# Example of usage of of the output grids

Please note that:

* thoses examples are also implemented in <mark>Notebooks</marks>
* gdal is used in those examples, which required a specific version
* [OTB](https://www.orfeo-toolbox.org/CookBook/Applications/app_GridBasedImageResampling.html) can also be used in parallel, with finer tuning

> [!CAUTION]
> gdal version shall be compatible with the new Sen2VM grids. GDAL version 3.12 is then required


## 1 Resampling using direct locations grids

Direct location grids can be used to preform a resampling. It can be done using gdal or using [OTB](https://www.orfeo-toolbox.org/CookBook/Applications/app_GridBasedImageResampling.html) resampler. To see the geometric validation of those 2 methods, please refer to the <mark>**Validation Document**</mark>

### 1.1 Using gdal

L1B with geolocation grids are seamlessly handled by gdal through the development of a dedicated S2 L1B gdal driver.  

```python
# If needed, point to the local gdal version handling Sen2VM grids
# export PATH=~/code/senv2vm/bin/bin/:$PATH
# export LD_LIBRARY_PATH=~/code/senv2vm/bin/lib:$LD_LIBRARY_PATH


#Initialisation of the product for gdal
gdalinfo  /PATH_TO_DATA/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/S2B_OPER_MTD_SAFL1B_PDMC_20241022T154709_R023_V20241019T120217_20241019T120235.xml

# Creation of a mosaic for D09_B01 (D - detector; B - band)
gdal_translate  SENTINEL2_L1B_WITH_GEOLOC:"/PATH_TO_DATA/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/S2B_OPER_MTD_SAFL1B_PDMC_20241022T154709_R023_V20241019T120217_20241019T120235.xml":S2B_OPER_GEO_L1B_DS_2BPS_20241019T153411_S20241019T120215_D09_B01 /PATH_TO_DATA/working/madeire_D09_B01.tif

# Resampling/orthorectification using gdal
gdal_warp SENTINEL2_L1B_WITH_GEOLOC:"/PATH_TO_DATA/S2B_OPER_MTD_SAFL1B_PDMC_20241022T154709_R023_V20241019T120217_20241019T120235.xml":S2B_OPER_GEO_L1B_DS_2BPS_20241019T153411_S20241019T120215_D09_B04 /PATH_TO_DATA/working/projected_D09_B04.tif -t_srs EPSG:32628 -tr 10 -10
```

### 1.2 Using otb
This method can be resumed into three main steps:
 * Creation of a mosaic of all images,
 * Convertion of the direct location grid into an inverse location grid using scipy,
 * Computation of the otb resampling using the mosaic and the inverse location grid.


```python
# If needed, point to the local gdal version handling Sen2VM grids
# export PATH=~/code/senv2vm/bin/bin/:$PATH
# export LD_LIBRARY_PATH=~/code/senv2vm/bin/lib:$LD_LIBRARY_PATH


# To get the name of the grid for your band/detector either
# - look inside teh DATASTRIP/S*/GEO_DATA folder 
# - launch a gdalinfo to get the list
gdalinfo  /PATH_TO_DATA/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/S2B_OPER_MTD_SAFL1B_PDMC_20241022T154709_R023_V20241019T120217_20241019T120235.xml

# Creation of a mosaic of images of all granules (for D09_B01)
gdal_translate  SENTINEL2_L1B_WITH_GEOLOC:"/PATH_TO_DATA/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/S2B_OPER_MTD_SAFL1B_PDMC_20241022T154709_R023_V20241019T120217_20241019T120235.xml":S2B_OPER_GEO_L1B_DS_2BPS_20241019T153411_S20241019T120215_D09_B01 /PATH_TO_DATA/working/madeire_D09_B01.tif

# Conversion of Sen2VM direct location grid into an inverse location grid with a 45m step
python sen2vm_invloc_from_dir_loc_grid.py /PATH_TO_DATA/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/DATASTRIP/S2B_OPER_MSI_L1B_DS_2BPS_20241019T153411_S20241019T120215_N05.11/GEO_DATA/S2B_OPER_GEO_L1B_DS_2BPS_20241019T153411_S20241019T120215_D09_B01.vrt --loglevel INFO 45.0  /PATH_TO_DATA/working/grid_D09_B01.tif

# Use OTB for resampling (default BCO interpolator) with
# - io.in: Mosaic of all images
# - grid.in: an inverse location grid
otbcli_GridBasedImageResampling -io.in  /PATH_TO_DATA/working/madeire_D09_B01.tif -io.out /PATH_TO_DATA/working/warp_otb_D09_B01.tif -grid.in  /PATH_TO_DATA/working/grid_D09_B01.tif -grid.type loc -out.ulx 293050  -out.uly 3697900 -out.spacingx 60 -out.spacingy -60 -out.sizex 933   -out.sizey   2040

# Add georeferencing to the image
gdal_translate -a_srs EPSG:32628 /PATH_TO_DATA/working/warp_otb_D09_B01.tif /PATH_TO_DATA/working/warp_otb_D09_B01_georef.tif
```

Please note that the script ```sen2vm_invloc_from_dir_loc_grid.py``` used by this method is directly available on this git, at this [location](/assets/scripts/sen2vm_invloc_from_dir_loc_grid.py).

Necessary prerequisites: 
 * numpy
 * rasterio
 * utm
 * scipy
 * argparse
 * pathlib

## 2 Resampling using inverse locations grids

> [!CAUTION]
> Please note that there is currently an [issue]((https://gitlab.orfeo-toolbox.org/orfeotoolbox/otb/-/issues/2317)) on the OTB side. Until corrected, **the grid information must be adjusted by half the resolution of the target pixel** (spacing) in both directions, to be synchronised as following:
>  * out.ulx **shall be updated to 293080** ( = 293050 + 60/2)
>  * out.uly **shall be updated to 3697870** ( = 3697900 + (-60)/2)

```python
# If needed, point to the local gdal version handling Sen2VM grids
# export PATH=~/code/senv2vm/bin/bin/:$PATH
# export LD_LIBRARY_PATH=~/code/senv2vm/bin/lib:$LD_LIBRARY_PATH


# To get the name of the grid for your band/detector either:
# - look inside the DATASTRIP/S*/GEO_DATA folder 
# - launch a gdalinfo to get the list
gdalinfo  /PATH_TO_DATA/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/S2B_OPER_MTD_SAFL1B_PDMC_20241022T154709_R023_V20241019T120217_20241019T120235.xml

# Creation of a mosaic of images of all granules (for D09_B01)
gdal_translate  SENTINEL2_L1B_WITH_GEOLOC:"/PATH_TO_DATA/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/S2B_OPER_MTD_SAFL1B_PDMC_20241022T154709_R023_V20241019T120217_20241019T120235.xml":S2B_OPER_GEO_L1B_DS_2BPS_20241019T153411_S20241019T120215_D09_B01 /PATH_TO_DATA/working/madeire_D09_B01.tif


# Use OTB for resampling (default BCO interpolator) with:
# - io.in: Mosaic of all images
# - grid.in: an inverse location grid
otbcli_GridBasedImageResampling -io.in  /PATH_TO_DATA/working/madeire_D09_B01.tif -io.out /PATH_TO_DATA/working/warp_otb_D09_B01.tif -grid.in  /PATH_TO_DATA/OUTPUT_INV_GRID/XXX.tif -grid.type loc -out.ulx 293050  -out.uly 3697900 -out.spacingx 60 -out.spacingy -60 -out.sizex 933   -out.sizey   2040

# Add georeferencing to the image
gdal_translate -a_srs EPSG:32628 /PATH_TO_DATA/working/warp_otb_D09_B01.tif /PATH_TO_DATA/working/warp_otb_D09_B01_georef.tif
```

# 

[README](../../README.md)
* [HOWTO](../Usage/HOWTO.md)
* [Inputs](..//Input/input_description.md)
* Outputs:

  * [direct location grids](../Output/output_direct_loc.md)
  * [inverse location grids](../Output/output_inverse_loc.md)
  * [output grids usage](../Output/output_grids_usage.md)