#!/usr/bin/env python
# coding: utf8
#
# Copyright (c) 2024 CSGROUP
#

import logging

from argparse import ArgumentParser
from pathlib import Path
from typing import Tuple, Optional
import numpy as np
import rasterio as rio
import utm
from rasterio import crs, warp
from rasterio.transform import Affine
from scipy.interpolate import griddata

def coordinates_conversion(coords, epsg_in, epsg_out):
    """
    Convert coords from a SRS to another one.
    :param coords: coords to project
    :type coords: numpy array of 2D coords  (shape  (2,) or (N,2) or 3D coords (shape  (3,) or (N,3))
    :param epsg_in: EPSG code of the input SRS
    :type epsg_in: int
    :param epsg_out: EPSG code of the output SRS
    :type epsg_out: int
    :returns: converted coordinates
    :rtype: numpy array of 2D coord (N,2) or 3D coords (N,3)
    """
    srs_in = crs.CRS.from_epsg(epsg_in)
    srs_out = crs.CRS.from_epsg(epsg_out)
    if (coords.size / 3 == 1 or coords.size / 2 == 1) and (coords.ndim == 1):
        coords = coords[np.newaxis, :]
    alti = None
    if coords.shape[1] == 3:
        alti = coords[:, 2]
    coords = np.array(warp.transform(srs_in, srs_out, coords[:, 0], coords[:, 1], alti))
    coords = coords.transpose()
    return coords

def transform_from_tags(tags):
    line_offset = float(tags['LINE_OFFSET'])
    line_step = float(tags['LINE_STEP'])
    pixel_offset= float(tags['PIXEL_OFFSET'])
    pixel_step = float(tags['PIXEL_STEP'])
    return Affine.from_gdal(pixel_offset,pixel_step,0,line_offset,0,line_step)

def resample():
    return 0

def write_grid(grid, geotransform, epsg,grid_name):

    coord_ref= crs.CRS.from_epsg(epsg)
    height, width, _ = grid.shape
    with rio.open(
        grid_name, "w", driver="GTiff", nodata=-32768,crs = coord_ref,dtype=np.float64, width=width, height=height, count=2, transform=geotransform
    ) as source_ds:
        source_ds.write(grid[:, :, 0], 1)
        source_ds.write(grid[:, :, 1], 2)
    return 0


def grid_sampling(transform,width,height):
    [ori_col, ori_row] = transform * (0.5, 0.5)  # center pixel position
    step_row = transform[4]
    step_col = transform[0]

    # print("ori {} {} step {} {}".format(ori_col,ori_y,step_x,step_y))
    last_col = ori_col + step_col * width
    last_row = ori_row + step_row * height

    cols = np.arange(ori_col, last_col, step_col)
    rows = np.arange(ori_row, last_row, step_row)

    grid_row, grid_col = np.mgrid[ori_col:last_col:step_col, ori_row:last_row:step_row]
    return grid_row, grid_col


def create_regular_grid(
    grid: np.ndarray,
    resolution: float,
    ullr: Optional[Tuple[float, float, float, float]] = None,
    epsg : int = None, round = 3
) -> Tuple[np.ndarray, Tuple[float, float, float, float, float, float], int]:
    """
    Creates an inverse regular grid from a direct location grid.

    :param grid: A numpy array of shape (row, col, 2) where the first channel
                 represents longitudes and the second channel represents latitudes.
    :param resolution: The desired resolution for the regular grid.
    :param ullr: Optional. A tuple (x1, y1, x2, y2) representing the coordinates of the upper-left (x1, y1)
                 and lower-right (x2, y2) corners of the regular grid domain. If None, the domain will be
                 inferred from the input grid.
    :param epsg: int EPSG code

    :return: A tuple containing:
             - A numpy array representing the generated inverse regular grid with the same structure
               as the input grid, but regular (row, col, 2).
             - A geotransform tuple (x_origin, pixel_width, x_rotation, y_origin, y_rotation, pixel_height)
               used to map the grid back to its spatial coordinates.
            - espg code of output

    """
    # Implementation of the function goes here

    # grid opening

    dataset_grid = rio.open(grid)
    grid_data = dataset_grid.read()
    epsg_in = int(dataset_grid.tags()['SRS'].split(':')[1])
    transform = transform_from_tags(dataset_grid.tags())


    logging.info(f" epsg in {epsg_in}")

    min_values = np.min(grid_data, axis=(1,2))
    max_values = np.max(grid_data, axis=(1,2))
    logging.info(f"min lon {min_values[0]} min lat {min_values[1]}")
    logging.info(f"max lon {max_values[0]} max lat {max_values[1]}")


    if epsg is None:
        logging.info(" grid CRS will be UTM")
        # TODO anti meridian case
        mean_lon = (min_values[0] + max_values[0])/2
        mean_lat = (min_values[1] + max_values[1])/2
        zone = utm.from_latlon(mean_lat,mean_lon)[2]
        epsg = 32000 + zone + 600 + 100*(mean_lat<0)
        logging.info(f"epsg code {epsg}")
    else:
        if ullr is not None:
            raise Exception("ullr with empty crs is note allowed")

    # values conversion
    coords = np.array((grid_data[0,:,:].flatten(),grid_data[1,:,:].flatten())).transpose()

    coords_epsg_out = coordinates_conversion(coords, epsg_in, epsg)

    grid_row, grid_col = grid_sampling(transform,dataset_grid.width,dataset_grid.height)
    # create griddata
    points = np.array((grid_row.transpose().flatten(),grid_col.transpose().flatten())).transpose()

    # output grid definition
    if ullr is None:
        min_epsg_out = coordinates_conversion(min_values, epsg_in, epsg)
        max_epsg_out = coordinates_conversion(max_values, epsg_in, epsg)
        rounding = 10 ** round
        upper_left = rounding * np.floor(min_epsg_out/rounding)[0]
        lower_right = rounding * np.floor(max_epsg_out/rounding)[0]
    else:
        upper_left = ullr[0:2]
        lower_right = ullr[2:4]

    print(f"resolution {resolution}")
    width_out = np.ceil((lower_right[0] - upper_left[0])/resolution)
    height_out = np.ceil((lower_right[1] - upper_left[1])/resolution)

    logging.info(f" origin {upper_left[0]} {lower_right[1]}")
    logging.info(f" width {width_out} height {height_out}")

    transform_out = Affine.from_gdal(upper_left[0], resolution, 0, lower_right[1], 0, -resolution)

    grid_row_out, grid_col_out = grid_sampling(transform_out,width_out,height_out)

    out_coords = griddata(coords_epsg_out, points, (grid_row_out.transpose(),grid_col_out.transpose()), method='linear', fill_value=-32768, rescale=False)
    # resample
    return out_coords,transform_out, epsg

def invloc_grid_parser():
    parser = ArgumentParser()
    parser.add_argument("grid", help="geolocation grid", type=str)
    parser.add_argument("resolution", help="resampling resolution", type=float, default=45.0)
    parser.add_argument("out_file", help="resampled image", type=str)
    parser.add_argument("--ullr", nargs=4, type=float,help="resampled coordinate corners [UL, LR]")
    parser.add_argument("--epsg", type=int, help="reference system (UTM by default)")
    parser.add_argument(
        "--loglevel",
        default="INFO",
        choices=("DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"),
        help="Logger level (default: WARNING. Should be one of "
        "(DEBUG, INFO, WARNING, ERROR, CRITICAL)",
    )
    return parser

def sen2vm_invloc_from_dir_loc_grid(grid, resolution, out_file, ullr=None,epsg=None):

    if not Path(grid).exists():
        raise Exception(f"geolocation grid doesn't exist : {grid}")

    logging.info("Transform direct loc grid to regular inverse one ")
    grid, geotransform, epsg = create_regular_grid(grid, resolution,ullr,epsg)
    write_grid(grid, geotransform,epsg,out_file)

    logging.info("Resample")
    resample()


def main():
    """
    Cli entry point
    """
    parser = invloc_grid_parser()
    args = parser.parse_args()

    grid = args.grid
    resolution = args.resolution

    out_file = args.out_file
    ullr = args.ullr
    epsg = args.epsg
    logging.getLogger().setLevel(args.loglevel)

    sen2vm_invloc_from_dir_loc_grid(grid, resolution, out_file, ullr,epsg)


if __name__ == "__main__":
    main()
