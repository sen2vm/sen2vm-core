# Project sen2vm-core
Sen2VM standalone tool that will also be called by Sen2VM SNAP Plugin

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
* configuration_filepath: **Mandatory**. configuration file containing all inputs related to product or grids that are required by Sen2VM (see §[1.2 Configuration file](#1.2-configuration-file) for further information),
* parameters_filepath: **Optional**. File to configure the detectors/bands to process. If not available, all detectors/bands will be processed (see §XXX for further information).

Example from current repository:

```
java -jar target/sen2vm-core-0.0.1-SNAPSHOT.jar -c src/test/resources/configuration_example.json -p src/test/resources/params.json
```
### 1.2 Configuration file
The configuration file will contain all information about the product, the aux data and the operation to be performed.
Its format is  [JSON format](https://en.wikipedia.org/wiki/JSON)
An example is available at: https://github.com/sen2vm/sen2vm-core/blob/main/src/test/resources/configuration_example.json

Each parameter description can be found in the table below. More information about them are available in the hereafter subsections, quoted in the table.

| Name        | Type     | Required      | Description   |
| ----------- | :------: | :-----------: | :-----------: |
| l1b_product | string   | **Mandatory** | Path to L1B_PRODUCT folder, where L1B_Product format is in SAFE format (described in §==**XXX**==, including DATASTRIP + GRANULE)|
| gipp_folder | string   | **Mandatory** | Path to a folder containing at least the 3 types of GIPP required by Sen2VM (other will be ignored). For more information, refer to §==**XXX**==.|
| gipp_check  | boolean  | Optional      | If true(default), check of GIPP version activated (see GIPP section §==**XXX**==)|
|dem          | string   | **Mandatory** | Path to the FOLDER containing a DEM in the right format (cf ==**XXX**==)|
|geoid        | string   | **Mandatory** | Path to the FOLDER containing a GEOID in the right format (cf ==**XXX**==)|
| ==pod==     |==string==| ==Optional==  |==Path to the POD FILE in the right format (cf ==**XXX**==)==|
| iers        | string   | Optional      | Path to the IERS folder containing the IERS file in the right format (cf ==**XXX**==)|
|operation    | string   | **Mandatory** | Possibilities:<ul><li>“direct”: to configure Sen2VM to compute direct location grids</li><li>“inverse”: to configure Sen2VM to compute inverse location grids</li></ul>|
| deactivate_available_refining| boolean  | Optional      | If false (default), refining information (if available in Datastrip Metadata) are used to correct the model before geolocation, cf product description in §==**XXX**==|
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
| output_folder | string   | **Mandatory**|Output path where the inverse location grids will be written (see §==**XXX**==)|

#### 1.2.1 L1B Product
L1B products can be downloaded at ==**XXX**==
The expected format is then SAFE format, i.e. a folder structured as illustrated in ==** TODO**==

Refining information can be found in the datastrip Metadata. If refined, the model of the satellite shall be modified accordingly. Information is stored in the filed “Level-1B_DataStrip_ID/Image_Data_Info//Geometric_Info/Refined_Corrections_List/Refined_Corrections/MSI_State”. They are present only if the flag “Image_Refining” is set at “REFINED” and absent if set at “NOT_REFINED”.
==**TODO Illustrating figure**==

An optional boolean argument is available in the configuration file (see §[1.2 Configuration file](#1.2-configuration-file)): deactivate_available_refining.
**By default, it is set at false**, meaning that the refining **information shall be taken into account** if available in the Datastrip Metadata. However, **if set at true**, the datastrip shall be **considered as NOT_REFINED**, meaning ignoring the refining information.
#### 1.2.2 GIPP
GIPP are configuration files used in operation to:
* represent the stable satellite information,
* configure calibration parameters of the satellite,
* configure the several algorithms of the processing chain.

By nature, GIPP are then versionnable. It is important to process with the version used to generate the L1B product. **A check is implemented** to verify that the version used is the same than the one listed in the Datastrip metadata (**check on the name**). This check can be deactivated through "gipp_check" parameter of the configuration file (cf ==**XXX**==). **This parameter is optional, and by default, its value is at true and the check is done, it can be forced at false if needed**.

The version used in operation of the GIPP are listed in the L1B Datastrip Metadata of the L1B product (see L1B_PRODUCT section), in the following section: Level-1B_DataStrip_ID/Auxiliary_Data_Info/GIPP_LIST, as illustrated in ==**FIGURE_XXX**==:

GIPP are however not directly available in the L1B product, then shall then be downloaded beforehand by the user. All versions are available at ==**XXX**==.

The GIPP required are the following ones:
* GIP_VIEDIR: contains Viewing Direction required by Rugged to create viewing model from TAN_PSI_X/Y_LIST tags, one GIP_VIEDIR file **per band**, each file containing information per **detector!** (in _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_X_LIST]_ and _[DATA/VIEWING_DIRECTIONS_LIST/VIEWING_DIRECTIONS/TAN_PSI_Y_LIST]_
* GIP_SPAMOD: contains transformations to apply to viewing direction from tags, available in the _[DATA]_ field:
--  PILOTING_TO_MSI,
-- MSI_TO_FOCAL_PLANE,
--FOCAL_PLANE_TO_DETECTOR XML, available **per detector**!
* GIP_BLINDP: contains information on blind pixel, contained in BLIND_PIXEL_NUMBER tag: _[DATA/BAND/BLIND_PIXEL_NUMBER]_, available **per band**!

#### 1.2.3 Altitude
==**TODO**==
#### 1.2.4 IERS
<![endif]-->

The IERS represents the ["International Earth Rotation and Reference Systems"](https://www.iers.org/IERS/EN/Home/home_node.html). It is important to have a valid and precise one to have a precise geolocation. During operation processing, IERS information are integrated in the L1B metadata datastrip, hence IERS information are available in the L1B product used in input, in the field _Level-1B_DataStrip_ID/Auxiliary_Data_Info/IERS_Bulletin_ as illustrated in the ==**FIGURE_XXX**==

<![endif]-->

IERS bulletins are used seamlessly with OREKIT, leading in very precise EOPhandling. However, the IERS available in the L1B Datastrip metadata is not in the right format, hence EOP (Earth Orientation Parameters) entries are initialized directly with the information, as “custom EOP”, skipping the IERS bulletins reader parts.

However, a very slight (neglectable) difference will be observed in geolocation compared to operational processing which uses the full IERS bulletin for its ortho-rectification process.

Hence an IERS bulletin can be provided in input for user (as optional). **Sen2VM behaviour regarding IERS will then be**:
* If an IERS bulletin is provided in input:
--Verify the date of the IERS bulletin compared to the Datastrip acquisition dates (using _DATASTRIP_SENSING_START_ and _DATASTRIP_SENSING_STOP_ in _“Level-1B_DataStrip_ID/General_Info/Datastrip_Time_Info"_
--Go in error if the IERS does not contains the full datastrip duration,
--Use the IERS bulletin if it contains the full datastrip duration,
* If not, use the information available in the L1B Datastrip Metadata.

==**TODO illustration "IERS information in L1B Datastrip Medata**==

Format of the IERS bulletin that can be provided is [Bulletin A](https://www.iers.org/IERS/EN/Publications/Bulletins/bulletins.html) 

#### 1.2.5 ==POD==
==**DESCOPED**==

### 1.3 Parameters files
Sen2VM calls SXGEO which is a mono-thread software. **Sen2VM is also designed to be a mono-threaded software.** However, as computations can be long, and because each couple detector/band is an independent mode, user might want to process only parts of the Datastrip per thread. This is handled by SXGEO and was propagated to Sen2VM. Configuration of the detectors/bands to process are done through the parameters file which is a json. **If not available, Sen2VM will process all detectors/bands**.

The configuration file contains 2 fields:
* “detectors”: Detectors are passed through string representing Sentinel-2 detectors encoded in **2 digits**, **separated by “-”.** Detectors indexes are from **“01” to “12”.**
* “bands”: Bands are passed through string representing Sentinel-2 bands encoded in **2 digits with a “B”** before and **separated from “-”**. Bands are going from **“B01” to “B12”, including a “B8A”.**

==**TODO Example**==

If a field (“detectors” or “bands”) **is missing in** the params.json file, **all items of the missing field** will be processed. For example, if “bands” if absent from the previous example in ==**FIGURE_XXX**==, Sen2VM will process all bands of detectors 1, 3 and 4.

**It is to be noted that a small optimization in SXGEO is done not to reload DEM tiles when processing bands of the same resolution for the same detector.**


