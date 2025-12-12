[README](README.md) (current file)
* [HOWTO](documentation/Usage/HOWTO.md)
* [Inputs](documentation/Input/input_description.md)
* Outputs:

  * [direct location grids](documentation/Output/output_direct_loc.md)
  * [inverse location grids](documentation/Output/output_inverse_loc.md)
  * [output grids usage](documentation/Output/output_grids_usage.md)



# Project sen2vm-core
The Sen2VM core is a standalone tool designed to generate geolocation grids that will be included in the Level-1B (L1B) product:


Please note that Sen2VM exists implemented as a SNAP plugin, which calls the Sen2VM standalone tool during execution.

Please note that Sen2VM exists implemented as a SNAP plugin, which calls the Sen2VM standalone tool (sen2vm-core) during execution.

This documentation is split into 5 parts:

* Quick reminder of L1C format and introduction to L1B format: § [L1B format description](#1-l1b-format-description)
* Instructions to  compile and/or run Sen2VM with some examples: $ [Quickstart](#2-quickstart)
* Description and format of expected inputs: § [Inputs](#3-inputs)
* Output § [Outputs](#4-outputs):

  * Details on generated outputs grids: § [Outputs](#41-output-grids-of-Sen2VM)
  * Examples of usage of the ouput grids: § [Usage of output grids](#42-usage-of-output-grids)

* Validation process, including test procedures and data used: § [Validation](#5-validation)

>[!NOTE]
> If you want to compile Sen2VM by yourself, please refer to dedicated [HOWTO section](documentation/Usage/HOWTO.md)

<mark> TODO: Schema of how all Sen2VM projects are linked</mark>

## 1. L1B format description

Sentinel-2 L1C products are store by tile, geolocated one ground. One Sentinel-2 acquisition (hereby called **Datastrip**) cover sevral L1C tiles. For each acquisition, several L1C tiles are create (with some not fully covered, with NoData inside). Each tile contains, a Datastrip metadata (duplicated in each L1C tiles generated), a Tile Metadata and all the scientific data (images/mask), stored in a GRANULE folder.


At L1B (as in L0 and L1A), data is not yet split in L1C tiles. Instead, data is split per GRANULE. One granule is an Along-Track portion of one Detector acquisition.

![Sentinel-2 Format Intro](/assets/images/README_Format_Intro.png "Format Intro")

![Sentinel-2 Format Intro](/assets/images/README_Format_L1B.png "Format Intro")

## 2. Quickstart

Inputs are split into 2 groups:

* configuration: all inputs related to product or grids that are required by Sen2VM (see §[2.1 Configuration file](#21-configuration-file) for further information). Please note that this input is **Mandatory**. 
* "parallelisation": the detectors/bands to process. If not available, all detectors/bands will be processed (see §[2.2 Parameters file](#22-parameters-file) for further information).This input is **Optional**.

Sen2VM can be called using the following command:
> [!CAUTION]
> Sen2VM core depends on java 8 and gdal=3.6.2 with java bindings.
**Please look below the command line for help in launching Sen2VM-core**</mark>
```
java -jar target/sen2vm-core-<NN.NN.NN>-jar-with-dependencies.jar -c [configuration_filepath] [-p [parameters_filepath]]
```
Where:

* <NN.NN.NN> is the version number of Sen2VM launched
* configuration_filepath: configuration file
* parameters_filepath:  "parallelisation" input 

> [!NOTE]
> This will only create geolocation grids. Examples of usage can be accessible through §[Usage of output grids](#4-usage-of-output-grids)


> [!IMPORTANT]
> As Sen2VM requires specific installation, several tools have been made accessible to help users using Sen2VM:
> * Notebooks, doing generation of grid **AND** examples of usage
> * A Dockerfile that will build a Docker with Sen2VM and all its dependencies installed inside it
> * A build-environment, that can be used to install Sen2VM inside it or use to build Sen2VM from sources
> Please refere to [HOWTO](documentation/Usage/HOWTO.md)


## 3. Inputs

> [!NOTE]
> The orekit-data is required to process the grid. During the first run, the orekit-data is extracted from the JAR file (.jar) and placed in the same directory as the JAR. The orekit-data can be replaced by the user if needed. The official orekit-data is available https://github.com/sen2vm/sen2vm-core/tree/main/orekit-data.

Inputs required by Sen2VM are:

* L1B Product (see [How to Download L1B Data from CDSE](documentation/Input/L1B_CDSE_Download.md) for download instructions),
* Some GIPP files (parameters files used in operational production),
* Digital Elevation Model (DEM),
* GEOID model to measure precise surface elevations,
* IERS bulletin that provides data and standards related to Earth rotation and reference frames,
* Additional information for configuration.

Inputs description can be access at [Inputs](documentation/Input/input_description.md)

### 2.1 Configuration file
The configuration file will contain all information about the product, the auxiliarry data and the operations to be performed.
It  is a file in  [JSON format](https://en.wikipedia.org/wiki/JSON) and an example is available at: https://github.com/sen2vm/sen2vm-core/blob/main/src/test/resources/configuration_example.json

![Configuration file example](/assets/images/README_ConfigurationFileExample.png "Configuration file example.")

Each parameter description can be found in the table below:

| Name | Type | Required |                                                                                                             Description                                                                                                                                           |
| ----------- | :------: | :-----------: |:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| l1b_product | string   | **Mandatory** |                                                                     Path to L1B_PRODUCT folder, where L1B_Product format is in SAFE format (described in § [L1B Product](#211-l1b-product), including DATASTRIP + GRANULE)                                                                      |
| gipp_folder | string   | **Mandatory** |                                                                    Path to a folder containing at least the 3 types of GIPP required by Sen2VM (other will be ignored). For more information, refer to § [GIPP](#212-gipp).                                                                     |
| auto_gipp_selection  | boolean  | Optional      |                                                                                                If true (default), the GIPPs are selected automaticaly from the datastrip metadata and an untar extraction is performed if necessary. Otherwise, the GIPPs must have a single, non-multiple version. This option is mainly intended for experimental use to test custom GIPP values (see GIPP section § [GIPP](#212-gipp)</mark>)      |
| grids_overwriting | boolean  | Optional      |                                                                                             Activate the grids overwritings if the grids have already or partialy computed. It is false by default.                                                                                             |
|dem          | string   | **Mandatory** |                                                                                                    Path to the FOLDER containing a DEM in the right format (cf § [Altitude/DEM](#2131-dem)).                                                                                                    |
|geoid        | string   | **Mandatory** |                                                                                                  Path to the FILE containing a GEOID in the right format (cf § [Altitude/GEOID](#2132-geoid))                                                                                                   |
| iers        | string   | Optional      |                                                                                                  Path to the IERS folder containing the IERS file in the right format (cf § [IERS](#214-iers))                                                                                                  |
| operation    | string   | **Mandatory** |                                                    In term of operation you can select the following Sen2VM configurations:<ul><li>“direct”: to compute direct location grids</li><li>“inverse”: to compute inverse location grids</li></ul>                                                    |
| deactivate_available_refining| boolean  | Optional      |                                          If set to false (default), refining information (if available in Datastrip Metadata) are used to correct the model before geolocation, cf. product description in § [L1B Product](#2112-refining-information)                                          |
| export_alt   | boolean  | Optional      |  If set to false (default), direct location grids will include only two bands: **Long/Lat**. If set to true, a third band representing the **Altitude** will also be exported, increasing the output grid size. See product description in §[Direct location grids](#31-direct location grids)  |
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
| output_folder | string   | **Mandatory**|                Path where the inverse location grids are written (see § [Inverse location grids](#32-inverse-location-grids))                |

> [!NOTE]
> The orekit-data is required to process the grid. During the first run, the orekit-data is extracted from the JAR file (.jar) and placed in the same directory as the JAR. The orekit-data can be replaced by the user if needed. The official orekit-data is available https://github.com/sen2vm/sen2vm-core/tree/main/orekit-data.

Inputs description can be access at [Inputs](documentation/Input/input_description.md)

## 4. Outputs

### 4.1 Output grids of Sen2VM

The output of the Sen2VM tool can be either direct location grids or inverse location grids. Their computation depends on the following parameters:

* selected detectors and bands,
* grid step,
* computation options (e.g. refining).

Please note that only the direct location grids will be included in the input product and handled by gdal. Inverse location grids, as they represent a particulat area on the ground will be exported outside the product, in a folder selected by the user (which can be inside the input product if wanted).

### 4.2 Usage of output grids

Examples are available:

* In §[Example section](documentation/Output/output_grids_usage.md)
* In <mark>Notebooks</mark>

## 5. Validation

In a nutshell, validation is split into 2 main parts:

 * Functional tests:

     * Integrated into the CI/CD process,
     * Validate that Sen2VM takes into account the different input provided, and that output grids are consistent with reference ones.

 * Quality tests:

     * Launched manually,
     * Proving, using several TDS, that grids generated by Sen2VM are consistent with the ones generated with the legacy,
     * Proving that those grids can be used to reach equivalent quality at L1C,
     * These processings will also be used to assess the processing performances.

Tests are more detailed in:

 * In [/src/test/java/esa/sen2vm/](/src/test/java/esa/sen2vm/) for functionnal tests,
 * In dedicated <mark>**Document**</mark>, for quality tests (which also includes functionnal tests description)

# 

[README](README.md) (current file)
* [HOWTO](documentation/Usage/HOWTO.md)
* [Inputs](documentation/Input/input_description.md)
* Outputs:

  * [direct location grids](documentation/Output/output_direct_loc.md)
  * [inverse location grids](documentation/Output/output_inverse_loc.md)
  * [output grids usage](documentation/Output/output_grids_usage.md)