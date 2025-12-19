[README](../../README.md)

* [HOWTO](../Usage/HOWTO.md)
* [Inputs description](../Input/input_description.md)

  * [How to Download L1B Data from CDSE](../Input/L1B_CDSE_Download.md)
  * [How to Download DEM Data from CDSE](../Input/DEM_CDSE_Download.md)

* Outputs description:

  * [Direct location grids](../Output/output_direct_loc.md)
  * [Inverse location grids](../Output/output_inverse_loc.md)
  * [Output grids usage](../Output/output_grids_usage.md)

# Inputs

**Inputs required by Sen2VM are:**

* L1B Product, accessible through **[Copernicus Data Space Ecosystem](https://dataspace.copernicus.eu/)**. *Please note that special access for L1B products might be required by submitting  a request via the [FAQ section](https://documentation.dataspace.copernicus.eu/FAQ.html).* For detailed download instructions, see **[How to Download L1B Data from CDSE](L1B_CDSE_Download.md)**.
* Some GIPP files *(parameters files used in operational production, defining Satellites)*, accessible through **[sen2vm-gipp-database](https://github.com/sen2vm/sen2vm-gipp-database)**
* Digital Elevation Model (DEM) (COPERNICUS DEM is accessible through **[Copernicus Data Space Ecosystem](https://dataspace.copernicus.eu/)**). For detailed download instructions, see **[How to Download DEM Data from CDSE](DEM_CDSE_Download.md)**.
* GEOID model to measure precise surface elevations, **it shall be the one used to generate the DEM you are providing**, an example can accessible through **[sen2vm-core git](../../src/test/resources/DEM_GEOID/)**
* IERS bulletin that provides data and standards related to Earth rotation and reference frames,  accessible through **[Bulletin A](https://www.iers.org/IERS/EN/Publications/Bulletins/bulletins.html)**
* Additional information for configuration.

> [!NOTE]
> The orekit-data is required to process the grid. During the first run, the orekit-data is extracted from the JAR file (.jar) and placed in the same directory as the JAR. The orekit-data can be replaced by the user if needed. The official orekit-data is available https://github.com/sen2vm/sen2vm-core/tree/main/orekit-data.

Please note that <mark>[Notebooks](TODO)</mark> are available to ease configuration and usage.


## 1. Configuration

The configuration is related to all information about:

* the product, and the auxiliarry data, 
* the operations to be performed.

Each parameter description can be found in the table below:

| Name | Type | Required |                                                                                                             Description                                                                                                                                           |
| ----------- | :------: | :-----------: |:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| l1b_product | string   | **Mandatory** |                                                                     Path to L1B_PRODUCT folder, where L1B_Product format is in SAFE format (described in §[L1B Product](#11-l1b-product), including DATASTRIP + GRANULE)                                                                      |
| gipp_folder | string   | **Mandatory** |                                                                    Path to a folder containing at least the 3 types of GIPP required by Sen2VM (other will be ignored). For more information, refer to §[GIPP](#12-gipp).                                                                     |
| auto_gipp_selection  | boolean  | Optional      |                                                                                                If true (default), the GIPPs are selected automaticaly from the datastrip metadata and an untar extraction is performed if necessary. Otherwise, the GIPPs must have a single, non-multiple version. This option is mainly intended for experimental use to test custom GIPP values (see GIPP section § [GIPP](#12-gipp)</mark>)      |
| grids_overwriting | boolean  | Optional      |                                                                                             Activate the grids overwritings if the grids have already or partialy computed. It is false by default.                                                                                             |
|dem          | string   | **Mandatory** |                                                                                                    Path to the FOLDER containing a DEM in the right format (cf §[Altitude/DEM](#131-dem)).                                                                                                    |
|geoid        | string   | **Mandatory** |                                                                                                  Path to the FILE containing a GEOID in the right format (cf §[Altitude/GEOID](#132-geoid))                                                                                                   |
| iers        | string   | Optional      |                                                                                                  Path to the IERS folder containing the IERS file in the right format (cf §[IERS](#14-iers))                                                                                                  |
| operation    | string   | **Mandatory** |                                                    In term of operation you can select the following Sen2VM configurations:<ul><li>“direct”: to compute direct location grids</li><li>“inverse”: to compute inverse location grids</li></ul>                                                    |
| deactivate_available_refining| boolean  | Optional      |                                          If set to false (default), refining information (if available in Datastrip Metadata) are used to correct the model before geolocation, cf. product description in §[L1B Product](#112-refining-information)                                          |
| export_alt   | boolean  | Optional      |  If set to false (default), direct location grids will include only two bands: **Long/Lat**. If set to true, a third band representing the **Altitude** will also be exported, increasing the output grid size. See product description in §[Direct location grids](../Output/output_direct_loc.md)  |
| steps       | double    | **Mandatory** | The step is mandatory and must be specified  as one per resolution: “10m_bands”, “20m_bands” & “60m_bands””. Please note that only floating numbers in the format NNNN.DDD are accepted and that the unit is given in pixel for direct location and in metrics of referential system for inverse location. |
| inverse_location_additional_info | | **Mandatory if “inverse”, else useless.**|                                                                                                    For the inverse location additional information please refer to the dedicated table below       |


The field “inverse_location_additional_info” is not required and will be ignored if direct location grids are asked. However, it is mandatory for inverse location grids generation and **Sen2VM will raise an error** if this information is missing.

| Name          | Type     | Required      |                                                                 Description                                                                  |
| ------------- | :------: | :-----------: |:--------------------------------------------------------------------------------------------------------------------------------------------:|
| ul_x          | double    | **Mandatory** |                        **X** coordinates of the **upper left** point defining the area of desired resampled product.                         |
| ul_y          | double    | **Mandatory** |                 **Y** coordinates of the **upper left** point defining the area of desired resampled product.                                |
| lr_x          | double    | **Mandatory** |                        **X** coordinates of the **lower right** point defining the area of desired resampled product.                        |
| lr_y          | double    | **Mandatory** |                        **Y** coordinates of the **lower right** pointdefining the area of desired resampled product.                         |
| referential   | string   | **Mandatory** | A string defining the **referential** used. **Please note, ul and lr points’ coordinates** are given in this referential. Example: EPSG:4326 |
| output_folder | string   | **Mandatory**|                Path where the inverse location grids are written (see §[Inverse location grids](../Output/output_inverse_loc.md))                |

> [!NOTE]
> Inverse location grids footprint will enclose desired product footprint [ul_x, ul_y, lr_x, lr_y]. 

Those parameters can be sent to Sen2 VM:

* either by setting each argument in a command line see [HOWTO](../Usage/HOWTO.md)
* either using an input configuration file in  [JSON format](https://en.wikipedia.org/wiki/JSON). An example of configuration file is available at: https://github.com/sen2vm/sen2vm-core/blob/main/src/test/resources/configuration_example.json :

![Configuration file example](/assets/images/README_ConfigurationFileExample.png "Configuration file example.")

### 1.1 L1B Product

> [!NOTE]
> L1B products can be downloaded at [https://browser.dataspace.copernicus.eu/](https://browser.dataspace.copernicus.eu/). Please note that special access for L1B products might be required by submitting  a request via the FAQ section. For detailed step-by-step instructions on how to download L1B data from the Copernicus Data Space Browser, please refer to the [How to Download L1B Data from CDSE](L1B_CDSE_Download.md) guide.

> [!IMPORTANT]
> The expected format is compatible with the SAFE format, i.e. a folder structured as illustrated in the following sections.

#### 1.1.1 L1B Product input tree structure

The mandatory inputs for Sen2VM are the Datastrip and the Granules metadata. 

These metadata must be organized in a specific directory structure. Within the L1B folder, only the following subdirectories will be considered:

 * DATASTRIP
 * GRANULE

![L1B Folder](/assets/images/README_L1BProduct.png "Necessary folders.")

Additional files (as in the SAFE format) may be present in the folders, but they will simply be ignored by Sen2VM core.

As for the DATASTRIP folder, it must contain a subfolder named after the Datastrip reference and inside this subfolder, the Datastrip Metadata (also named after the Datastrip reference). This structure is compatible with the SAFE format and others files/folders will be ignored:

![DATASTRIP Folder](/assets/images/README_DatastripFolder.png "tree structure example of DATASTRIP folder.")

Finally the GRANULE folder shall contain one folder per granule, each with the GRANULE naming convention, and each containing the GRANULE metadata. This format is compatible with SAFE and others files/folders will be ignored:

![GRANULE Folder](/assets/images/README_GranuleFolder.png "tree structure example of GRANULE folder.")
\[...\]


#### 1.1.2 Refining information
 
Refining for Sentinel-2 refers to geolocation refinement that aims at correcting the satellite geometric models relative to the [**G**lobal **R**eference **I**mage](https://s2gri.csgroup.space).

Refining information can be found in the Datastrip metadata. If refined, the model of the satellite shall be adjusted accordingly. This information is stored in the field “Level-1B_DataStrip_ID/Image_Data_Info//Geometric_Info/Refined_Corrections_List/Refined_Corrections/MSI_State”. It is present only if the flag “Image_Refining” is set to “REFINED” and absent if set to “NOT_REFINED”.
![Refining information inside Datastrip metadata](/assets/images/README_RefiningInformationInsideDatastripMetadata.png "Refining information inside Datastrip metadata.")

An optional boolean argument is available in the configuration file (see §[2.1 Configuration file](#21-configuration-file)): deactivate_available_refining.
> [!WARNING]
> **By default, it is set at false**, meaning that the refining **information shall be taken into account** if available in the Datastrip Metadata. However, **if set at true**, the Datastrip shall be **considered as NOT_REFINED**, meaning ignoring the refining information.

### 1.2 GIPP

GIPP are configuration files used in operation to:

* represent the stable satellite information,
* configure calibration parameters of the satellite,
* configure several algorithms of the processing chain.

By nature, GIPP are versionnable. It is important to process with the version used to generate the L1B product.
> [!WARNING]
> **A check is implemented** to verify that the version used is the same than the one listed in the Datastrip metadata (**check on the name**). This check can be deactivated through "auto_gipp_selection" parameter. **This parameter is optional, and by default, its value is set to true.**.

The versions of the GIPP used in operation are listed in the L1B Datastrip Metadata of the L1B product (see §[1.1 L1B Product](#11-l1b-product)), in the tag _Level-1B_DataStrip_ID/Auxiliary_Data_Info/GIPP_LIST_, as illustrated below:

![GIPP list in L1B Datastrip metadata](/assets/images/README_GIPPListInL1BDatastripMetadata.png "GIPP list in L1B Datastrip metadata.")

> [!CAUTION]
> Please note that the GIPP are not directly available in L1B products; they must be downloaded beforehand by users on  [this dedicated repository](https://github.com/sen2vm/sen2vm-gipp-database).

The auto_gipp_selection option enables the automatic retrieval of GIPP files from the Datastrip GIPP list, and using a GIPP version check function.
The GIPP folder does not require a specific structure; the system searches through all subdirectories and selects the first valid GIPP instance according to the listed name.

If only .tar or .tar.gz archives of the GIPPs are available, the archives are extracted.

The GIPP required are the following ones:
* **GIP_VIEDIR**: contains Viewing Direction required by Rugged to create viewing model based on TAN_PSI_X/Y_LIST tags. There is one GIP_VIEDIR file **per band** and each file contains information per **detector** (in the following tags: _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_X_LIST]_ and _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_Y_LIST]_)
* **GIP_SPAMOD**: contains transformations to apply to viewing direction from tags, available in the _[DATA]_ field:

    * PILOTING_TO_MSI,
    * MSI_TO_FOCAL_PLANE,
    * FOCAL_PLANE_TO_DETECTOR XML, available **per detector**
    
* **GIP_BLINDP**: contains information on blind pixel, contained in BLIND_PIXEL_NUMBER tag: _[DATA/BAND/BLIND_PIXEL_NUMBER]_, available **per band**

### 1.3 Altitude

The main purpose of the tool is to add geolocation to the L1B product images. Hence to be precise, the altitude shall be taken into account, as its importance is far from neglectable on final geolocation, as illustrated below:

![Altitude importance on geolocation](/assets/images/README_AltitudeImportanceOnGeolocation.png "Altitude importance on geolocation.")

For this, as Sen2VM uses SXGEO (OREKIT/RUGGED), a GEOID and a DEM shall be used. So, path of both shall be provided in input (cf §[1 Configuration file](#1-configuration)).

#### 1.3.1 DEM

> [!NOTE]
> DEM can be downloaded at [https://browser.dataspace.copernicus.eu/](https://browser.dataspace.copernicus.eu/). For detailed step-by-step instructions on how to download DEM data from the Copernicus Data Space Browser, please refer to the [How to Download DEM Data from CDSE](DEM_CDSE_Download.md) guide.

Access to the DEM is provided via a path to a folder containing the dataset. 
The DEM must meet the following requirements:
 * it should be split into files or folders (dynamically read) per square degrees,
 * each DEM file (per square degree) shall be readable by gdal.  

 Examples of DEM structures can be found in [/src/test/resources/DEM/](/src/test/resources/DEM)

#### 1.3.2 GEOID

The GEOID shall be readable by gdal. One example of GEOID is available at [S2__OPER_DEM_GEOIDF_MPC__20200112T130120_S20190507T000000.gtx](/src/test/resources/DEM_GEOID/S2__OPER_DEM_GEOIDF_MPC__20200112T130120_S20190507T000000.gtx)


### 1.4 IERS

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

## 2. Parameters file

Sen2VM calls SXGEO which is a mono-thread software. **Sen2VM is also designed to be a mono-threaded software.** However, as computations can be long, and because each couple detector/band is an independent model, user might want to process only parts of the Datastrip per thread. This capability is handled by SXGEO and was propagated to Sen2VM. This way, users can parallelize the process by itself, outside of Sen2VM.

The selection of detectors and bands to process is defined in a JSON parameters file. **If this file is not provided, Sen2VM will process all detectors/bands**.

The parameters file contains 2 fields:
* “detectors”: detectors are passed through string representing Sentinel-2 detectors encoded in **2 digits**, **separated by “-”.** Detectors indexes are from **“01” to “12”.**
* “bands”: bands are passed through string representing Sentinel-2 bands encoded in **2 digits with a “B”** before and **separated from “-”**. Bands are going from **“B01” to “B12”, including a “B8A”.**


![Parameters file example](/assets/images/README_ParametersFileExample.png "Parameters file example.")

If a field (“detectors” or “bands”) **is missing in** the params.json file, **all items of the missing field** will be processed. For example, if “bands” if absent from the previous example in figure above, Sen2VM will process all bands of detectors 1, 3 and 4.


> [!TIP]
> It is to be noted that a small optimization in SXGEO is done not to reload DEM tiles when processing bands of the same resolution for the same detector.

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