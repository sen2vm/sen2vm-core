Return to [README](README.md)

# HOWTO

>[!CAUTION]
> Sen2VM core depends on java 8 and gdal=3.6.2 with java bindings.

Sections:

* Notebooks are available to users to configure and run Sen2VM in a already installed enviroment <mark>TODO</mark>
* Those Notebook are using a Docker with full environment and Sen2VM installed inside it <mark>TODO</mark>
* calling Sen2VM in Java
* Inside build-env

Also available: how to compile <mark>TODO<mark>

## 1. How to run using Notebooks

## 2. How to run within a Docker

* To build the image, download the Dockerfile from the project root directory and run the following command in the same directory:
```
docker build . -t sen2vm
```
* Launch Sen2vm using the following command:
```
docker run -it --rm -v [input-data]:[/mounted-name] sen2vm -c [configuration_filepath] [-p [parameters_filepath]]
```
> [!CAUTION]
> -v /NNN:/NNN can be done several time, for example -v /data:/data -v /home/login/working_dir:/home/login/working_dir. This will mount directories inside the docker. All files (input json and folders/files listed in configuration_file) shall be on a mounted disk, if not, the won't be visible

Where:
* input-data: the input folder
* configuration_filepath: configuration file containing all inputs related to product or grids that are required by Sen2VM (see §[2.1 Configuration file](#21-configuration-file) for further information). Please indicate the input paths with **absolute path** from the docker volume directory (/data in the example above), note that this input is **Mandatory**.
* parameters_filepath:  file to configure the detectors/bands to process. If not available, all detectors/bands will be processed (see §[2.2 Parameters file](#22-parameters-file) for further information).This input is **Optional**.

> [!NOTE]
> To understand the configuration, please refer to §[2 Inputs](#2-inputs)

## 3. How to run using java command

Sen2VM core depends on java 8 and gdal=3.6.2 with java bindings.
First, download the jar of Sen2VM core, then run the following command to launch it:
```
java -jar target/sen2vm-core-<NN.NN.NN>-jar-with-dependencies.jar -c [configuration_filepath] [-p [parameters_filepath]]
```
Where:
* <NN.NN.NN> is the version number of Sen2VM launched
* configuration_filepath: configuration file containing all inputs related to product or grids that are required by Sen2VM (see §[2.1 Configuration file](#21-configuration-file) for further information). Please note that this input is **Mandatory**. 
* parameters_filepath:  file to configure the detectors/bands to process. If not available, all detectors/bands will be processed (see §[2.2 Parameters file](#22-parameters-file) for further information).This input is **Optional**.

To install the java environment, it possible to use the docker build environment. Please refer to §[1.5 How to install build environment](#15-how-to-install-build-environment)

> [!NOTE]
> Sen2VM core can also be rebuild from sources. Please refer to §[1.4 How to compile sen2vm-core](#14-how-to-compile-sen2vm-core)

Example from current repository:
```
java -jar target/sen2vm-core-0.0.1-jar-with-dependencies.jar -c src/test/resources/configuration_example.json -p src/test/resources/params.json
```


## 4. How to compile sen2vm-core from sources

Before compiling/installing sen2vm-core, make sure to install the required dependencies. To do so, please refer to [https://github.com/sen2vm/sen2vm-build-env/tree/main](https://github.com/sen2vm/sen2vm-build-env/tree/main)

Then, inside `sen2vm-core` folder, run the next commands:
```
mvn clean install
java -jar target/sen2vm-core-<NN.NN.NN>-jar-with-dependencies.jar -c [configuration_filepath] [-p [parameters_filepath]]
```

## 5. How to install build environment

Sen2VM core depends on gdal=3.6.2 with java bindings. An ready for use docker image is available. Please, pull the image from here : https://github.com/sen2vm/sen2vm-build-env/pkgs/container/sen2vm-build-env

* Pull the image :
```
docker pull ghcr.io/sen2vm/sen2vm-build-env:<tag>
```
* Launch the container :
```
docker run -it --rm --user $UID:$GID -v <sen2vm-core-folder>:/Sen2vm ghcr.io/sen2vm/sen2vm-build-env:latest bash
```
* Execute the running java commands inside the opening bash.