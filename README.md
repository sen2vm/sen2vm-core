[README](README.md) (current file)
* [HOWTO](documentation/Usage/HOWTO.md)
* [Inputs](documentation/Input/input_description.md)
* Outputs:

  * [direct location grids](documentation/Output/output_direct_loc.md)
  * [inverse location grids](documentation/Output/output_inverse_loc.md)
  * [output grids usage](documentation/Output/output_grids_usage.md)



# Project sen2vm-core
The Sen2VM core is a standalone tool designed to generate geolocation grids that will be included in the Level-1B (L1B) product:

* Its primary function is to create **direct location grids**, mapping the L1B product in sensor geometry down to the ground.
* Additionally, the tool supports the generation of inverse location grids, enabling mapping from a specific ground area back to the corresponding area in sensor geometry.

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

Regarding the several git repository, here is a schema on how they are linked together:
![Git links](/assets/images/README_Git_links.png "Git links")


## 1. L1B format description

Sentinel-2 L1C products are stored by tiles, geolocated on ground. One Sentinel-2 acquisition (hereby called **Datastrip**) cover several L1C tiles. For each acquisition, several L1C tiles are create (with some not fully covered, with NoData inside). Each tile contains, a Datastrip metadata (duplicated in each L1C tiles generated), a Tile Metadata and all the scientific data (images/mask), stored in a GRANULE folder.


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
* L1B Product,
* Some GIPP files (parameters files used in operational production),
* Digital Elevation Model (DEM),
* GEOID model to measure precise surface elevations,
* IERS bulletin that provides data and standards related to Earth rotation and reference frames,
* Additional information for configuration.

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