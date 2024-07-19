# sen2vm-core
Sen2VM standalone tool that will be also called by Sen2VM SNAP Plugin

# Installation guide 

Sen2VM requires some external libraries like :
- SXGEO : v0.1.13
- GDAL : v3.6.2

If you don't want to face any environment compatibility matters, we provide a Dockerfile to build a docker image that contains all the required dependencies :
```
docker build -t sen2vm_core .
```

