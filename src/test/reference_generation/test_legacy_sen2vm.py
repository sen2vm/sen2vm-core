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

import json
import os
import os.path as osp
import re

# flake8: noqa ignore auto-generated bash code lines that are > 120 characters
import glob
import sys
import time
from collections import defaultdict
from pathlib import Path
from typing import Type
import os.path as op
import xml.etree.ElementTree as ET
import numpy as np
from osgeo import gdal, gdalconst
from termcolor import colored

# import dill as pickle

# from asgard.core.toolbox import NumpyArrayEncoder
# from asgard.sensors.sentinel2.msi import S2MSIGeometry
from asgard.sensors.sentinel2.s2_band import S2Band
from asgard.sensors.sentinel2.s2_detector import S2Detector
from asgard.sensors.sentinel2.s2_sensor import S2Sensor

from asgard_legacy.sensors.sentinel2.msi import S2MSILegacyGeometry

from asgard_legacy_drivers.drivers.sentinel_2_legacy import S2LegacyDriver

from pyrugged.errors.pyrugged_exception import PyRuggedError

# from helpers.compare import GeodeticComparator, planar_captor_error
# from validations.common import (
#     check_with_reference,
#     get_points_list_xml,
#     print_error,
#     print_results,
#     setup_remote_dem,
#     write_results_in_json,
# )

# from collections import namedtuple
# Data = namedtuple(
#     "Data",
#     [
#         "product_class",  # Set the product_class between S2MSILegacyGeometry (Legacybased implementation ) and
#         # S2MSIGeometry (refactored implementation)
#         "interface_path",  # Set path to xml file from legacy S2GEO_Inpu_interface.xml describing all inputs
#         "altitude",  # Set a constant altitude
#         "ref_data_path",  # Set path to the file containing references to be compared to
#         "ref_footprint_path",  # Set path to the folder containing reference footprints
#         "config_dump_path",
#         "ref_script_path",
#         "line_count_margin",
#         "isInverseLocation",  # Either doiing inverse location grids, either doing 9 points
#         # per band/detector with direct/inverse loc/ sun angles and footprint generation
#         "steps",  # Set list of steps if not in inverse location grid mode
#     ],
# )
# S2MSI_TDS1_L0c_DEM_LEGACY = Data(
#     S2MSILegacyGeometry,
#     osp.join(FOLDER, "S2MSI_TDS1/L0c_DEM_Legacy_S2GEO_Input_interface.xml"),
#     None,
#     osp.join(FOLDER, "S2MSI_TDS1/L0c_DEM_Legacy_s2geo_reference.txt"),
#     osp.join(FOLDER, "S2MSI_TDS1/L0c_DEM_Legacy_FOOTPRINTS"),
#     None,
#     None,
#     None,
#     False,
#     {
#         "direct_location": True,
#         "inverse_location": True,
#         "sun_angles": True,
#         "incidence_angles": True,
#         "footprint": True,
#     },
# )



def get_position_granule(xml_path):
    tree = ET.parse(xml_path)
    myroot = tree.getroot()
    return int(myroot.find(".//POSITION").text)

def get_geotransfrom(img_path):
    data = gdal.Open(img_path, gdalconst.GA_ReadOnly)
    geoTransform = data.GetGeoTransform()
    minx = geoTransform[0]
    maxy = geoTransform[3]
    maxx = minx + geoTransform[1] * data.RasterXSize
    miny = maxy + geoTransform[5] * data.RasterYSize
    resx = geoTransform[1]
    resy = geoTransform[5]
    return minx, miny, resx, resy, maxx, maxy

def get_resolution(img_path):
    band = img_path.split("_")[-1].split(".")[0]
    if band in ["B01", "B09", "B10"]:
        return 60
    elif band in ["B02", "B03", "B04", "B08"]:
        return 10
    else:
        return 20

WGS84_EARTH_EQUATORIAL_RADIUS = 6378137.0

def distance(s_lat: float, s_lng: float, e_lat: float, e_lng: float, degrees: bool = True):
    """
    The function calculates the distance between two points on the Earth's surface given their latitude and longitude
    coordinates.

    :param s_lat: The starting latitude of the location
    :param s_lng: The starting longitude coordinate
    :param e_lat: The latitude of the end point
    :param e_lng: The longitude of the end point
    :param degrees: deg2rad conversion needed
    :return distance (meters)
    """

    if degrees:
        s_lat = np.deg2rad(s_lat)
        s_lng = np.deg2rad(s_lng)
        e_lat = np.deg2rad(e_lat)
        e_lng = np.deg2rad(e_lng)

    dlat = np.sin((e_lat - s_lat) / 2)
    dlat_meters = 2 * WGS84_EARTH_EQUATORIAL_RADIUS * np.arcsin(dlat)
    dlon = np.cos(s_lat) * np.cos(e_lat) * np.sin((e_lng - s_lng) / 2) ** 2
    dlon_meters = 2 * WGS84_EARTH_EQUATORIAL_RADIUS * np.arcsin(np.sqrt(dlon))
    d = np.sin((e_lat - s_lat) / 2) ** 2 + np.cos(s_lat) * np.cos(e_lat) * np.sin((e_lng - s_lng) / 2) ** 2
    d_meters = 2 * WGS84_EARTH_EQUATORIAL_RADIUS * np.arcsin(np.sqrt(d))
    if isinstance(dlon_meters, np.float64):
        if e_lng < s_lng:
            dlon_meters = -dlon_meters

    else:  # todo
        for i in range(len(dlon_meters)):
            if e_lng[i] < s_lng[i]:
                dlon_meters[i] = -dlon_meters[i]

    return dlat_meters, dlon_meters, d_meters

def general_test_sentinel2_msi():
    """
    Test steps:
    - Read the S2geo interface file using the Asgard legacy loader into a Python dict (json format).
    - Read the Python dict into an Asgard S2 MSI product that uses the Java/JCC/Sxgeo bindings.
    - Run direct and inverse locations and compare results to reference.

    :param class data: ..
    :param dict thresholds: ..
    """

    output_dir = "/DATA/sen2vm-core/src/test/resources/TDS1_madeire/L1B_reducedSize"

    # S2geo interface file -> Python dict
    # config = S2LegacyDriver(
    #     "/DATA/sen2vm-core/src/test/resources/TDS1_madeire/L1B_reducedSize/DATASTRIP/S2A_OPER_MSI_L1B_DS_DPRM_20140630T140000_S20200816T120220_N05.00/S2A_OPER_MTD_L1B_DS_DPRM_20140630T140000_S20200816T120220.xml",
    #     "/DATA/sen2vm-core/src/test/resources/TDS1_madeire/L1B_reducedSize/AUX_DATA/IERS/S2__OPER_AUX_UT1UTC_PDMC_20190725T000000_V20190726T000000_20200725T000000.txt",
    #     "/DATA/sen2vm-core/src/test/resources/DEM_GEOID/S2__OPER_DEM_GEOIDF_MPC__20200112T130120_S20190507T000000.gtx",
    #     "/DATA/sen2vm-core/src/test/resources/DEM",
    #     "/DATA/sen2vm-core/src/test/resources/TDS1_madeire/L1B_reducedSize/AUX_DATA/GIPP/").read()

    config = S2LegacyDriver(
        "/home/aburie/Projets/S2_MPC/Sen2VM/sen2vm-core/src/test/resources/tests/input/TDS1/L1B_all/", #L1B
        "/home/aburie/Projets/S2_MPC/Sen2VM/sen2vm-core/src/test/resources/tests/input/TDS1/inputs/IERS/S2__OPER_AUX_UT1UTC_PDMC_20190725T000000_V20190726T000000_20200725T000000.txt", #IERS
        "/home/aburie/Projets/S2_MPC/Sen2VM/sen2vm-core/src/test/resources/DEM_GEOID/S2__OPER_DEM_GEOIDF_MPC__20200112T130120_S20190507T000000.gtx", #GEOID
        "/home/aburie/Projets/S2_MPC/Sen2VM/sen2vm-core/src/test/resources/DEM/", #DEM
        "/home/aburie/Projets/S2_MPC/Sen2VM/sen2vm-core/src/test/resources/tests/input/TDS1/inputs/GIPP/", #GIPP
        ).read()
    direct_loc = False

    print("#############")
    # # Estimate the line counts with the user given margin in seconds
    # if data.line_count_margin is not None:
    #     config["line_counts"] = S2MSIGeometry.estimate_line_counts(config, data.line_count_margin)

    # Python dict -> Asgard S2 product
    product = S2MSILegacyGeometry(**config)
    # product_asgard = S2MSIGeometry(**config)
    # comp = GeodeticComparator(product_asgard.propagation_model.body)

    error_log = ""

    # import pandas as pd
    # df = pd.read_csv('/home/mbouchet/Documents/Sen2VM/asgard/res.csv')
    list_band_diff_res = [S2Band.VALUES[0], S2Band.VALUES[4], S2Band.VALUES[1]]

    print("BXX/DXX) Erreur directloc(0,0) = (dist lat, dist lon, abs dist total) entre sen2vm - asgard")
    print("si dist lat > 0 : asgard au dessus de sen2vm")
    print("si dist lon < 0 : asgard à gauche de sen2vm")

    for band in list_band_diff_res[:1]:

        for detector in S2Detector.VALUES[:1]:
            # Test sensor from its Asgard name as defined in S2MSIGeometry
            sensor_s2 = S2Sensor(detector, band)
            sensor = sensor_s2.name
            grounds_test, acq_times = product.direct_loc(np.array([[0.0, 0.0]]), sensor)
            print("#", sensor_s2.name)
            print("grounds_test at 0.0, 0.0 =", grounds_test[0][0])
            print("grounds_test at 0.0, 0.0 =", grounds_test[0][1])
            print("grounds_test at 0.0, 0.0 =", grounds_test[0][2])
            grounds_test, acq_times = product.direct_loc(np.array([[200.5, 250.5]]), sensor)
            print("#", sensor_s2.name)
            print("grounds_test at 0.0, 0.0 =", grounds_test[0][0])
            print("grounds_test at 0.0, 0.0 =", grounds_test[0][1])
            print("grounds_test at 0.0, 0.0 =", grounds_test[0][2])

            inverse_pixels = product.inverse_loc(np.array([[grounds_test[0][0], grounds_test[0][1], grounds_test[0][2]]]), geometric_unit=sensor)
            print("sensor_test at 1.0 0.0", inverse_pixels[0][0])
            print("sensor_test at 1.0 0.0", inverse_pixels[0][1])

            if direct_loc:
                list_granules = glob.glob(op.join(output_dir, "GRANULE", "S2A_OPER_MSI_L1B_GR_DPRM_20140630T140000_S20200816T120226_D01_N05*"))  # "*"  + sensor[4:] + "*"))

                for granule in list_granules[:1]:
                    xml_path = glob.glob(op.join(granule, "*.xml"))[0]
                    position = get_position_granule(xml_path)
                    geo_grid = glob.glob(op.join(granule, "GEO_DATA", "*" + sensor[0:3] + ".tif"))[0]
                    res = get_resolution(geo_grid)

                    minx, maxy, resx, resy, maxx, miny = get_geotransfrom(geo_grid)
                    start_y = (position - 1) / (res / 10) + resy / 2 + 0.5
                    start_x = resx / 2 - 0.5

                    # Calculate 9 pixel coordinates = edges and centers
                    pixels = np.array(
                        [[col, row] for row in np.arange(start_y + miny, start_y + maxy, resy) for col in
                         np.arange(start_x + minx, start_x + maxx, resx)],
                        np.float64,
                    )

                    print("range y: (", start_y + miny, ',', start_y + maxy, ',', resy, ") ",
                          "range x: (", start_x + minx, ',', start_x + maxx, ',', resx, ") ")
                    grounds = []

                    # Call direction location
                    is_in_error = False
                    try:
                        grounds, acq_times = product.direct_loc(pixels, sensor)
                    except PyRuggedError as exp:
                        is_in_error = True
                        print(colored("    ! PyRuggedError: " + str(exp), "red"))
                        error_log += "   ! PyRuggedError: " + sensor + " [direct_loc]: " + str(exp) + "\n"

                    if is_in_error:
                        print(colored("  Operation after DirectLocation are skipped.", "yellow"))
                        continue


                    dataset = gdal.Open(geo_grid, gdalconst.GA_ReadOnly)
                    longitude_ref = np.array(dataset.GetRasterBand(1).ReadAsArray()).flatten()
                    latitude_ref = np.array(dataset.GetRasterBand(2).ReadAsArray()).flatten()
                    altitude_ref = grounds[:, 2]  # TODO
                    ref = np.stack((longitude_ref, latitude_ref, altitude_ref), axis=1)

                    # error_2d = comp.planar_error(np.array(ref), np.array(grounds))
                    error_2d = distance(np.array(ref), np.array(grounds))
                    print("error_2d =", np.mean(error_2d), "(std =", np.std(error_2d), ") \n")


    # end of loops over sensors

    # Print results
    print("")
    return error_log

general_test_sentinel2_msi()

# def inverse_loc_grid_test_sentinel2_msi(
#         product_class: Type,
#         interface_path: str,
#         ref_data_path: str,
#         line_count_margin: int = None,
#         thresholds: dict = None,
# ):
#     """
#     Test steps:
#     - Read the S2geo interface file using the Asgard legacy loader into a Python dict (json format).
#     - Read the Python dict into an Asgard S2 MSI product that uses the Java/JCC/Sxgeo bindings.
#     - Run inverse locations and compare results to reference.

#     :param class product_class: S2MSILegacyGeometry (legacy-base) ; S2MSIGeometry (refactored)
#     :param str interface_path: Path to the 'S2GEO_Input_interface.xml' interface file
#     :param str ref_data_path: Path to the txt file that contains the S2Geo reference results.
#     Asgard S2 MSI processing is run and results are compared to the reference.
#     :param int line_count_margin: margin in seconds when estimating the line counts from min/max
#     dates without granule information.
#     :param dict thresholds: ...
#     """
#     # S2geo interface file -> Python dict
#     config = S2geoInterface(interface_path).read()

#     # Estimate the line counts with the user given margin in seconds
#     if line_count_margin is not None:
#         config["line_counts"] = S2MSIGeometry.estimate_line_counts(config, line_count_margin)

#     # Python dict -> Asgard S2 product
#     product = product_class(**config)

#     # Initialize results structures
#     value_diffs_vs_ref = {"inverse_loc": np.array([])}
#     list_diffs_vs_ref = {"inverse_loc": np.array([])}
#     error_log = ""

#     # Ground reference results
#     ref_data = None

#     # Read the ground refererence results (as a Python script) obtained with S2Geo (or None to
#     # use the DEM)
#     if Path(ref_data_path).exists():
#         ref_data = defaultdict(lambda: defaultdict(list))
#         print("Read reference operation text file " + osp.realpath(ref_data_path))
#         print("This might take a while as the parsed file is big.")

#         with open(ref_data_path, "rb") as fhd:
#             ref_data = pickle.load(fhd)
#             ref_time = ref_data["ref_time"]
#             print("ref_time read from pkl file:" + ref_time)
#     else:
#         print(
#             colored(
#                 ref_data_path
#                 + ": is supposed to be a pkl file containing the dictionnary as it defines the list of points",
#                 "red",
#             )
#         )
#         return ""

#     # Test each S2 detector and band
#     index = 1
#     total = len(S2Detector.VALUES) * len(S2Band.VALUES)
#     for detector in S2Detector.VALUES:
#         for band in S2Band.VALUES:
#             # Test sensor from its Asgard name as defined in S2MSIGeometry
#             sensor = S2Sensor(detector, band).name

#             print(f"Sensor {sensor!r} #{index}/{total}")
#             index += 1
#             assert product.coordinates[sensor] == product.coordinates[sensor]

#             if sensor in ref_data.keys() and np.array(ref_data[sensor]["inverse_loc"]).size != 0:
#                 print("    Number of inverse locations: " + str(np.array(ref_data[sensor]["inverse_loc"]).size))
#             else:
#                 continue

#             # Inverse location method from the Asgard product takes diiferent inputs depending on product type
#             # The legacy implementation takes (lon,lat,alt) arrays.
#             # The Asgard implementation takes (lon,lat) arrays.
#             if product_class == S2MSIGeometry:
#                 pixels = np.array(ref_data[sensor]["inverse_loc"])[:, :2]
#             else:
#                 pixels = np.array(ref_data[sensor]["inverse_loc"])[:, :3]

#             # Call inverse location method
#             print("    Compute inverse loc grid.")
#             inverse_pixels = None
#             perf = time.perf_counter()
#             try:
#                 inverse_pixels = product.inverse_loc(pixels, geometric_unit=sensor)
#             except AttributeError as exp:
#                 print(colored(f"  ! AttributError({type(exp)=}): inverse loc " + str(exp), "red"))
#                 error_log += f"  ! AttributError({type(exp)=}): inverse loc " + str(exp) + "\n"
#                 list_diffs_vs_ref["inverse_loc"] = np.concatenate((list_diffs_vs_ref["inverse_loc"], [sensor]))
#                 value_diffs_vs_ref["inverse_loc"] = np.concatenate((value_diffs_vs_ref["inverse_loc"], [float("nan")]))
#             print("      time ~ " + str(time.perf_counter() - perf) + "s")

#             # Inverse Location errors
#             error_invloc = (
#                     planar_captor_error(np.array(ref_data[sensor]["inverse_loc"])[:, 3:], inverse_pixels)
#                     / band.pixel_height
#             )
#             # Divide by resolution to have distance in pixel
#             # regardless of the resolution of the band

#             list_diffs_vs_ref, value_diffs_vs_ref = print_error(
#                 sensor,
#                 "inverse_loc",
#                 thresholds,
#                 error_invloc,
#                 list_diffs_vs_ref,
#                 value_diffs_vs_ref,
#                 precision=3,
#                 inverse=True,
#             )

#     return "There was an error during the nominal execution of this test"
