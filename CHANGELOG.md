# Sen2VM Release history

## 1.1.2 (2025-12-19)

### Main feature

Documentation rework and example Notebook provision

### Updates

* Fix:

  *

* Feature:

  * Addition of example Notebook

* Doc:

  * Doc refactoring
  * Inclusion of usage of Notebooks
  * Inclusion of tutorials on how to get required inputs
  * Updates on orthorectification example to be compatible with PDI/EUP.SAFE/EUP.COMPACT_SAFE
  * Addition of a CHANGELOG

## 1.1.1 (2025-12-18)

### Main feature

Fix Dockerfile with updated version number

### Updates

* Fix:

  * Hotfixe Dockerfile to have the same version than pom.xm. Addition of Commentary not to reproduce the issue


## 1.1.0 (2025-12-18)

### Main feature

Optimized GIPP loading and License clarification

### Updates

* Fix:
  
  * Addition of License header

* Feature:

  * GIPP can now be stored in any subfolders, and can be in tgz or tar.gz

* Doc:

  * New GIPP loading strategy described
  * Clear statment of the License in the main README

## 1.0.3 (2025-12-10)

### Main feature

### Updates

* Fix:

  * Hotfix of a `grids_overwriting` with "s" in the code


## 1.0.2 (2025-12-10)

### Main feature

Ease user experience and addition of a Dockerfile to have Sen2VM automatically installed in a suitable environment

### Updates

* Fix:
  
  * Logs clearer when GEOID is missing
  * Processing of only one granule allowed

* Feature:

  * New `grids_overwriting` configuration parameter
  * New Dockerfile to automatically install Sen2VM in a suitable context

* Doc:

  * New `grids_overwriting` configuration parameter description
  * Usage of Sen2VM updated: how to launch Sen2VM and how to use the new Dockerfile

## 1.0.1 (2025-12-03)

### Main feature

Allow Sen2VM to handle both PDI and EUP format

### Updates

* Fix:
  
  * Handling of PDI format (not only EUP) for Sen2VM

* Feature:

  * orekit-data folder extracted and placed beside jar at first run

* Doc:

  * update regarding orekit-data folder

## 1.0.0 (2025-06-12)

### Main feature

* First complete, official, validated version