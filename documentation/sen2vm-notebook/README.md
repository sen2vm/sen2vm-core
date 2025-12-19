# sen2vm-notebook Processing Workflow

This repository provides a complete workflow to run **sen2vm** inside Docker and generate orthorectified and mosaicked outputs from Sentinel-2 L1B data.  
The project is designed and tested on **Linux**. It may not work reliably on **Windows**.
Actually, this notebook can generate an inverse grid but can not use it to apply the orthorectification. 

---
## Prerequisites

- **Docker** installed and running  
- **Python 3.x**  
- Required data:
  - **Sentinel-2 L1B product** : /DATSTRIP and /GRANULE.  
  - **DEM files** placed in the appropriate directory


---

## Mandatory Directory Structure

```bash
WORKDIR
│
├── DATA/
│ ├── DEM/ # Put your DEM files here
| ├── GEOID/ # Put your GEOID files here (Optional)
│ └── <L1B_product>/ # Place the full L1B product here
|   ├── DATASTRIP  # Required
|   ├── GRANULE   # Required
|   └──  ...
└── ...
```

If the GEOID folder is empty, the notebopok will automaticaly use the geoid provided by sen2vm-core

## Python Environment Setup

In the notebook directory create your venv :

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Select the virtual environment kernel in your Jupyter session.

## Notebook Configuration

In the first cell of the notebook:

1. Set the path to:
   - The working directory 
   - The L1B product
   - The output directory

2. Adjust configuration parameters for:
   - sen2vm
   - Orthorectification settings

---

## Processing Steps

Execute the notebook cell by cell in the following order:

1. Variable definitions  
2. Clone `sen2vm-gipp`, and manage GIPP assets  
3. Automatic download of the IERS bulletin 
4. Generation of `config.json` in: `/WORKDIR/UserConf`
5. Generation of `params.json` in: `/WORKDIR/UserConf`
6. Execution of sen2vm inside Docker  
7. Generation of a `.sh` script, then execution inside a second Docker container running the latest GDAL:
     - Orthorectification by band  
     - Mosaicking  

Docker images are automatically cleaned up after each execution.

---

## Execution

Run all notebook cells sequentially.  
If all paths and parameters are correct, the workflow should run end-to-end without manual intervention.
