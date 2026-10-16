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
            grounds = np.array([col_lon, row_lat])
            grounds_2 = np.array([col_lon, row_lat, 0])
            product_asgard = S2MSIGeometry(**config)  # TOFIX
            if product_asgard.propagation_model is not None:
                
                tile_updater = product_asgard.propagation_model.tile_updater
                if tile_updater is not None:
                    if product_asgard._cache is None:
                        product_asgard._cache = TilesCache(SimpleTile, tile_updater, product_asgard._max_cached_tiles)

                    ground_rad = np.deg2rad(grounds)
                    tiles, indexes = product_asgard._cache.get_tiles(np.atleast_1d(ground_rad[1]), np.atleast_1d(ground_rad[0]))
                    for tile, index in zip(tiles, indexes):
                        altitude = tile.interpolate_elevation_arr(np.atleast_1d(ground_rad[1]), np.atleast_1d(ground_rad[0])
                        )
                        grounds_2[2] = altitude[0]
            grounds = grounds_2
            print("Inverse loc, recomputing altitude as not provided: ", grounds_2)

        compute_inverse_location(product, detector, band, grounds_2)    
    else:
        print('No test found: no "direct" or "inverse" found in name test')
    

def compute_inverse_location(product, detector, band, grounds):
    """
    Inverse location of a data test from SEN2VM

    :param product: S2MSILegacyGeometry (legacy-base) ; S2MSIGeometry (refactored)
    :param detector: S2Detector Object from ASGARD
    :param band: S2Band Object from ASGARD
    :param grounds: Grounds np array lon, lat, alt
    :return:
    """

    sensor = S2Sensor(detector, band).name
    print("#", sensor)

    # Call inverse location
    inverse_pixels = None
    try:
        inverse_pixels = product.inverse_loc(grounds, geometric_unit=sensor)

        print(
          f"sensor (col, row) for input ground coord ",
          f"({grounds[0]:.20f}, {grounds[1]:.20f}, {grounds[2]:.20f}): ",
          f"({inverse_pixels[0]:.20f}, {inverse_pixels[1]:.20f})"
          )

    except AttributeError as exp:
        print(colored("    ! PyRuggedError: " + str(exp), "red"))


def compute_direct_location(product, detector, band, col, row):
    """
    Direct location of a data test from SEN2VM

    :param product: S2MSILegacyGeometry (legacy-base); S2MSIGeometry (refactored)
    :param detector: S2Detector Object from ASGARD
    :param band: S2Band Object from ASGARD
    :param col: column in float
    :param row: row in float
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
        print(
          f"ground (lon,lat,alt) for input coord ",
          f"({pixel[0]:.20f}, {pixel[1]:.20f}): ",
          f"({grounds[0]:.20f}, {grounds[1]:.20f}, {grounds[2]:.20f})"
          )
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

def is_float(value):
    try:
        float(value)
        return True
    except (ValueError, TypeError):
        return False

if __name__ == "__main__":
    
  expected_message = "Waiting for 5 (or 6) args: <a_config_file_path> <detector> <band> <col_or_longitude> <row_or_latitude> (<altitude>)" 

  match len(sys.argv) - 1:
    case 5:
      print(f"Five arguments (expecting <a_config_file_path> <detector> <band> <col_or_longitude> <row_or_latitude>): {sys.argv[1]}, {sys.argv[2]}, {sys.argv[3]}, {sys.argv[4]}")

      launch_sen2vm = True      
      if not Path(sys.argv[1]).is_file():
        print(f"{sys.argv[1]} is supposed to be a file")
        launch_sen2vm = False
      if not re.fullmatch(r"D(0[1-9]|1[0-2])", sys.argv[2]):
        print(f"{sys.argv[2]} is supposed to be a detector string between D01-D12")
        launch_sen2vm = False
      if not re.fullmatch(r"B(0[1-9]|1[0-2]|8A)", sys.argv[3]):
        print(f"{sys.argv[3]} is supposed to be a band string between B01-B12 (including B8A)")
        launch_sen2vm = False
      if not is_float(sys.argv[4]):
        print(f"{sys.argv[4]} is supposed to be a float")
        launch_sen2vm = False
      if not is_float(sys.argv[5]):
        print(f"{sys.argv[5]} is supposed to be a float")
        launch_sen2vm = False
        
      if launch_sen2vm:
        test_Sen2VM(sys.argv[1], S2Detector.from_name(sys.argv[2]), S2Band.from_name(sys.argv[3]), float(sys.argv[4]), float(sys.argv[5]), None)
      else:
        print(expected_message)
    case 6:
      print(f"Six arguments (expecting <a_config_file_path> <detector> <band> <col_or_longitude> <lrow_or_latitude> <altitude>): {sys.argv[1]}, {sys.argv[2]}, {sys.argv[3]}, {sys.argv[4]}, {sys.argv[5]}")
    
      launch_sen2vm = True      
      if not Path(sys.argv[1]).is_file():
        print(f"{sys.argv[1]} is supposed to be a file")
        launch_sen2vm = False
      if not re.fullmatch(r"D(0[1-9]|1[0-2])", sys.argv[2]):
        print(f"{sys.argv[2]} is supposed to be a detector string between D01-D12")
        launch_sen2vm = False
      if not re.fullmatch(r"B(0[1-9]|1[0-2]|8A)", sys.argv[3]):
        print(f"{sys.argv[3]} is supposed to be a band string between B01-B12 (including B8A)")
        launch_sen2vm = False
      if not is_float(sys.argv[4]):
        print(f"{sys.argv[4]} is supposed to be a float")
        launch_sen2vm = False
      if not is_float(sys.argv[5]):
        print(f"{sys.argv[5]} is supposed to be a float")
        launch_sen2vm = False
      if not is_float(sys.argv[6]):
        print(f"{sys.argv[6]} is supposed to be a float")
        launch_sen2vm = False
      
      if launch_sen2vm:          
        test_Sen2VM(sys.argv[1], S2Detector.from_name(sys.argv[2]), S2Band.from_name(sys.argv[3]), float(sys.argv[4]), float(sys.argv[5]), float(sys.argv[6]))
      else:
        print(expected_message)
    case _:
      print(expected_message)
  print()