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
import fiona.transform
from termcolor import colored

from math import sqrt, isnan

from asgard.sensors.sentinel2.s2_band import S2Band
from asgard.sensors.sentinel2.s2_detector import S2Detector
from asgard.sensors.sentinel2.s2_sensor import S2Sensor

#from helpers.compare import GeodeticComparator, planar_captor_error

from asgard_legacy.sensors.sentinel2.msi import S2MSILegacyGeometry

from asgard_legacy_drivers.drivers.sentinel_2_legacy import S2LegacyDriver

from pyrugged.errors.pyrugged_exception import PyRuggedError


georefConventionOffsetPixel = -0.5
georefConventionOffsetLine = 0.5

THRESHOLDS = {
    "direct": 1e-7,  # degrees
    "inverse": 1e-2  # pixel
}

#def test_Sen2VM(test_to_generate):
def test_Sen2VM():
    """
    Run general test to compare direct or inverse grids generated with SEN2VM

    :param data: list of test cases
    """

    #print(f"Test: {test_to_generate}")
    print(f"Start")


    f"""# Read configuration
    with open(os.path.join(input_dir, "configuration.json")) as json_file:
        json_data = json.load(json_file)

    # Get all needed info in the json
    l1b_product = op.join(json_data["l1b_product"])
    xml_product = glob.glob(op.join(l1b_product, "DATASTRIP", "*", "*.xml"))[0]
    iers = op.join(json_data["iers"])
    geoid = op.join( json_data["geoid"])
    dem = op.join(json_data["dem"])
    gipp = op.join( json_data["gipp_folder"])
    refining = not (json_data["deactivate_available_refining"])"""

    # Init S2geoInterface
    #config = S2geoInterfaceSen2VM(xml_product, iers, geoid, dem, gipp, refining).read()
    #config = S2LegacyDriver(xml_product, iers, geoid, dem, gipp).read()
    
    config = S2LegacyDriver(
        "/DATA/Sen2VM/MadeiraSAFE_SXGEO-0.3.1/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/DATASTRIP/S2B_OPER_MSI_L1B_DS_2BPS_20241019T153411_S20241019T120215_N05.11/S2B_OPER_MTD_L1B_DS_2BPS_20241019T153411_S20241019T120215.xml", #L1B
        "/DATA/Sen2VM/MadeiraSAFE_SXGEO-0.3.1/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/AUX_DATA/IERS/S2__OPER_AUX_UT1UTC_PDMC_20190725T000000_V20190726T000000_20241017T000000.txt", #IERS
        "/DATA/DEM_Legacy/DEM_GEOID/S2__OPER_DEM_GEOIDF_MPC__20200112T130120_S20190507T000000.gtx", #GEOID
        "/DATA/DEM_Legacy/DEM_SRTM", #DEM
        "/DATA/Sen2VM/MadeiraSAFE_SXGEO-0.3.1/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/AUX_DATA/GIPP_restricted/", #GIPP
        ).read()
    
    product = S2MSILegacyGeometry(**config)  # TOFIX

    """# Init ref dir for asgard outputs
    ref_dir = op.join(REF_DIR, test_to_generate)
    Path(ref_dir).mkdir(parents=True, exist_ok=True)"""

    # List detectors and bands to test
    #detectors, bands = read_params(input_dir)
    detectors = [S2Detector.from_name("D01")]
    bands = [S2Band.from_name("B01")]

    l1b_product = "/DATA/Sen2VM/MadeiraSAFE_SXGEO-0.3.1/S2B_MSIL1B_20241019T120219_N0511_R023_20241022T154709.SAFE/"
    ref_dir = "/home/aburie/Sen2VM/sen2vm-core/src/test/reference_generation/test_generation/"

    compute_direct_location(config, l1b_product, product, detectors, bands, ref_dir)
    """# Direct or inverse case
    if "direct" in test_to_generate.lower():
        compute_direct_location(config, l1b_product, product, detectors, bands, ref_dir)
    elif "inverse" in test_to_generate.lower():
        # Replace input directory to output_folder written in configuration file
        input_dir = json_data["inverse_location_additional_info"]["output_folder"]
        compute_inverse_location(input_dir, product, detectors, bands, ref_dir)
    else:
        print('No test found: no "direct" or "inverse" found in name test')"""


#def compute_inverse_location(input_dir: str, product, detectors, bands, ref_dir=None):
    """
    Inverse location of a data test from SEN2VM

    :param input_dir: directory with all inverse grids to verify (bands x detectors in params.json)
    :param product: S2MSILegacyGeometry (legacy-base) ; S2MSIGeometry (refactored)
    :param ref_dir: output directory for asgard grids
    :return:
    """
"""
    for band in bands:
        df = pd.DataFrame(columns=['band', 'det', "nb_valeurs not nan", "nb_errors > 10-6 m", "nb_errors > 10-4 m",
                                   "nb_errors > 10-2 m", "mean (m)", "max (m)", "std (m)", "ce95 (m)"])
        for detector in detectors:

            sensor = S2Sensor(detector, band).name
            #print("#", sensor)

            # Select corresponding grid
            geo_grid = glob.glob(op.join(input_dir, "*INV*" + sensor[4:] + "_" + sensor[0:3] + ".tif"))[0]

            # Init all grounds coordinates to compute inverse loc
            sensor = S2Sensor(detector, band).name
            print("#", sensor)
            # Select corresponding grid
            geo_grid = glob.glob(op.join(input_dir, "*INV*" + sensor[4:] + "_" + sensor[0:3] + ".tif"))[0]

            # Init all grounds coordinates to compute inverse loc

            minx_, maxy_, sizex_, sizey_, stepx_, stepy_, src_crs = get_geotransfrom(geo_grid)
            cols = [minx_ + stepx_/2 + i * stepx_ for i in range(sizex_)]
            rows = [maxy_ + stepy_/2 + i * stepy_ for i in range(sizey_)]


            grounds = np.array([[col, row] for row in rows  for col in cols  ], np.float64)
            en = np.array([[c, r] for r, row in  enumerate(rows)  for c, col in  enumerate(cols)  ], np.float64)

            # Transform them to WG84
            grounds = fiona.transform.transform(src_crs, "EPSG:4326", grounds[:, 0], grounds[:, 1])
            # note : fiona.transform.transform(src_crs, dst_crs, xs, ys)

            # Pixels need to be [[col0, col1...], [row0, row1...]] for inverse location TODO
            grounds = np.array([[grounds[0][i], grounds[1][i]] for i in range(len(grounds[0]))], np.float64)
            print (grounds.shape)
            # Call inverse location
            inverse_pixels = None
            try:
                inverse_pixels = product.inverse_loc(grounds, geometric_unit=sensor)

                # note : (col, row) conv
            except AttributeError as exp:
                print(colored("    ! PyRuggedError: " + str(exp), "red"))

            # TODO
            inverse_pixels = inverse_pixels + [-georefConventionOffsetPixel, -georefConventionOffsetLine]

            # Compare results to Sen2vm
            dataset = gdal.Open(geo_grid, gdalconst.GA_ReadOnly)
            columns_ref = np.array(dataset.GetRasterBand(1).ReadAsArray()).flatten()
            rows_ref = np.array(dataset.GetRasterBand(2).ReadAsArray()).flatten()
            ref = np.stack((columns_ref, rows_ref), axis=1)

            # Compute planar error
            # error_invloc = planar_captor_error(ref, inverse_pixels) / band.pixel_height
            error_invloc = []
            for i in range(len(ref)) :

                if not isnan(ref[i][0]):

                    diff_col = inverse_pixels[i][0] - ref[i][0]
                    sqrt_diff_col = diff_col * diff_col
                    diff_line = inverse_pixels[i][1] - ref[i][1]
                    sqrt_diff_line = diff_line * diff_line
                    sum = sqrt_diff_line + sqrt_diff_col
                    diff = sqrt(sum)
                    diff_metres = diff * band.pixel_height
                    if diff_metres > 1:

                        print(i, "input", grounds_in[i], "=>", grounds[i][0], grounds[i][1])
                        print("  asgard:", inverse_pixels[i], "vs sen2vm:", ref[i], "=> diff de", diff_metres, "m")
                    error_invloc.append(diff_metres)
                else :
                    error_invloc.append(np.nan)

            # Compute Stats
            error_invloc = np.array(error_invloc)
            nb_values = len(error_invloc[error_invloc > -1.0])

            nb_errors_threshold = len(error_invloc[error_invloc > THRESHOLDS["inverse"]])
            nb_errors_106 = len(error_invloc[error_invloc > 1e-6])
            nb_errors_104 = len(error_invloc[error_invloc > 1e-4])
            nb_errors_102 = len(error_invloc[error_invloc > 1e-2])
            mean_err = np.nanmean(error_invloc)
            max_err = np.nanmax(error_invloc)
            std_err = np.nanstd(error_invloc)
            ce95_err = np.nanpercentile(error_invloc, 95)

            # If ground truth (asgard) need to be saved
            if ref_dir:
                sensor_colomns = inverse_pixels[:, 0].reshape(len(rows), len(cols))
                sensor_rows = inverse_pixels[:, 1].reshape(len(rows), len(cols))
                sensor_pixels = [sensor_colomns, sensor_rows]

                # Same grid name file saved into ref_dir
                Path(ref_dir).mkdir(parents=True, exist_ok=True)
                output_ref_grid = op.join(ref_dir, op.basename(geo_grid))
                arrays_to_raster(output_ref_grid, sensor_pixels)
                # print("Grid save in", output_ref_grid)

            print_error(sensor, error_invloc, "inverse")
            df.loc[len(df)] = [sensor[0:3], sensor[4:], nb_values, nb_errors_106,
                               nb_errors_104, nb_errors_102, mean_err, max_err, std_err, ce95_err]

            df.to_csv(op.join(ref_dir, sensor[0:3] + "_results.csv"))
            print (df)"""



def compute_direct_location(config: dict, input_dir: str, product, detectors, bands, ref_dir=None):
    """
    Direct location of a data test from SEN2VM

    :param config:
    :param input_dir: directory with all inverse grids to verify (bands x detectors in params.json)
    :param product: dict of S2geo interface
    :param ref_dir: output directory for asgard grids
    """

    # Init comparator for planar_error
    #product_asgard = S2MSIGeometry(**config)
    #comp = GeodeticComparator(product_asgard.propagation_model.body)

    for band in bands:

        df = pd.DataFrame(columns=['band', 'det', 'name', "nb_errors > 10-8", "nb_errors > 1",
                                   "mean (m)", "median (m)", "min (m)", "max (m)", "std (m)", "ce95 (m)",
                                    "argmax (flatten list coords)", "coordinates_argmax_c (pixel)", "coordinates_argmax_r (pixel)"])

        for detector in detectors:
            sensor = S2Sensor(detector, band).name
            path_csv = glob.glob(op.join(ref_dir, sensor[0:3] + "_results.csv"))
            if len(path_csv) > 0:
                df = pd.read_csv(path_csv[0], index_col=0)
                print ("df exist", df.columns)
            else:
                print ("df not exist")
                df = pd.DataFrame(columns=['band', 'det', 'name', "nb_errors > 10-8", "nb_errors > 1","mean (m)", "median (m)", "min (m)", "max (m)", "std (m)", "ce95 (m)", "argmax (flatten list coords)", "coordinates_argmax_c (pixel)",  "coordinates_argmax_r (pixel)"])

            df_ = df.loc[df["det"] == sensor[4:]]
            if len(df_) > 0:
                print(sensor, "already computed")
                continue
            else :
                print(sensor, "to be computed")

            error_2d = np.array([])

            # Select corresponding granules

            list_granules = glob.glob(op.join(input_dir, "GRANULE", "*" + sensor[4:] + "*"))
            #list_granules = glob.glob(op.join("/home/aburie/Sen2VM/sen2vm-core/src/test/reference_generation/test_generation/GRANULE/", "*" + sensor[4:] + "*"))
            
            # list_granules = glob.glob(op.join(input_dir, "GRANULE", "S2B_OPER_MSI_L1B_GR_DPRM_20140630T140000_S20240116T154306_D06*"))

            print(f"# Sensor {sensor}")
            if len(list_granules) == 0:
                print("No granules")
                continue

            rows_detecteur_band_grid = []

            for g, granule in enumerate(list_granules):

                # Select corresponding geo grid
                geo_grid = glob.glob(op.join(granule, "GEO_DATA", "*" + sensor[0:3] + ".tif"))[0]

                # Init all sensor points to compute direct loc
                minx, maxy, sizex, sizey, stepx, stepy, src_crs = get_geotransfrom(geo_grid)


                # compute upper left in general detector grid
                start_y = stepy / 2 + georefConventionOffsetLine  # image_upper_left_y + cony = (step/2 - 0.5) + 1
                start_x = stepx / 2 + georefConventionOffsetPixel  # image_upper_left_y + conx = (step/2 - 0.5) + 0

                # Compute coordinates for the specific granule
                #rows = np.arange(start_y + miny, start_y + maxy, resy)  # from upper_left_det_y + upper_left_granule_y
                #cols = np.arange(start_x + minx, start_x + maxx, resx)  # from upper_left_det_y + upper_left_granule_y
                rows = [start_y + maxy + iy * stepy for iy in range(sizey)]
                cols = [start_x + minx + ix * stepx for ix in range(sizex)]

                # Pixels need to be [[col1, row1], [col1, row1] ...] for direct location
                pixels = np.array([[col, row] for row in rows for col in cols], np.float64)
                en = np.array([[c, r] for r, row in enumerate(rows) for c, col in enumerate(cols)], np.float64)

                # Call direction location
                grounds = []
                try:
                    grounds, _ = product.direct_loc(pixels, sensor)
                    # note: long, lat, alt conv
                except PyRuggedError as exp:
                    print(colored("    ! PyRuggedError: " + str(exp), "red"))

                # Compare results to Sen2vm
                print("GEO_GRID: ", geo_grid)
                dataset = gdal.Open(geo_grid, gdalconst.GA_ReadOnly)
                """
                print(dataset.GetRasterBand(1))
                nBands = dataset.RasterCount      # how many bands, to help you loop
                nRows  = dataset.RasterYSize      # how many rows
                nCols  = dataset.RasterXSize      # how many columns
                Band = dataset.GetRasterBand(1)
                dType = Band.DataType          # the datatype for this band
                dType = gdal.GDT_Float64
                 
                
                RowRange = range(nRows)
                for ThisRow in RowRange:
                    # read a single line from this band
                    ThisLine = Band.ReadRaster(0,ThisRow,nCols,1,nCols,1,dType)
                    print("ThisLine", ThisLine)
                    import struct
                    print(struct.unpack('>f', ThisLine[:4]))"""
                    
                import rasterio
                image = rasterio.open(geo_grid)
                
                band = image.read(1)
                
                longitude_test = np.array(image.read(1)).flatten()
                latitude_test = np.array(image.read(2)).flatten()
                if dataset.RasterCount == 3:
                    altitude_test = np.array(image.read(3)).flatten()
                else:
                    # if altitude not saved in grid, take direct altitude from ground truth (asgard)
                    altitude_test = grounds[:, 2]


                ref = np.stack((longitude_test, latitude_test, altitude_test), axis=1)
                #error_2d_g = np.array(comp.planar_error(ref, np.array(grounds)))
                print("ref1: ",np.array(ref[:,1]))
                print("ground1: ",np.array(grounds[:,1]))
                error_2d_g = np.array(distance(latitude_test, longitude_test, np.array(grounds[:,1]),np.array(grounds[:,0])))[2]
                print("error_2d: ",error_2d_g)
                if error_2d_g[np.nanargmax(error_2d_g)] > 0.0002 :
                    #print ("pixel before direct loc (" + str(pixels[np.nanargmax(error_2d_g)][1])+","+ str([np.nanargmax(error_2d_g)][0]),")")
                    #print("   asgard:", grounds[np.nanargmax(error_2d_g)], "vs sen2vm:", ref[np.nanargmax(error_2d_g)])
                    #print("   avec diff =", error_2d_g[np.nanargmax(error_2d_g)], "m")
                    print("error max: ", grounds[np.nanargmax(error_2d_g)], ref[np.nanargmax(error_2d_g)], error_2d_g[np.nanargmax(error_2d_g)])
                    print("errors (nb=", len(error_2d_g[error_2d_g > 0.0002]), "/", len(error_2d_g) ,"): ", error_2d_g[error_2d_g > 0.0002][::10])
                df = add_direct_errors_infos_dataframe(sensor[0:3], sensor[4:], granule, error_2d_g, pixels, df)

                # If ground truth (asgard) need to be saved
                if ref_dir:
                    lat = grounds[:, 0].reshape(len(rows), len(cols))
                    lon = grounds[:, 1].reshape(len(rows), len(cols))
                    alt = grounds[:, 2].reshape(len(rows), len(cols))
                    grounds = [lat, lon, alt]

                    # Same grid name file saved into ref_dir
                    geo_grid_dir = op.join(ref_dir, "GRANULE", op.basename(granule), "GEO_DATA")
                    Path(geo_grid_dir).mkdir(parents=True, exist_ok=True)
                    output_ref_grid = op.join(geo_grid_dir, op.basename(geo_grid))
                    
                    #TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script
                    print()
                    print()
                    print()
                    print()
                    print()
                    print("TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script.")
                    print("TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script.")
                    print("TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script.")
                    print("TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script.")
                    print("TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script.")
                    print("TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script.")
                    print("TODO, change writing of output grids (after validation that input grids are equivalent to the ones generated by this script.")
                    # arrays_to_raster(output_ref_grid, grounds)
                    # print(f"Granule {g} save in {output_ref_grid}")


                intersection = np.intersect1d(rows_detecteur_band_grid, rows)
                if rows[0] in intersection:
                    error_2d_g = error_2d_g[len(cols):]
                if rows[len(rows) -1] in intersection:
                    error_2d_g = error_2d_g[:len(error_2d_g) - len(cols)]

                error_2d = np.concatenate((error_2d, error_2d_g))
            df = add_direct_errors_infos_dataframe(sensor[0:3], sensor[4:], "all", error_2d, [], df)
            print_error(sensor, error_2d, "direct")
            print (df.columns)
            df.to_csv(op.join(ref_dir, sensor[0:3] + "_results.csv"))
        print (df.loc[df["name"] == "all"])



def add_direct_errors_infos_dataframe(band, det, name, errors, pixels, df):
    mean_err = np.nanmean(errors)
    median_err = np.nanmedian(errors)
    min_err = np.nanmin(errors)
    max_err = np.nanmax(errors)
    std_err = np.nanstd(errors)
    argmax_err = np.nanargmax(errors)
    ce95_err = np.nanpercentile(errors, 95)
    if name != "all" :
        coordinates_argmax_c = pixels[np.nanargmax(errors)][0]
        coordinates_argmax_r = pixels[np.nanargmax(errors)][1]
    else :
        coordinates_argmax_c = -1
        coordinates_argmax_r = -1

    if max_err > 0.01:
        print(max_err, argmax_err, coordinates_argmax_c, coordinates_argmax_r)
    nb_error = len(errors[errors > THRESHOLDS["direct"]])
    nb_error_1 = len(errors[errors > 1])
    df.loc[len(df)] = [band, det, name, nb_error, nb_error_1,
                       mean_err, median_err, min_err, max_err, std_err, ce95_err,
                       argmax_err, coordinates_argmax_c, coordinates_argmax_r]
    return df


def get_geotransfrom(img_path):
    """
    Simplier GeoTransform
    :param img_path: the input image
    :return: minx, miny, resx, resy, maxx, maxy, src_crs
    """
    data = gdal.Open(img_path, gdalconst.GA_ReadOnly)
    geoTransform = data.GetGeoTransform()
    minx = geoTransform[0]
    maxy = geoTransform[3]
    sizex = data.RasterXSize
    sizey = data.RasterYSize
    stepx = geoTransform[1]
    stepy = geoTransform[5]
    src_crs = data.GetProjectionRef()
    return minx, maxy, sizex, sizey, stepx, stepy, src_crs



def arrays_to_raster(dst_filename, arrays):
    """
    Write arrays in dst_filename

    :param dst_filename: the output file (gTiff)
    :param arrays: the array to write xnBand x nbX x nbY
    """
    array = arrays[0]
    x_pixels = array.shape[1]  # number of pixels in x
    y_pixels = array.shape[0]  # number of pixels in y
    PIXEL_SIZE = 1  # size of the pixel
    x_min = 0  # x_min ("top left" corner)
    y_max = array.shape[0]  # y_max ("top left")
    wkt_projection = 'a projection in wkt that you got from other file'  # todo
    driver = gdal.GetDriverByName('GTiff')
    dataset = driver.Create(dst_filename, x_pixels, y_pixels, len(arrays), gdal.GDT_Float64, )
    dataset.SetGeoTransform((x_min, PIXEL_SIZE, 0, y_max, 0, -PIXEL_SIZE))
    dataset.SetProjection(wkt_projection)
    for b, array in enumerate(arrays):
        dataset.GetRasterBand(b + 1).WriteArray(array)
    dataset.FlushCache()  # Write to disk.


def read_params(input_test: str):
    """
    Read bands and detectors listed in params.json
    :param input_test: test directory with params.json file
    :return: detectors: list of S2Detector, bands: list of S2Band
    """
    with open(os.path.join(input_test, "params.json")) as json_file:
        params = json.load(json_file)
    detectors = [S2Detector.from_name("D" + name) for name in params["detectors"]]
    bands = [S2Band.from_name(name) for name in params["bands"]]
    return detectors, bands


def print_error(sensor, error, type):
    """
    Function to format and print error

    :param sensor: sensor name
    :param error: errors values
    :param type: direct or inverse
    """

    # unit = "pixels" if type == "inverse" else "metres"
    unit = "metres"
    if len(error) and np.sum(~np.isnan(error)):
        nb = np.sum(~np.isnan(error))
        max_error = np.nanmax(error)
        std_error = np.nanstd(error)
        ce95_error = np.nanpercentile(error, 95)
        if np.nanmax(error) > THRESHOLDS[type]:
            msg = f"      ! {sensor} ({nb} values): Max error value superior for {sensor}:"
            msg = msg + f"{max_error} {unit} (ce95={ce95_error})"
            print(colored(msg, "red"))
        else:
            print(f"{sensor} ({nb} values): max={max_error} {unit} (ce95={ce95_error})")
    else :
        print(f"{sensor} (no value)")
        
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

test_Sen2VM()