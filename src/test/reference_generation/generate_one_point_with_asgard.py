#!/usr/bin/env python
# coding: utf8
#
# Copyright 2023 CS GROUP
# Licensed to CS GROUP (CS) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# CS licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# pylint: disable=too-many-locals,import-error,too-many-branches,too-many-lines,too-many-arguments,too-many-statements,pointless-string-statement
"""
Unit tests for MSI Sentinel 2 products
"""

import math

import os
import os.path as osp

# flake8: noqa ignore auto-generated bash code lines that are > 120 characters
import glob
import sys
from pathlib import Path
import os.path as op
import numpy as np
from osgeo import gdal, gdalconst
import json
import pandas as pd
from termcolor import colored
import re

from math import sqrt, isnan

from asgard.sensors.sentinel2.s2_band import S2Band
from asgard.sensors.sentinel2.s2_detector import S2Detector
from asgard.sensors.sentinel2.s2_sensor import S2Sensor
from asgard_legacy.sensors.sentinel2.msi import S2MSILegacyGeometry
from asgard.sensors.sentinel2.msi import S2MSIGeometry

from asgard_legacy_drivers.drivers.sentinel_2_legacy import S2LegacyDriver

from pyrugged.errors.pyrugged_exception import PyRuggedError


georefConventionOffsetPixel = -0.5
georefConventionOffsetLine = 0.5

def normalize_path(path):
  return path if (op.isabs(path) or path.startswith("./")) else "../../../" + path


#def test_Sen2VM(test_to_generate):
def test_Sen2VM(config_file: str, detector: str, band: str, col_lon: float, row_lat: float, alt: float | None):
    """
    Run general test to compare direct or inverse grids generated with SEN2VM

    :param data: list of test cases
    """

    #print(f"Test: {test_to_generate}")
    print(f"Start")
    

    # Read configuration
    # with open(os.path.join(input_dir, "configuration.json")) as json_file:
    with open(config_file) as json_file:
        json_data = json.load(json_file)

    # Get all needed info in the json
    l1b_product = normalize_path(json_data["l1b_product"])
    print(f"l1b_product: {l1b_product}")
    iers = normalize_path(op.join(json_data["iers"]))
    print(f"iers: {iers}")
    geoid = normalize_path(op.join( json_data["geoid"]))
    print(f"geoid: {geoid}")
    dem = normalize_path(op.join(json_data["dem"]))
    print(f"dem: {dem}")
    gipp = normalize_path(op.join(json_data["gipp_folder"]))
    print(f"gipp: {gipp}")

    xml_product = glob.glob(op.join(l1b_product, "DATASTRIP", "*", "*.xml"))[0]
    refining = not (json_data["deactivate_available_refining"])

    # Init
    config = S2LegacyDriver(xml_product, iers, geoid, dem, gipp).read()
        
    print("Config Keys: ", config.keys())
    print("Refining information: ", config["refining"])
    
    product = S2MSILegacyGeometry(**config)


    # Direct or inverse case
    if "direct" in json_data["operation"]:
        if alt:
           print(f"Direct loc, Altitude {alt} ignored (6th argument)")
        compute_direct_location(product, detector, band, col_lon, row_lat)
    elif "inverse" in json_data["operation"]:
        # Replace input directory to output_folder written in configuration file
        input_dir = normalize_path(json_data["inverse_location_additional_info"]["output_folder"])
        
        # Redefine product as ASGARD-Legacy is requiring the altitude as input
        if alt:
            print("Inverse loc, using input alt: ", alt)
            grounds_2 = np.array([col_lon,row_lat,alt])
        else:
            # Generate the altitudes from TileUpdater from the asgard propagation_model.
            # It is necessary as ASGGARD-Legacy requires the altitudes as input
            grounds = np.array([[grounds[0][i], grounds[1][i]] for i in range(len(grounds[0]))], np.float64)
            grounds_2 = np.array([[grounds[i][0], grounds[i][1], 0] for i in range(grounds.shape[0])], np.float64)            
            product_asgard = S2MSIGeometry(**config)  # TOFIX
            if product_asgard.propagation_model is not None:
                flat_ground_coords = grounds[..., :2].reshape((-1, 2))
                
                tile_updater = product_asgard.propagation_model.tile_updater
                if tile_updater is not None:
                    if product_asgard._cache is None:
                        product_asgard._cache = TilesCache(SimpleTile, tile_updater, product_asgard._max_cached_tiles)
                    altitudes = np.ones(flat_ground_coords.shape[0:1]) * np.nan
                    flat_ground_coords_rad = np.deg2rad(flat_ground_coords)
                    tiles, indexes = product_asgard._cache.get_tiles(flat_ground_coords_rad[:, 1], flat_ground_coords_rad[:, 0])
                    for tile, index in zip(tiles, indexes):
                        altitudes[index] = tile.interpolate_elevation_arr(
                            flat_ground_coords_rad[index, 1], flat_ground_coords_rad[index, 0]
                        )
                        grounds_2[index,2] = altitudes[index]
                        grounds = grounds_2
            print("Inverse loc, recomputing altitude as not provided: ", grounds_2)

        compute_inverse_location(input_dir, product, product_asgard, detector, band, grounds_2)    
    else:
        print('No test found: no "direct" or "inverse" found in name test')
    

def compute_inverse_location(product, product_asgard, detector, band, grounds):
    """
    Inverse location of a data test from SEN2VM

    :param input_dir: directory with all inverse grids to verify (bands x detectors in params.json)
    :param product: S2MSILegacyGeometry (legacy-base) ; S2MSIGeometry (refactored)
    :param ref_dir: output directory for asgard grids
    :return:
    """

    sensor = S2Sensor(detector, band).name
    print("#", sensor)


    
    # Call inverse location
    inverse_pixels = None
    try:
        inverse_pixels = product.inverse_loc(grounds, geometric_unit=sensor)
        # note : (col, row) conv
    except AttributeError as exp:
        print(colored("    ! PyRuggedError: " + str(exp), "red"))

    # TODO
    inverse_pixels = inverse_pixels + [-georefConventionOffsetPixel, -georefConventionOffsetLine]



def compute_direct_location(product, detector, band, col, row):
    """
    Direct location of a data test from SEN2VM

    :param config:
    :param input_dir: directory with all inverse grids to verify (bands x detectors in params.json)
    :param product: dict of S2geo interface
    :param ref_dir: output directory for asgard grids
    """

    sensor = S2Sensor(detector, band).name
    print(f"# Sensor {sensor}")

    rows_detecteur_band_grid = []


    # Pixels need to be [[col1, row1], [col1, row1] ...] for direct location
    pixel = np.array([col, row], np.float64)

    # Call direction location
    ground = []
    try:
        grounds, _ = product.direct_loc(pixel, sensor)
        # note: long, lat, alt conv
        print(f"ground (lon,lat,alt) for input coord {pixel}: ", ground)
    except PyRuggedError as exp:
        print(colored("    ! PyRuggedError: " + str(exp), "red"))


def read_params(input_test: str):
    """
    Read bands and detectors listed in params.json
    :param input_test: test directory with params.json file
    :return: detectors: list of S2Detector, bands: list of S2Band
    """
    #with open(os.path.join(input_test, "params.json")) as json_file:
    with open(input_test) as json_file:
            params = json.load(json_file)
    detectors = [S2Detector.from_name("D" + name) for name in params["detectors"]]
    bands = [S2Band.from_name(name) for name in params["bands"]]
    return detectors, bands

    

if __name__ == "__main__":
    
  expected_message = "Waiting for 5 (or 6) args: <a_config_file_path> <detector> <band> <col_or_longitude> <row_or_latitude> (<altitude>)" 

  match len(sys.argv) - 1:
    case 5:
      print(f"Five arguments (expecting <a_config_file_path> <detector> <band> <col_or_longitude> <row_or_latitude>): {sys.argv[1]}, {sys.argv[2]}, {sys.argv[3]}, {sys.argv[4]}")
      
      if (Path(sys.argv[1]).is_file()
      and re.fullmatch(r"(0[1-9]|1[0-2])",sys.argv[2])
      and re.fullmatch(r"B(0[1-9]|1[0-2]|8A)",sys.argv[3])
      and float(sys.argv[4])
      and float(sys.argv[5])):
        test_Sen2VM(sys.argv[1],sys.argv[2],sys.argv[3], sys.argv[4], sys.argv[5])
      else:
        print(expected_message)
    case 6:
      print(f"Six arguments (expecting <a_config_file_path> <detector> <band> <col_or_longitude> <lrow_or_latitude> <altitude>): {sys.argv[1]}, {sys.argv[2]}, {sys.argv[3]}, {sys.argv[4]}, {sys.argv[5]}")
    
      if (Path(sys.argv[1]).is_file()
      and re.fullmatch(r"(0[1-9]|1[0-2])",sys.argv[2])
      and re.fullmatch(r"B(0[1-9]|1[0-2]|8A)",sys.argv[3])
      and float(sys.argv[4])
      and float(sys.argv[5])
      and float(sys.argv[6])):
        test_Sen2VM(sys.argv[1],sys.argv[2],sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6])
      else:
        print(expected_message)
    case _:
      print(expected_message)
