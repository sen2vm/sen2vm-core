# Project sen2vm-core
The Sen2VM core is a standalone tool designed to generate geolocation grids that will be included in the Level-1B (L1B) product. Its primary function is to create direct location grids, mapping the L1B product in sensor geometry down to the ground. Additionally, the tool supports the generation of inverse location grids, enabling mapping from a specific ground area back to the corresponding area in sensor geometry.

Please note that Sen2VM exists implemented as a SNAP plugin, which calls the Sen2VM standalone tool during execution.

This documentation is split into 4 parts:
* Instructions to  compile and/or run Sen2VM with some examples: $ [Quickstart](#1-quickstart)
* Description and format of expected inputs: § [Inputs](#2-inputs)
* Details on generated outputs: § [Outputs](#3-outputs)
* Validation process, including test procedures and data used: § [Validation](#4-validation)

## 1 Quickstart

### 1.1 How to run or compile sen2vm-core

First, download the jar of Sen2VM core, then run the following commad to launch it:
```
java -jar target/sen2vm-core-<NN.NN.NN>-jar-with-dependencies.jar -c [configuration_filepath] [-p [parameters_filepath]]
```

Where:
* <NN.NN.NN> is the version number of Sen2VM launched,
* configuration_filepath: configuration file containing all inputs related to product or grids that are required by Sen2VM (see §[2.1 Configuration file](#21-configuration-file) for further information). Please note that this input is **Mandatory**. 
* parameters_filepath:  file to configure the detectors/bands to process. If not available, all detectors/bands will be processed (see §XXX for further information).This input is **Optional**.

Example from current repository:
```
java -jar target/sen2vm-core-0.0.1-jar-with-dependencies.jar -c src/test/resources/configuration_example.json -p src/test/resources/params.json
```

Sen2VM core can also be rebuild from sources:
Before compiling/installing sen2vm-core, make sure to install the required dependencies. To do so, please refer to [https://github.com/sen2vm/sen2vm-build-env/tree/main](https://github.com/sen2vm/sen2vm-build-env/tree/main)

Then, inside `sen2vm-core` folder, run the next commands:
```
mvn clean install
java -jar target/sen2vm-core-<NN.NN.NN>-jar-with-dependencies.jar -c [configuration_filepath] [-p [parameters_filepath]]
```
### 1.2 Example of use

> [!CAUTION]
> gdal version shall be compatible with the new Sen2VM grids. Official gdal does not yet include this driver/possibility. A [Pull Request](https://github.com/OSGeo/gdal/pull/12431 ) is currently openned, but in the meantime, this gdal version can be find [here](https://github.com/rouault/gdal/tree/sen2vm_plus_s2c)


#### 1.2.1 Resampling using direct locations grids

Direct location grids can be used to preform a resampling. It can be done using gdal or using [OTB](https://github.com/rouault/gdal/tree/sen2vm_plus_s2c) resampler. To see the geometric validation of those 2 methods, please refer to the <mark>**Validation Document**</mark>

##### 1.2.1.1 Using gdal

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

##### 1.2.1.1 Using otb
This method consists of three main steps:
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

Necessary prerequisites include: 
 * numpy
 * rasterio
 * utm
 * scipy
 * argparse
 * pathlib

#### 1.2.2 Resampling using inverse locations grids

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


## 2. Inputs
Inputs required by Sen2VM are:
* L1B Product,
* Some GIPP files (parameters files used in operational production),
* Digital Elevation Model (DEM),
* GEOID model to measure precise surface elevations,
* IERS bulletin that provides data and standards related to Earth rotation and reference frames,
* Additional information for configuration.

All inputs are described in this section.

### 2.1 Configuration file
The configuration file will contain all information about the product, the auxiliarry data and the operations to be performed.
It  is a file in  [JSON format](https://en.wikipedia.org/wiki/JSON) and an example is available at: https://github.com/sen2vm/sen2vm-core/blob/main/src/test/resources/configuration_example.json

![Configuration file example](/assets/images/README_ConfigurationFileExample.png "Configuration file example.")

Each parameter description can be found in the table below:

| Name        | Type     | Required      | Description   |
| ----------- | :------: | :-----------: | :-----------: |
| l1b_product | string   | **Mandatory** | Path to L1B_PRODUCT folder, where L1B_Product format is in SAFE format (described in § [L1B Product](#211-l1b-product), including DATASTRIP + GRANULE)|
| gipp_folder | string   | **Mandatory** | Path to a folder containing at least the 3 types of GIPP required by Sen2VM (other will be ignored). For more information, refer to § [GIPP](#212-gipp).|
| gipp_check  | boolean  | Optional      | If true (default), check of GIPP version activated (see GIPP section § [GIPP](#212-gipp)</mark>)|
|dem          | string   | **Mandatory** | Path to the FOLDER containing a DEM in the right format (cf § [Altitude/DEM](#2131-dem)).|
|geoid        | string   | **Mandatory** | Path to the FILE containing a GEOID in the right format (cf § [Altitude/GEOID](#2132-geoid))|
| iers        | string   | Optional      | Path to the IERS folder containing the IERS file in the right format (cf § [IERS](#214-iers))|
|operation    | string   | **Mandatory** | In term of operation you can select the following Sen2VM configurations:<ul><li>“direct”: to compute direct location grids</li><li>“inverse”: to compute inverse location grids</li></ul>|
| deactivate_available_refining| boolean  | Optional      | If set to false (default), refining information (if available in Datastrip Metadata) are used to correct the model before geolocation, cf. product description in § [L1B Product](#2112-refining-information)|
| export_alt   | boolean  | Optional      | If set to false (default), direct location grids will include only two bands: **Lat/Long**. If set to true, a third band representing the **Altitude** will also be exported, increasing the output grid size. See product description in §[Direct location grids](#31-direct location grids)|
| steps       | float    | **Mandatory** | The step is mandatory and must be specified  as one per resolution: “10m_bands”, “20m_bands” & “60m_bands””. Please note that only floating numbers in the format NNNN.DDD are accepted and that the unit is given in pixel.|
| inverse_location_additional_info | | **Mandatory if “inverse”, else useless.**| For the inverse location additional information please refer to the dedicated table below|


The field “inverse_location_additional_info” is not required and will be ignored if direct location grids are asked. However, it is mandatory for inverse location grids generation and **Sen2VM will raise an error** if this information is missing.

| Name          | Type     | Required      | Description   |
| ------------- | :------: | :-----------: | :-----------: |
| ul_x          | float    | **Mandatory** | **X** coordinates of the **upper left** point defining the squared area where to compute the grids|
| ul_y          | float    | **Mandatory** | **Y** coordinates of the **upper left** point defining the squared area where to compute the grids|
| lr_x          | float    | **Mandatory** | **X** coordinates of the **lower right** point defining the squared area where to compute the grids|
| lr_y          | float    | **Mandatory** | **Y** coordinates of the **lower right** point defining the squared area where to compute the grids|
| referential   | string   | **Mandatory** | A string defining the **referential** used. **Please note, ul and lr points’ coordinates** are given in this referential. Example: EPSG:4326|
| output_folder | string   | **Mandatory**|Path where the inverse location grids are written (see § [Inverse location grids](#32-inverse-location-grids))|

#### 2.1.1 L1B Product
> [!NOTE]
> L1B products can be downloaded at [https://browser.dataspace.copernicus.eu/](https://browser.dataspace.copernicus.eu/). Please note that special access for L1B products might be required by submitting  a request via the FAQ section.

> [!IMPORTANT]
> The expected format is compatible with the SAFE format, i.e. a folder structured as illustrated in the following sections.

##### 2.1.1.1 L1B Product input tree structure
The mandatory inputs for Sen2Vm are the Datastrip and the Granules metadata. 

These metadata must be organized in a specific directory structure. Within the L1B folder, only the following subdirectories will be considered:
 * DATASTRIP
 * GRANULES

![L1B Folder](/assets/images/README_L1BProduct.png "Necessary folders.")

Additional files (as in the SAFE format) may be present in the folders, but they will simply be ignored by Sen2VM core.

As for the DATASTRIP folder, it must contain a subfolder named after the Datastrip reference and inside this subfolder, the Datastrip Metadata (also named after the Datastrip reference). This structure is compatible with the SAFE format and others files/folders will be ignored:

![DATASTRIP Folder](/assets/images/README_DatastripFolder.png "tree structure example of DATASTRIP folder.")

Finally the GRANULE folder shall contain one folder per granule, each with the GRANULE naming convention, and each containing the GRANULE metadata. This format is compatible with SAFE and others files/folders will be ignored:

![GRANULE Folder](/assets/images/README_GranuleFolder.png "tree structure example of GRANULE folder.")
\[...\]


##### 2.1.1.2 Refining information
 
Refining for Sentinel-2 refers to geolocation refinement that aims to correct the satellite geometric models relative to the [**G**lobal **R**eference **I**mage](https://s2gri.csgroup.space).

Refining information can be found in the Datastrip metadata. If refined, the model of the satellite shall be adjusted accordingly. This information is stored in the field “Level-1B_DataStrip_ID/Image_Data_Info//Geometric_Info/Refined_Corrections_List/Refined_Corrections/MSI_State”. It is present only if the flag “Image_Refining” is set to “REFINED” and absent if set to “NOT_REFINED”.
![Refining information inside Datastrip metadata](/assets/images/README_RefiningInformationInsideDatastripMetadata.png "Refining information inside Datastrip metadata.")

An optional boolean argument is available in the configuration file (see §[2.1 Configuration file](#21-configuration-file)): deactivate_available_refining.
> [!WARNING]
> **By default, it is set at false**, meaning that the refining **information shall be taken into account** if available in the Datastrip Metadata. However, **if set at true**, the datastrip shall be **considered as NOT_REFINED**, meaning ignoring the refining information.
#### 2.1.2 GIPP
GIPP are configuration files used in operation to:
* represent the stable satellite information,
* configure calibration parameters of the satellite,
* configure several algorithms of the processing chain.

By nature, the GIPP are versionnable. It is important to process with the version used to generate the L1B product.
> [!WARNING]
>  **A check is implemented** to verify that the version used is the same than the one listed in the Datastrip metadata (**check on the name**). This check can be desactivated through "gipp_check" parameter of the configuration file (cf §[2.1 Configuration file](#21-configuration-file)). **This parameter is optional, and by default, its value is set to true.**.

The versions of the GIPP used in operation are listed in the L1B Datastrip Metadata of the L1B product (see §[2.1.1 L1B Product](#211-l1b-product)), in the tag _Level-1B_DataStrip_ID/Auxiliary_Data_Info/GIPP_LIST_, as illustrated below:

![GIPP list in L1B Datastrip metadata](/assets/images/README_GIPPListInL1BDatastripMetadata.png "GIPP list in L1B Datastrip metadata.")

> [!CAUTION]
> Please note that the GIPP are not directly available in L1B products; they must be downloaded beforehand by the user at <mark>**XXX**</mark>.

The GIPP required are the following ones:
* **GIP_VIEDIR**: contains Viewing Direction required by Rugged to create viewing model based on TAN_PSI_X/Y_LIST tags. There is one GIP_VIEDIR file **per band** and each file contains information per **detector** (in the following tags: _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_X_LIST]_ and _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_Y_LIST]_)
* **GIP_SPAMOD**: contains transformations to apply to viewing direction from tags, available in the _[DATA]_ field:
    * PILOTING_TO_MSI,
    * MSI_TO_FOCAL_PLANE,
    * FOCAL_PLANE_TO_DETECTOR XML, available **per detector**
* **GIP_BLINDP**: contains information on blind pixel, contained in BLIND_PIXEL_NUMBER tag: _[DATA/BAND/BLIND_PIXEL_NUMBER]_, available **per band**

#### 2.1.3 Altitude
The main purpose of the tool is to add geolocation to the L1B product images. Hence to be precise, the altitude shall be taken into account, as its importance is far from neglectable on final geolocation, as illustrated below:

![Altitude importance on geolocation](/assets/images/README_AltitudeImportanceOnGeolocation.png "Altitude importance on geolocation.")

For this, as Sen2VM uses SXGEO (OREKIT/RUGGED), a GEOID and a DEM shall be used. So, path of both shall be provided in input (cf §[2.1 Configuration file](#21-configuration-file)).

##### 2.1.3.1 DEM
Access to the DEM is provided via a path to a folder containing the dataset. 
The DEM must meet the following requirements:
 * it should be split into files or folders (dynamically read) per square degrees,
 * each DEM file (per square degree) shall be readable by gdal.  

 Examples of DEM structures can be found in [/src/test/resources/DEM/](/src/test/resources/DEM)

##### 2.1.3.2 GEOID
The GEOID shall be readable by gdal. One example of GEOID is available at [S2__OPER_DEM_GEOIDF_MPC__20200112T130120_S20190507T000000.gtx](/src/test/resources/DEM_GEOID/S2__OPER_DEM_GEOIDF_MPC__20200112T130120_S20190507T000000.gtx)


#### 2.1.4 IERS
The IERS represents the ["International Earth Rotation and Reference Systems"](https://www.iers.org/IERS/EN/Home/home_node.html). It is important to have a valid and precise one to get a precise geolocation. During operational processing of the Sentinel-2 data, IERS information are integrated in the L1B metadata datastrip, hence IERS information are available in the L1B product used in input, in the field _Level-1B_DataStrip_ID/Auxiliary_Data_Info/IERS_Bulletin_ as illustrated below:

![IERS information in L1B Datastrip metadata](/assets/images/README_IERSInformationInL1BDatastripMetadata.png "IERS information in L1B Datastrip metadata.")


IERS bulletins are used seamlessly with OREKIT, leading in very precise EOP (Earth Orientation Parameters) handling. However, the IERS available in the L1B Datastrip metadata is not in the right format, hence EOP entries are initialized directly with the information, as “custom EOP”, skipping the IERS bulletins reader parts.

However, a very slight (neglectable) difference will be observed in geolocation compared to operational processing which uses the full IERS bulletin for its ortho-rectification process.

Hence an IERS bulletin can be provided in input by users (as optional).
> [!IMPORTANT]
> **Sen2VM behaviour regarding IERS will then be**:
> * If an IERS bulletin is provided in input:
>    * Verify the date of the IERS bulletin compared to the Datastrip acquisition dates (using _DATASTRIP_SENSING_START_ and _DATASTRIP_SENSING_STOP_ in _“Level-1B_DataStrip_ID/General_Info/Datastrip_Time_Info"_
>    * Go in error if the IERS does not contains the full datastrip duration,
>    * Use the IERS bulletin if it contains the full datastrip duration,
>* If not, use the information available in the L1B Datastrip Metadata.

Format of the IERS bulletin that can be provided is [Bulletin A](https://www.iers.org/IERS/EN/Publications/Bulletins/bulletins.html)

### 2.2 Parameters file
Sen2VM calls SXGEO which is a mono-thread software. **Sen2VM is also designed to be a mono-threaded software.** However, as computations can be long, and because each couple detector/band is an independent mode, user might want to process only parts of the Datastrip per thread. This capability is handled by SXGEO and was propagated to Sen2VM. This way, users can parallelize the process by itself, outside of Sen2VM.

The selection of detectors and bands to process is defined in a JSON parameters file. **If this file is not provided, Sen2VM will process all detectors/bands**.

The parameters file contains 2 fields:
* “detectors”: detectors are passed through string representing Sentinel-2 detectors encoded in **2 digits**, **separated by “-”.** Detectors indexes are from **“01” to “12”.**
* “bands”: bands are passed through string representing Sentinel-2 bands encoded in **2 digits with a “B”** before and **separated from “-”**. Bands are going from **“B01” to “B12”, including a “B8A”.**


![Parameters file example](/assets/images/README_ParametersFileExample.png "Parameters file example.")

If a field (“detectors” or “bands”) **is missing in** the params.json file, **all items of the missing field** will be processed. For example, if “bands” if absent from the previous example in figure above, Sen2VM will process all bands of detectors 1, 3 and 4.


> [!TIP]
> It is to be noted that a small optimization in SXGEO is done not to reload DEM tiles when processing bands of the same resolution for the same detector.

## 3. Outputs
Output can be direct location grids or inverse location grids. Their computation is parametrized by:
* Detector/bands,
* Grid step,
* Computation options (refining for example).

Direct and inverse location grids are different subject. Only the direct location grids will be included in the input product and handled by gdal. Inverse location grids, as they represent an area on the ground will be exported outside the product, in a folder chosen by the user (which can be inside the input product if wanted).

### 3.1 Direct location grids
A direct location grid is a grid which maps sensor coordinates with ground ones in WGS84 coordinates (EPSG:4326). Direct location grid is regular in sensor reference frame (for one couple band/detector).

Sen2VM direct location grid computation takes as input product and auxiliary information (see [L1B product](#211-l1b-product), [GIPP](#212-gipp), [Altitude](#123-altitude), [IERS](#214-iers)) and grid parametrization:
* Band/detector to process ([Parameters File](#22-parameters-file)),
* 3 steps, one per band resolution (10m, 20m, 60m) in pixels (floating number) [Configuration File](#21-configuration-file).

As output:
* At granule level: geolocation grids will be written (per granules/bands).
* At datastrip level: several .vrt (virtual dataset) will be written (per detectors/bands).

#### 3.1.1 Direct locations grids' outputs
Output grids will be integrated directly in the input product.
>  [!CAUTION]
> Hence writing rights in the input L1B folder will be **mandatory**.

Before processing, **a check will be done** to see if direct location grids are already available in the input L1B product folder, for the detectors/bands selected. If at least one is present for one couple detector/band, Sen2VM **will raise an error and stop**. Both granules and datastrip folder will be checked (see output grids format and location in the rest of this section).

##### 3.1.1.1 Granule level
Grids’ location and naming is at granules level:
* 1 grid per couple “L1B granule”/”Sentinel-2 band”,
* Grids **include 2 (_optionally 3_) bands (Long/Lat/_alt_)**
* Grids are in **geotiff format with float32 coding positions** which allow approximately centimetre precision on lat/lon coordinates. JP2000 cannot be used at it does not allow float32 encoding, meaning the precision will not be enough,
* Grids location will be inside a **GEO_DATA folder** which will be inside each granules folders (at the same level than IMG_DATA and QI_DATA folders),
* Grids naming conventions will respect the corresponding image data inside the IMG_DATA folder with:
    * GEO instead of MSI
    * .tif instead of .jp2 extension as jp2 encoding is not possible for float32 data.

As example, for an image of the IMG_DATA folder, named:
* S2B_OPER_**MSI**_L1B_GR_DPRM_20140630T140000_S20230428T151505_D02_B01.<strong>jp2</strong>

The direct location grid will be generated in the GEO_DATA folder, and named:
* S2B_OPER_**GEO**_L1B_GR_DPRM_20140630T140000_S20230428T151505_D02_B01.<strong>tif</strong>

##### 3.1.1.2 Datastrip level
At datastrip level grids’ location and naming is:
* 1 vrt per couple “detector”/”Sentinel-2 band”,
* Grids **include 2 (_optionally 3_) bands (Long/Lat/_alt_)**
* Grid location will be inside a **GEO_DATA folder** inside **DATASTRIP** folder (at the same level QI_DATA folder)
* Grids naming conventions will respect the corresponding datastrip metadata convention with:
    * **GEO** instead of **MTD**
    * <strong>_DXX_BYY.vrt</strong> instead of <strong>.xml</strong> extension.

As example, for an datastrip metadata of the DATASTRIP folder, named:
* S2B_OPER_**MTD**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>.xml</strong>

A folder named GEO_DATA, beside the QI_DATA folder and datastrip metadata will contain 156 vrt files (12detectorsx13bands) named:
* S2B_OPER_**GEO**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>_DXX_BYY.vrt</strong>

Example of product with grid inside it:

![Output example of Datastrip vrt for direct location grids](/assets/images/README_OutputDatastrip.PNG "Output example of Datastrip vrt for direct location grids.")

#### 3.1.2 Direct location grids’ specifications
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
 
 

#### 3.1.3 Grid handling
Direct location grids are intended to be used with bilinear interpolation operation. Direct location (i.e lon/lat positions) should be as follow:

 * Given an image position (_row_/_col_) compute grid fractional position (grid row, grid col):

   ![Direct grid handling](/assets/images/README_DirectGridHandling.png "Direct Grid Handling.")

 * Use bilinear interpolation on (_grid row_, _grid col_) to retrieve lon/lat/(alt).

> [!CAUTION]
> Beware of convention [1,1] is the first pixel of the grid

> [!TIP]
> If user wants to perform direct location of a position outside the granule footprint, a bilinear extrapolation is possible.

#### 3.1.4 Degraded cases
Grids should at least have 2x2 cells

### 3.2 Inverse location grids
An inverse location grid is a grid which maps ground coordinates with sensor ones.
Inverse location grids are georeferenced in **geographic or cartographic reference frame**.
Inverse location grid is regular in **ground reference frame** (for one band and one detector).

To define the extend of the inverse location grid, parameters are described in section §[2.1 Configuration file](#21-configuration-file), but it can be resumed at:
* A referential system,
* A square defined by:
    * One Upper Left point (UL),
    * One Lower Right point (LR),
* 3 steps, one per band resolution (10m, 20m, 60m) in the referential metrics,
* An output folder.

As output, a geolocation grid will be created **for each band and detector** to process.
#### 3.2.1 Inverse location grids’ outputs
Outputs will be **written in the folder provided by the user**. For inverse location grids, granule level is not foreseen, since granule footprint can result in large margins in projected data.

Output grids’ convention will be:
* 13 grids (1 per Sentinel-2 band) per detector intersecting the area,
* Grids are in **geotiff format with float32 coding positions** which allow decimal information on **col/row** positions. JP2000 cannot be used at it does not allow float32 encoding, meaning the precision will not be enough,
* Grids naming conventions will respect the corresponding datastrip metadata convention with:
    * **INV** instead of **MTD**,
    * <strong>_DXX_BYY.tif</strong> instead of <strong>.xml</strong> extension.

As example, for a Datastrip metadata of the DATASTRIP folder:
* S2B_OPER_**MTD**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>.xml</strong>

Output will be named:
* S2B_OPER_**INV**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>_DXX_BYY.tif</strong>

Example:

![Output example of inverse location grids](/assets/images/README_OutputInv.PNG "Output example of inverse location grids.")


#### 3.2.2 Inverse location grids’ specifications
Inverse location grids **will give the coordinates in the “detector” image**, meaning as if the granules of the same detector were concatenated.

 * Grid metadata:
    * are written as GDAL GEO information (SRS and geotransform) and Metadata keys, containing ROW_BAND and COL_BAND to specify row col index and PIXEL_ORIGIN which specify granule first pixel center convention, and NoData value.  
   * Metadata keys are:
       * the SRS
       * NoData fill value
       * the grid footprint
       * the size of the grid,
       * the step of the grid,
       * margins,
       * the GIPP dataset used,
       * the DEM and associated geoid (TBD),
       * Was refining deactivated

as for Direct location, the first grid center if syncrhonized with the first center of theoutput boundinig box considering a pixel at resolution of the processed band. THe center of the last pixel will be computed to cover at least the last pixel of the Bounding box provided in input:

![Inverse Convention](/assets/images/README_InverseConvention.png "Inverse Convention.")
 


#### 3.2.3 Grid handling

Grids are intended to be used with bilinear interpolation operation. Inverse locations (i.e image position given a ground position) should be as follow:

 * Given an ground position (_lon_/_lat_) or (_x_, _y_) compute grid fractional position (grid row, grid col)using grid geotransform for GRID_ORIGIN and GRID_STEP:

     ![Inverse grid handling](/assets/images/README_InverseGridHandling.png "Inverse Grid Handling.")

 * Use bilinear interpolation on (_grid row_, _grid col_) to retrieve _row_/_col_.
 
> [!CAUTION]
> Beware of convention [PIXEL ORIGIN,PIXEL ORIGIN] is the first pixel of the grid..



#### 3.2.4 Degraded cases
Grids should at least have 2x2 cells.

## 4. Validation

In a nutshell, validation is split into 2 main parts:
 * **Some functional tests**:
     * Integrated into the CI/CD process:
     * Validate that Sen2VM takes into account the different input provided, and that output grids are consistent with reference ones,
 * Some quality tests:
     * Launched manually
     * Proving, using several TDS, that grids generated by Sen2VM are consistent with the ones generated with the legacy,
     * Prove that those grids can be used to reach equivalent quality at L1C,
     * these processings will also be used to assess the processing performances,

Tests are more detailed in:
 * In [/src/test/java/esa/sen2vm/](/src/test/java/esa/sen2vm/) for fucntionnal tests,
 * In dediceted <mark>**Document**</mark>, for quality tests (which also includes functionnal tests description)

