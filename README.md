# Project sen2vm-core
Sen2VM standalone tool that will also be called by Sen2VM SNAP Plugin

This documentation is split into 3 parts:
* Expected inputs description and format + how to run Sen2VM: §[Inputs](#1-inputs)
* Outputs description: §[Outputs](#2-outputs)
* Validation description (tests and test data used): §[Validation](#3-validation)

## 1. Inputs
Inputs required by Sen2VM are:
* Parts of the L1B Product,
* Some GIPP files (parameters file used in operational production),
* DEM,
* GEOID,
* IERS,
* Attitude data,
* Additional information for configuration.

All inputs are described in this section.

It starts by the command line which will be the entry point for user or for the Sen2VM SNAP plugin. Then a description of all what can be passed from the user to Sen2VM (through command line) is described.

### 1.1 How to run sen2vm-core

Inside `sen2vm-core` folder, run the next commands :
```
mvn clean install
java -jar target/sen2vm-core-0.0.1-SNAPSHOT.jar -c [configuration_filepath] [-p [parameters_filepath]]
```

Where:
* <NN.NN.NN> is the version number of Sen2VM launched,
* configuration_filepath: **Mandatory**. configuration file containing all inputs related to product or grids that are required by Sen2VM (see §[1.2 Configuration file](#12-configuration-file) for further information),
* parameters_filepath: **Optional**. File to configure the detectors/bands to process. If not available, all detectors/bands will be processed (see §XXX for further information).

Example from current repository:
```
java -jar target/sen2vm-core-0.0.1-SNAPSHOT.jar -c src/test/resources/configuration_example.json -p src/test/resources/params.json
```

### 1.2 Configuration file
The configuration file will contain all information about the product, the aux data and the operation to be performed.
Its format is  [JSON format](https://en.wikipedia.org/wiki/JSON)
An example is available at: https://github.com/sen2vm/sen2vm-core/blob/main/src/test/resources/configuration_example.json

![Configuration file example](/assets/images/README_ConfigurationFileExample.png "Configuration file example.")

Each parameter description can be found in the table below. More information about them are available in the hereafter subsections, quoted in the table.

| Name        | Type     | Required      | Description   |
| ----------- | :------: | :-----------: | :-----------: |
| l1b_product | string   | **Mandatory** | Path to L1B_PRODUCT folder, where L1B_Product format is in SAFE format (described in §<mark>**XXX**</mark>, including DATASTRIP + GRANULE)|
| gipp_folder | string   | **Mandatory** | Path to a folder containing at least the 3 types of GIPP required by Sen2VM (other will be ignored). For more information, refer to §<mark>**XXX**</mark>.|
| gipp_check  | boolean  | Optional      | If true(default), check of GIPP version activated (see GIPP section §<mark>**XXX**</mark>)|
|dem          | string   | **Mandatory** | Path to the FOLDER containing a DEM in the right format (cf <mark>**XXX**</mark>)|
|geoid        | string   | **Mandatory** | Path to the FOLDER containing a GEOID in the right format (cf <mark>**XXX**</mark>)|
| <mark>pod</mark>     |<mark>string</mark>| <mark>Optional</mark>  |<mark>Path to the POD FILE in the right format (cf <mark>**XXX**</mark>)</mark>|
| iers        | string   | Optional      | Path to the IERS folder containing the IERS file in the right format (cf <mark>**XXX**</mark>)|
|operation    | string   | **Mandatory** | Possibilities:<ul><li>“direct”: to configure Sen2VM to compute direct location grids</li><li>“inverse”: to configure Sen2VM to compute inverse location grids</li></ul>|
| deactivate_available_refining| boolean  | Optional      | If false (default), refining information (if available in Datastrip Metadata) are used to correct the model before geolocation, cf product description in §<mark>**XXX**</mark>|
| steps       | float    | **Mandatory** | <ul><li>One per resolution: “10m_bands”, “20m_bands” & “60m_bands”</li><li>Accept only floating numbers (NNNN.DDD)</li><li>Unit in pixel</li></ul>|
| inverse_location_additional_info | | **Mandatory if “inverse”, else useless.**| See dedicated table below|


The field “inverse_location_additional_info” is not required and will be ignored if direct location grids are asked. However, it is mandatory for inverse location grids generation and **Sen2VM will raise an error** if information is missing in it.

| Name          | Type     | Required      | Description   |
| ------------- | :------: | :-----------: | :-----------: |
| ul_x          | float    | **Mandatory** | **X** coordinates of the upper left point that will define the squared area where to compute the grids|
| ul_y          | float    | **Mandatory** | **Y** coordinates of the **upper left** point that will define the squared area where to compute the grids|
| lr_x          | float    | **Mandatory** | **X** coordinates of the **lower right** point that will define the squared area where to compute the grids|
| lr_y          | float    | **Mandatory** | **Y** coordinates of the **lower right** point that will define the squared area where to compute the grids|
| referential   | string   | **Mandatory** | A string defining the **referential** used. **Note, UL and LR points’ coordinates** will be in this referential. Example: EPSG:4326|
| output_folder | string   | **Mandatory**|Output path where the inverse location grids will be written (see §<mark>**XXX**</mark>)|

#### 1.2.1 L1B Product
> [!NOTE]
> L1B products can be downloaded at <mark>**XXX**</mark>

> [!IMPORTANT]
> The expected format is the SAFE format, i.e. a folder structured as illustrated in <mark>**TODO**</mark>

Refining information can be found in the datastrip Metadata. If refined, the model of the satellite shall be modified accordingly. Information is stored in the filed “Level-1B_DataStrip_ID/Image_Data_Info//Geometric_Info/Refined_Corrections_List/Refined_Corrections/MSI_State”. They are present only if the flag “Image_Refining” is set at “REFINED” and absent if set at “NOT_REFINED”.
![Refining information inside Datastrip metadata](/assets/images/README_RefiningInformationInsideDatastripMetadata.png "Refining information inside Datastrip metadata.")

An optional boolean argument is available in the configuration file (see §[1.2 Configuration file](#1.2-configuration-file)): deactivate_available_refining.
> [!WARNING]
> **By default, it is set at false**, meaning that the refining **information shall be taken into account** if available in the Datastrip Metadata. However, **if set at true**, the datastrip shall be **considered as NOT_REFINED**, meaning ignoring the refining information.
#### 1.2.2 GIPP
GIPP are configuration files used in operation to:
* represent the stable satellite information,
* configure calibration parameters of the satellite,
* configure the several algorithms of the processing chain.

By nature, GIPP are then versionnable. It is important to process with the version used to generate the L1B product.
> [!WARNING]
>  **A check is implemented** to verify that the version used is the same than the one listed in the Datastrip metadata (**check on the name**). This check can be deactivated through "gipp_check" parameter of the configuration file (cf <mark>**XXX**</mark>). **This parameter is optional, and by default, its value is at true and the check is done, it can be forced at false if needed**.

The version used in operation of the GIPP are listed in the L1B Datastrip Metadata of the L1B product (see §[1.2.2 L1B Product](#121-l1b-product)), in the following section: _Level-1B_DataStrip_ID/Auxiliary_Data_Info/GIPP_LIST_, as illustrated below:

![GIPP list in L1B Datastrip metadata](/assets/images/README_GIPPListInL1BDatastripMetadata.png "GIPP list in L1B Datastrip metadata.")

> [!CAUTION]
> GIPP are however not directly available in the L1B product, then shall then be downloaded beforehand by the user. All versions are available at <mark>**XXX**</mark>.

The GIPP required are the following ones:
* **GIP_VIEDIR**: contains Viewing Direction required by Rugged to create viewing model from TAN_PSI_X/Y_LIST tags, one GIP_VIEDIR file **per band**, each file containing information per **detector!** (in _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_X_LIST]_ and _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_Y_LIST]_
* **GIP_SPAMOD**: contains transformations to apply to viewing direction from tags, available in the _[DATA]_ field:
    * PILOTING_TO_MSI,
    * MSI_TO_FOCAL_PLANE,
    * FOCAL_PLANE_TO_DETECTOR XML, available **per detector**!
* **GIP_BLINDP**: contains information on blind pixel, contained in BLIND_PIXEL_NUMBER tag: _[DATA/BAND/BLIND_PIXEL_NUMBER]_, available **per band**!

#### 1.2.3 Altitude
The main purpose of the tool is to add geolocation to the L1B product images. Hence to be precise, the altitude shall be taken into account, as its importance is far from neglectable on final geolocation, as illustrated below:

![Altitude importance on geolocation](/assets/images/README_AltitudeImportanceOnGeolocation.png "Altitude importance on geolocation.")

<mark>**TODO**</mark>
#### 1.2.4 IERS
The IERS represents the ["International Earth Rotation and Reference Systems"](https://www.iers.org/IERS/EN/Home/home_node.html). It is important to have a valid and precise one to have a precise geolocation. During operation processing, IERS information are integrated in the L1B metadata datastrip, hence IERS information are available in the L1B product used in input, in the field _Level-1B_DataStrip_ID/Auxiliary_Data_Info/IERS_Bulletin_ as illustrated below:

![IERS information in L1B Datastrip metadata](/assets/images/README_IERSInformationInL1BDatastripMetadata.png "IERS information in L1B Datastrip metadata.")


IERS bulletins are used seamlessly with OREKIT, leading in very precise EOPhandling. However, the IERS available in the L1B Datastrip metadata is not in the right format, hence EOP (Earth Orientation Parameters) entries are initialized directly with the information, as “custom EOP”, skipping the IERS bulletins reader parts.

However, a very slight (neglectable) difference will be observed in geolocation compared to operational processing which uses the full IERS bulletin for its ortho-rectification process.

Hence an IERS bulletin can be provided in input for user (as optional).
> [!IMPORTANT]
> **Sen2VM behaviour regarding IERS will then be**:
> * If an IERS bulletin is provided in input:
>    * Verify the date of the IERS bulletin compared to the Datastrip acquisition dates (using _DATASTRIP_SENSING_START_ and _DATASTRIP_SENSING_STOP_ in _“Level-1B_DataStrip_ID/General_Info/Datastrip_Time_Info"_
>    * Go in error if the IERS does not contains the full datastrip duration,
>    * Use the IERS bulletin if it contains the full datastrip duration,
>* If not, use the information available in the L1B Datastrip Metadata.

<mark>**TODO illustration "IERS information in L1B Datastrip Medata**</mark>

Format of the IERS bulletin that can be provided is [Bulletin A](https://www.iers.org/IERS/EN/Publications/Bulletins/bulletins.html) 

#### 1.2.5 <mark>POD</mark>
<mark>**DESCOPED**</mark>

### 1.3 Parameters file
Sen2VM calls SXGEO which is a mono-thread software. **Sen2VM is also designed to be a mono-threaded software.** However, as computations can be long, and because each couple detector/band is an independent mode, user might want to process only parts of the Datastrip per thread. This is handled by SXGEO and was propagated to Sen2VM. Configuration of the detectors/bands to process are done through the parameters file which is a json. **If not available, Sen2VM will process all detectors/bands**.

The configuration file contains 2 fields:
* “detectors”: Detectors are passed through string representing Sentinel-2 detectors encoded in **2 digits**, **separated by “-”.** Detectors indexes are from **“01” to “12”.**
* “bands”: Bands are passed through string representing Sentinel-2 bands encoded in **2 digits with a “B”** before and **separated from “-”**. Bands are going from **“B01” to “B12”, including a “B8A”.**

<mark>**TODO Link Example**</mark>

![Parameters file example](/assets/images/README_ParametersFileExample.png "Parameters file example.")

If a field (“detectors” or “bands”) **is missing in** the params.json file, **all items of the missing field** will be processed. For example, if “bands” if absent from the previous example in figure above, Sen2VM will process all bands of detectors 1, 3 and 4.


> [!TIP]
> It is to be noted that a small optimization in SXGEO is done not to reload DEM tiles when processing bands of the same resolution for the same detector.

## 2. Outputs
Output can be direct location grids or inverse location grids. Their computation is parametrized by:
* Detector/bands,
* Grid step,
* Computation options (refining for example).

Direct and inverse location grids are different subject. Only the direct location grids will be included in the input product and handled by gdal. Inverse location grids, as they represent an area on the ground will be exported outside the product, in a folder chosen by the user (which can be inside the input product if wanted).

### 2.1 Direct location grids
A direct location grid is a grid which maps sensor coordinates with ground ones in WGS84 coordinates (EPSG:4326). Direct location grid is regular in sensor reference frame (for one band and one detector).

Sen2VM direct location grid computation takes as input product and auxiliary information (see <mark>**TODO**</mark>) and grid parametrization:
* Band/detector to process,
* 3 steps, one per band resolution (10m, 20m, 60m) in pixels (floating number).

As output:
* At granule level: geolocation grids will be written (per granules/bands).
* At datastrip level: several .vrt (virtual dataset) will be written (per detectors/bands).

#### 2.1.1 Direct locations grids' outputs
Output grids will be integrated directly in the input product.
>  [!CAUTION]
> Hence writing rights in the input L1B folder will be **mandatory**.

Before processing, **a check will be done** to see if direct location grids are already available in the input L1B product folder, for the detectors/bands selected. If at least one is present for one couple detector/band, Sen2VM **will raise an error and stop**. Both granules and datastrip folder will be checked (see output grids format and location in the rest of this section).

##### 2.1.1.1 Granule level
Grids’ location and naming is at granules level:
* 1 grid per couple “L1B granule”/”Sentinel-2 band”,
* Grids **include 2 (_optionally 3_) bands (Lat/Long/_alt_)**
* Grids are in **geotiff format with float32 coding positions** which allow approximately centimetre precision on lat/lon coordinates. JP2000 cannot be used at it does not allow float32 encoding, meaning the precision will not be enough,
* Grids location will be inside a **GEO_DATA folder** which will be inside each granules folders (at the same level than IMG_DATA and QI_DATA folders),
* Grids naming conventions will respect the corresponding image data inside the IMG_DATA folder with:
    * GEO instead of MSI
    * .tif instead of .jp2 extension as jp2 encoding is not possible for float32 data.

As example, for an image of the IMG_DATA folder, named:
* S2B_OPER_**MSI**_L1B_GR_DPRM_20140630T140000_S20230428T151505_D02_B01.<strong>jp2</strong>

The direct location grid will be generated in the GEO_DATA folder, and named:
* S2B_OPER_**GEO**_L1B_GR_DPRM_20140630T140000_S20230428T151505_D02_B01.<strong>tif</strong>

##### 2.1.1.2 Datastrip level
At datastrip level grids’ location and naming is:
* 1 vrt per couple “detector”/”Sentinel-2 band”,
* Grids **include 2 (_optionally 3_) bands (Lat/Long/_alt_)**
* Grid location will be inside a **GEO_DATA folder** inside **DATASTRIP** folder (at the same level QI_DATA folder)
* Grids naming conventions will respect the corresponding datastrip metadata convention with:
    * **GEO** instead of **MTD**
    * <strong>_DXX_BYY.vrt</strong> instead of <strong>.xml</strong> extension.

As example, for an datastrip metadata of the DATASTRIP folder, named:
* S2B_OPER_**MTD**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>.xml</strong>

A folder named GEO_DATA, beside the QI_DATA folder and datastrip metadata will contain 156 vrt files (12detectorsx13bands) named:
* S2B_OPER_**GEO**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>_DXX_BYY.vrt</strong>

<mark>**TODO: example of product with grid inside it**</mark>

#### 2.1.2 Direct location grids’ specifications
<mark>**TODO**</mark>

#### 2.1.3 Grid handling
<mark>**TODO**</mark>

#### 2.1.4 Degraded cases
<mark>**TODO**</mark>

### 2.2 Inverse location grids
An inverse location grid is a grid which maps ground coordinates with sensor ones.
Inverse location grids are georeferenced in **geographic or cartographic reference frame**.
Inverse location grid is regular in **ground reference frame** (for one band and one detector).

To define the extend of the inverse location grid, parameters are described in section §<mark>**XXX**</mark>, but it can be resumed at:
* A referential system,
* A square defined by:
    * One Upper Left point (UL),
    * One Lower Right point (LR),
* 3 steps, one per band resolution (10m, 20m, 60m) in the referential metrics,
* An output folder.

As output, a geolocation grid will be created **for each band and detector** to process.
#### 2.2.1 Inverse location grids’ outputs
Outputs will be **written in the folder provided by the user**. For inverse location grids, granule level is not foreseen, since granule footprint can result in large margins in projected data.

Output grids’ convention will be:
* 13 grids (1 per Sentinel-2 band) per detector intersecting the area,
* Grids are in **geotiff format with float32 coding positions** which allow decimal information on row/col positions. JP2000 cannot be used at it does not allow float32 encoding, meaning the precision will not be enough,
* Grids naming conventions will respect the corresponding datastrip metadata convention with:
    * **INV** instead of **MTD**,
    * <strong>_DXX_BYY.tif</strong> instead of <strong>.xml</strong> extension.

As example, for a Datastrip metadata of the DATASTRIP folder:
* S2B_OPER_**MTD**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>.xml</strong>

Output will be named:
* S2B_OPER_**INV**_L1B_DS_DPRM_20140630T140000_S20230428T150801<strong>_DXX_BYY.tif</strong>

<mark>**TODO: Graph to illustrate.**</mark>


#### 2.2.2 Inverse location grids’ specifications
<mark>**TODO**</mark>

#### 2.2.3 Grid handling
<mark>**TODO**</mark>

#### 2.2.4 Degraded cases
<mark>**TODO**</mark>

## 3. Validation
<mark>**TODO Intro**</mark>
### 3.1 Test Data
<mark>**TODO**</mark>

### 3.2 Tests description
<mark>**TODO**</mark>

<mark>List demo:</mark>
- [x] Test done
- [ ] Test Not Done
- [ ] Final Issue :tada:


