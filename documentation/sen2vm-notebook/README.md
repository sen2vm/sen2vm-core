# sen2vm-notebook Processing Workflow

This repository provides a complete workflow to run **sen2vm** inside Docker and generate orthorectified and mosaicked outputs from Sentinel-2 L1B data.  
The project is designed and tested on **Linux**. It may not work reliably on **Windows**.

---

## Prerequisites

- **Docker** installed and running  
- **Python 3.x**  
- Required data:
  - A complete **Sentinel-2 L1B product**.  
    *If the product does not contain the **SAFL1B** XML file, the orthorectification step cannot be executed.*
  - **DEM files** placed in the appropriate directory


---

## Directory Structure

```bash
sen2vm-notebook
│
├── DATA/
│ ├── DEM/ # Put your DEM files here
│ └── <L1B_product>/ # Place the full L1B product here
|   ├── DATASTRIP  # Required
|   ├── GRANULE   # Required
|   ├── (*SAFL1B* .xml)  # Required for the orthorectification step
|   └──  ...
│
├── src/
│ ├── requirements.txt
│ └── notebook.ipynb 
│
└── ...
```

## Python Environment Setup

Inside the `src/` directory:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Select the virtual environment kernel in your Jupyter session.

## Notebook Configuration

In the first cell of the notebook:

1. Set the path to:
   - The L1B product
   - The output directory
   - The geoid file (optional - the default sen2vm geoid is used if not specified)

2. Adjust configuration parameters for:
   - sen2vm
   - Orthorectification settings

---

## Processing Steps

Execute the notebook cell by cell in the following order:

1. Variable definitions  
2. Clone `sen2vm-core` and `sen2vm-gipp`, and manage GIPP assets  
3. Automatic download of the IERS bulletin (A or B)  
4. Generation of `config.json` in: `/sen2vm-notebook/UserConf`
5. Generation of `params.json`  
6. Execution of sen2vm inside Docker  
7. Generation of a `.sh` script, then execution inside a second Docker container running the latest GDAL:
     - Orthorectification by band  
     - Mosaicking  

Docker images are automatically cleaned up after each execution.

---

## Execution

Run all notebook cells sequentially.  
If all paths and parameters are correct, the workflow should run end-to-end without manual intervention.
