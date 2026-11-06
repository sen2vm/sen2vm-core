#!/usr/bin/env python
# coding: utf8
#
# Copyright 2022 CS GROUP
# Licensed to CS GROUP (CS) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# CS licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""
Module for Sentinel 2 instruments
"""

import os.path as osp
from typing import Dict, List, Tuple

# asgard imports
import asgard.sensors.sentinel2.s2_constants as S2C
import numpy as np

# Force import of sxgeo first
import sxgeo  # pylint: disable=unused-import # noqa: F401
from asgard.core.math import (  # pylint: disable=ungrouped-imports
    flatten_array,
    restore_array,
)
from asgard.core.product import (  # pylint: disable=ungrouped-imports
    AbstractOpticalGeometry,
)
from asgard.sensors.sentinel2.msi import S2MSIGeometry as RefactoredMSI
from asgard.sensors.sentinel2.s2_band import S2Band
from asgard.sensors.sentinel2.s2_detector import S2Detector
from asgard.sensors.sentinel2.s2_sensor import S2Sensor

# This import provides the initVM()
from asgard.wrappers.orekit.utils import (
    dates_from_json,
    get_data_context,
    get_orekit_resources,
)

# Import java/orekit/sxgeo classes
from org.hipparchus.analysis.polynomials import (  # pylint: disable=import-error, wrong-import-order
    PolynomialFunction,
)
from org.hipparchus.geometry.euclidean.threed import (  # pylint: disable=import-error, wrong-import-order
    Rotation,
    Vector3D,
)
from org.orekit.data import (  # pylint: disable=import-error, wrong-import-order
    DataContext,
)
from org.orekit.rugged.linesensor import (  # pylint: disable=import-error, wrong-import-order
    LinearLineDatation,
)
from org.orekit.time import (  # pylint: disable=import-error, wrong-import-order
    AbsoluteDate,
)
from org.orekit.utils import (  # pylint: disable=import-error, wrong-import-order
    TimeStampedAngularCoordinates,
    TimeStampedPVCoordinates,
)
from org.sxgeo.engine import (  # pylint: disable=import-error, wrong-import-order
    IncidenceAnglesEngine,
    SimpleLocEngine,
    SunAnglesEngine,
)
from org.sxgeo.input.datamodels import (  # pylint: disable=import-error, wrong-import-order
    DataSensingInfos,
    RefiningInfo,
)
from org.sxgeo.input.datamodels.sensor import (  # pylint: disable=import-error, wrong-import-order
    Sensor,
    SensorViewingDirection,
    SpaceCraftModelTransformation,
)
from org.sxgeo.input.dem import (  # pylint: disable=import-error, wrong-import-order
    DemGlobeFileManager,
    DemManager,
    GeoidManager,
    SrtmFileManager,
)
from org.sxgeo.rugged import (  # pylint: disable=import-error, wrong-import-order
    RuggedManager,
)

from asgard_legacy.sensors.sentinel2.s2_spacecraft import S2SpacecraftTransform


class S2MSILegacyGeometry(AbstractOpticalGeometry):  # pylint: disable=too-many-instance-attributes
    """
    Sentinel 2 MSI legacy product.
    """

    # Same methods than in S2MSIGeometry
    _build_min_max_lines = RefactoredMSI._build_min_max_lines  # pylint: disable=protected-access

    def __init__(self, *args, **kwargs):
        """
        Constructor.

        :param j_ref_date: reference date from the first sensor line datation
        :param sensors: jcc/sxgeo Sensor instance for each sensor ASGARD name
        :param rugged_manager: jcc/sxgeo RuggedManager instance
        :param loc_engine: jcc/sxgeo SimpleLocEngine instance
        :param sun_engine: jcc/sxgeo SunAnglesEngine instance
        :param incidence_engine: jcc/sxgeo IncidenceAnglesEngine instance
        """

        # call superclass constructor
        super().__init__(*args, **kwargs)

        self.j_ref_date: AbsoluteDate = None
        self.sensors: Dict[str, Sensor] = {}
        self.rugged_manager: RuggedManager = None
        self.loc_engine: SimpleLocEngine = None
        self.sun_engine: SunAnglesEngine = None
        self.incidence_engine: IncidenceAnglesEngine = None

        #######################
        # Read file resources #
        #######################

        j_tile_updater = self._read_file_resources()

        #######################
        # Read line datations #
        #######################

        line_datations = self._read_line_datations()

        ########################
        # Read attitude values #
        ########################

        d_attitudes = self.config["attitudes"]
        dates = dates_from_json(d_attitudes, "GPS")
        quaternions = d_attitudes["quaternions"]

        # One time by quaternion
        assert len(dates) == len(quaternions)

        # List of satellite angular Q (quaternion) values read from the attitudes
        q_coords = [
            TimeStampedAngularCoordinates(
                time,
                Rotation(
                    float(quat[3]),
                    float(quat[0]),
                    float(quat[1]),
                    float(quat[2]),
                    False,
                ),  # no normalization
                Vector3D.ZERO,
                Vector3D.ZERO,
            )
            for time, quat in zip(dates, quaternions)
        ]

        #####################
        # Read orbit values #
        #####################

        d_orbits = self.config["orbits"]
        dates = dates_from_json(d_orbits, "GPS")
        positions = d_orbits["positions"]
        velocities = d_orbits["velocities"]

        # One time by position/velocity
        assert len(dates) == len(positions) == len(velocities)

        # List of satellite PV (position velocity) values read from the ephemeris
        pv_coords = [
            TimeStampedPVCoordinates(
                time,
                Vector3D(*position.tolist()),
                Vector3D(*velocity.tolist()),
                Vector3D.ZERO,
            )
            for time, position, velocity in zip(dates, positions, velocities)
        ]

        ######################
        # Read min/max lines #
        ######################

        min_lines, max_lines = self._build_min_max_lines()

        self._instr_list = list(self.coordinates.keys())

        ###########################
        # Read viewing directions #
        ###########################

        viewing_directions = {}
        for d_viewing in self.config["viewing_directions"]:
            values = d_viewing["values"]
            sensor_name = d_viewing["sensor"]
            viewing_directions[sensor_name] = SensorViewingDirection(
                len(values[0]), values[0].array_list(), values[1].array_list()
            )
            self.coordinates[sensor_name]["pixels"] = viewing_directions[sensor_name].getNbPixels()

        ####################################
        # Read spacecraft model parameters #
        ####################################

        d_spacecraft = self.config["spacecraft"]
        j_piloting_to_msi = S2SpacecraftTransform.from_json(
            "piloting_to_msi", d_spacecraft, "java"
        )
        assert isinstance(j_piloting_to_msi, SpaceCraftModelTransformation)
        msi_to_focalplane = S2SpacecraftTransform.from_json("msi_to_focalplane", d_spacecraft, "java")
        assert isinstance(msi_to_focalplane, dict)
        focalplane_to_sensor = S2SpacecraftTransform.from_json("focalplane_to_sensor", d_spacecraft, "java")
        assert isinstance(focalplane_to_sensor, dict)

        if j_piloting_to_msi is None:
            raise RuntimeError("Spacecraft piloting to MSI info is missing")

        ################
        # Init sensors #
        ################

        self._build_sensors(
            viewing_directions,
            line_datations,
            msi_to_focalplane,
            focalplane_to_sensor,
            j_piloting_to_msi,
        )

        ######################
        # Read refining info #
        ######################

        self.j_refining = self._build_refining()

        ###############
        # Init Rugged #
        ###############

        self.j_data_sensing_info = DataSensingInfos(
            q_coords.array_list(),
            pv_coords.array_list(),
            min_lines.hash_map(),  # pylint: disable=no-member
            max_lines.hash_map(),  # pylint: disable=no-member
        )

        # Const values
        self.min_max_quarter_lines = S2C.MINMAX_LINES_INTERVAL_QUARTER
        self.reference_pixel_height = S2C.PIXEL_HEIGHT_10

        # TODO margin depends on band ?
        band_pixel_size = S2C.PIXEL_HEIGHT_10  # to be fixed
        self.todo_margin = S2C.GRANULE_NB_LINE_60_M * S2C.PIXEL_HEIGHT_10 / band_pixel_size

        # Init rugged instance
        self.rugged_manager = RuggedManager.initRuggedManagerDefaultValues(
            j_tile_updater,
            self.j_data_sensing_info,
            self.min_max_quarter_lines,
            self.reference_pixel_height,
            list(self.sensors.values()).array_list(),  # pylint: disable=no-member
            self.todo_margin,
            self.j_refining,
        )

        # Default
        self.light_time_correction = False
        self.aberration_of_light_correction = False
        self.rugged_manager.setLightTimeCorrection(self.light_time_correction)
        self.rugged_manager.setAberrationOfLightCorrection(self.aberration_of_light_correction)

        # Init engine instances
        self.loc_engine = SimpleLocEngine(self.j_data_sensing_info, self.rugged_manager, j_tile_updater)
        self.sun_engine = SunAnglesEngine(self.j_data_sensing_info, self.rugged_manager, j_tile_updater)
        self.incidence_engine = IncidenceAnglesEngine(self.j_data_sensing_info, self.rugged_manager, j_tile_updater)

    @classmethod
    def init_schema(cls) -> dict:
        """
        Expected schema for constructor, as a JSON schema.

        :download:`JSON schema <doc/scripts/init_schema/schemas/S2MSILegacyGeometry.schema.json>`

        :download:`JSON example <doc/scripts/init_schema/examples/S2MSILegacyGeometry.example.json>`
        """
        # Same methods than in S2MSIGeometry
        return RefactoredMSI.init_schema()

    def _read_file_resources(self):
        """
        Initialize by reading file resource
        """

        iers_dir = self.config["resources"].get("iers")
        self.geoid_path = self.config["resources"].get("geoid")
        dem_globe_path = self.config["resources"].get("dem_globe")
        dem_srtm_path = self.config["resources"].get("dem_srtm")
        overlapping_tiles = self.config["resources"].get("overlapping_tiles")

        # Set the IERS directory in Orekit
        self._data_context = get_data_context(iers_dir)
        # We modify the default DataContext because, for now, SXGeo and Rugged don't support a
        # custom DataContext to be specified.
        DataContext.setDefault(self._data_context)

        # Use the default geoid
        if not self.geoid_path:
            self.geoid_path = osp.join(get_orekit_resources(), "resources/GEOID/egm96_15.gtx")
            assert osp.isfile(self.geoid_path)  # FIX: AttributeError: 'str' object has no attribute 'is_file'
            self.geoid_path = str(self.geoid_path)

        # Init a DEM globe or SRTM manager from path specified in the interface file
        # TODO: not for HCLOUD ?
        j_dem_file_manager = None
        if dem_globe_path:
            j_dem_file_manager = DemGlobeFileManager(dem_globe_path)  # overlap = true
        elif dem_srtm_path:
            j_dem_file_manager = SrtmFileManager(dem_srtm_path)  # overlap = false
        else:
            raise RuntimeError("DEM_GLOBE and DEM_SRTM paths are missing from the interface file.")
        j_dem_file_manager.findRasterFile()
        j_tile_updater = DemManager(
            j_dem_file_manager,
            GeoidManager(self.geoid_path, True),  # geoid is a single file (not tiles) so set overlap to True by default
            overlapping_tiles,
        )

        return j_tile_updater

    def _read_line_datations(self):
        """
        Build the line datations
        """

        d_line_datations = self.config["line_datations"]
        detector_names = d_line_datations["col_names"]
        band_names = d_line_datations["row_names"]
        dates = dates_from_json(d_line_datations, "GPS")  # 2D list
        ref_lines = d_line_datations["ref_lines"]
        rates = d_line_datations["rates"]

        # One time by detector and band
        assert len(dates) == len(ref_lines) == len(rates) == len(detector_names)
        if dates:
            assert len(dates[0]) == len(ref_lines[0]) == len(rates[0]) == len(band_names)

        # Convert 2D list into 2D dict of LinearLineDatation instances
        line_datations = {}
        self.j_ref_date = None
        for (
            det_name,
            det_dates,
            det_ref_lines,
            det_rates,
        ) in zip(detector_names, dates, ref_lines, rates):
            detector = S2Detector.from_name(det_name)
            line_datations[detector] = {}

            for band_name, j_date, ref_line, rate in zip(band_names, det_dates, det_ref_lines, det_rates):
                band = S2Band.from_name(band_name)
                line_datations[detector][band] = LinearLineDatation(j_date, float(ref_line), float(rate))

                # Save the reference date for the first sensor
                if not self.j_ref_date:
                    self.j_ref_date = j_date

        return line_datations

    def _build_sensors(
        self,
        viewing_directions: dict,
        line_datations: dict,
        msi_to_focalplane: dict,
        focalplane_to_sensor: dict,
        j_piloting_to_msi: SpaceCraftModelTransformation,
    ):
        """
        Build the list of sensor objects

        :param viewing_directions: dict of viewing directions
        :param line_datations: dict of line datations
        :param msi_to_focalplane: dict of MSI to focalplane transforms
        :param focalplane_to_sensor: dict of focalplane to sensor transforms
        :param j_piloting_to_msi: piloting to MSI transform
        """

        # Save sensors for each focal plane
        self.focalplane_sensors: dict[str, list] = {"VNIR": [], "SWIR": []}

        # Build the Java Sensor object list
        for detector in S2Detector.VALUES:
            for band in S2Band.VALUES:
                sensor = S2Sensor(detector, band).name

                # Read viewing direction values
                try:
                    j_viewing = viewing_directions[sensor]
                except KeyError:
                    continue  # next sensor

                # Read line datation
                try:
                    j_line_datation = line_datations[detector][band]
                except KeyError:
                    continue  # next sensor

                # Read spacecraft parameters
                try:
                    j_msi_to_focalplane = msi_to_focalplane[band.focal_plane]
                except KeyError:
                    continue  # next sensor
                try:
                    j_focalplane_to_sensor = focalplane_to_sensor[band.focal_plane][detector.name]
                except KeyError:
                    continue  # next sensor

                # Save sensor information
                j_sensor = Sensor(
                    sensor,
                    j_viewing,
                    j_line_datation,
                    float(band.pixel_height),
                    j_focalplane_to_sensor,
                    j_msi_to_focalplane,
                    j_piloting_to_msi,
                )
                self.sensors[sensor] = j_sensor

                # Save sensor for the current focal plane
                self.focalplane_sensors[band.focal_plane].append(j_sensor)

    def _build_refining(self) -> RefiningInfo:
        """
        Read refining data and build a RefiningInfo object

        :return: RefiningInfo object
        """

        d_refining = self.config.get("refining")
        if d_refining is None:
            return RefiningInfo()

        # Read acquisition center time
        center_time = AbsoluteDate(
            d_refining["center_time"]["UTC"],
            self._data_context.getTimeScales().getUTC(),
        )

        def read_uncertainties(json_fields: List[str], axis: List[str]):
            """Read X,Y,Z values from JSON into Java"""

            coefs = []
            try:
                d_coefs = d_refining
                for field in json_fields:
                    d_coefs = d_coefs[field]

                for this_axis in axis:
                    coefs.append(PolynomialFunction(d_coefs[this_axis].jarray()))

                return coefs

            # Return None if any field is missing
            except KeyError:
                return [None] * len(axis)

        # Concatenate polynomial functions for each field and axis.
        # Note: Spacecraft_Position and Focal_Plane_State don't seem to be used.
        poly_funcs = []

        # ephemeris[X,Y,Z]polyFunc = 3 PolynomialFunctions
        poly_funcs += read_uncertainties(["spacecraft_position"], ["x", "y", "z"])

        # spacecraftToMSITransfoMatrix[X,Y,Z]Func = 3 PolynomialFunctions
        poly_funcs += read_uncertainties(["msi_state", "rotation"], ["x", "y", "z"])

        # spacecraftToMSIHomothetyZFunc = 1 PolynomialFunctions
        poly_funcs += read_uncertainties(
            ["msi_state", "homothety"],
            ["z"],
        )

        # msiToFocalPlaneTransfoMatrix[X,Y,Z]Func + msiToFocalPlaneHomothetyZFunc
        # = 4 HashMap<Sensor, PolynomialFunction>
        poly_dicts = [{}, {}, {}, {}]
        for focalplane in ("VNIR", "SWIR"):
            # Read JSON
            rotation = read_uncertainties(["focalplane_state", focalplane, "rotation"], ["x", "y", "z"])
            homothety = read_uncertainties(
                ["focalplane_state", focalplane, "homothety"],
                ["z"],
            )

            # Save the polynomial function for each sensor of the current focal plane
            # = for each VNIR band or each SWIR band
            for j_sensor in self.focalplane_sensors[focalplane]:
                # For rotation x, rotation y, rotation z and homothety z
                for i_poly_func, poly_func in enumerate(rotation + homothety):
                    poly_dicts[i_poly_func][j_sensor] = poly_func

        # Convert Python dicts into Java HashMaps
        poly_maps = [poly_dict.hash_map() for poly_dict in poly_dicts]  # pylint: disable=no-member

        # Create Java RefiningInfo instance
        return RefiningInfo(True, center_time, *poly_funcs, *poly_maps)

    def init_orekit(self, iers_files: List[str], orekit_data_dir_path: str = None):
        """
        Initialize orekit data context.
        :param iers_files: list of IERS bulletin to handle
        :param orekit_data_dir_path: if None use internal orekit data (DE-430 and UTC-TAI history),
        use explicit orekit data otherwise
        """

    # pylint: disable=arguments-differ,unused-argument
    def direct_loc(
        self,
        coordinates: np.ndarray,
        geometric_unit: str = None,
        altitude: float = None,
        **kwargs,
    ) -> Tuple[np.ndarray, np.ndarray]:
        """
        Implementation of the direct location routine for S2 MSI

        :param coordinates: n-dim numpy array of integer (col,row) pixel coordinates.
        :param geometric_unit: Sensor ASGARD name as B0x/D0x
        :param altitude: Constant altitude to use for direct location, if None the DEM is used
        :return: n-dim numpy array of ground coordinates as double (lon,lat,alt) in (deg,deg,m),
                 n-dim numpy array of acquisition times (for each coordinate) as double offsets
                 in seconds from the reference date (from the line datations)
        """

        # Use more convenient names
        pixels = coordinates
        sensor = geometric_unit

        # Check values
        if not self.sensors:
            raise RuntimeError("Sentinel-2 sensor list is empty")
        if not sensor:
            raise RuntimeError("Sensor name is mandatory")
        if sensor not in self.sensors:
            raise RuntimeError(
                f"Unknown sensor {sensor!r}, " f"must be one of: {', '.join(str(key) for key in self.sensors)}"
            )

        # Constant altitude or default rugged algorithm
        if altitude is not None:
            self.rugged_manager.setConstantElevationMode(float(altitude))
        else:
            self.rugged_manager.resetIntersectionAlgorithm()

        # Change shape, leave 2 components at the end
        flat_pixels = flatten_array(pixels, 2)

        # Flip the asgard (col,row) into the sxgeo (row,col) convention
        # then convert the python integer list into a java double array
        pixels_jarray = np.flip(flat_pixels, 1).astype(np.double).jarray()

        # Call the java direct location
        grounds_jarray = self.loc_engine.computeDirectLoc(self.sensors[sensor], pixels_jarray)

        # Convert the java double array into a python double numpy array
        grounds = np.array(grounds_jarray.tuple("double"))  # "double" = Java type

        # Line datation
        j_line_datation = self.sensors[sensor].getLineDatation()

        # Get the acquisition date for each pixel line.
        # Express it as an offset from the reference date.
        times = np.array(
            [j_line_datation.getDate(float(row)).durationFrom(self.j_ref_date) for _, row in flat_pixels],
            np.double,
        )

        # reshape outputs
        grounds = restore_array(grounds, pixels.shape[:-1], last_dim=3)
        times = restore_array(times, pixels.shape[:-1])

        return grounds, times

    def direct_loc_over_geoid(
        self,
        coordinates: np.ndarray,
        geometric_unit: str = None,
        altitude: float = None,
    ) -> Tuple[np.ndarray, np.ndarray]:
        """
        Implementation of the direct location routine for S2 MSI

        :param coordinates: n-dim numpy array of integer (col,row) pixel coordinates.
        :param geometric_unit: Sensor ASGARD name as B0x/D0x
        :param altitude: Constant altitude to use for direct location, if None the DEM is used
        :return: n-dim numpy array of ground coordinates as double (lon,lat,alt) in (deg,deg,m),
                 n-dim numpy array of acquisition times (for each coordinate) as double offsets
                 in seconds from the reference date (from the line datations)
        """

        # Use more convenient names
        pixels = coordinates
        sensor = geometric_unit

        # check coordinates are integral
        assert np.issubdtype(pixels.dtype, np.integer)

        # Check values
        if not self.sensors:
            raise RuntimeError("Sentinel-2 sensor list is empty")
        if not sensor:
            raise RuntimeError("Sensor name is mandatory")
        if sensor not in self.sensors:
            raise RuntimeError(
                f"Unknown sensor {sensor!r}, " f"must be one of: {', '.join(str(key) for key in self.sensors)}"
            )

        # Constant altitude or default rugged algorithm
        loc_engine = self.loc_engine
        if altitude is not None:
            j_tile_updater_tmp = GeoidManager(self.geoid_path, True, altitude)
            rugged_manager_tmp = RuggedManager.initRuggedManagerDefaultValues(
                j_tile_updater_tmp,
                self.j_data_sensing_info,
                self.min_max_quarter_lines,
                self.reference_pixel_height,
                list(self.sensors.values()).array_list(),  # pylint: disable=no-member
                self.todo_margin,
                self.j_refining,
            )

            # Default
            rugged_manager_tmp.setLightTimeCorrection(self.light_time_correction)
            rugged_manager_tmp.setAberrationOfLightCorrection(self.aberration_of_light_correction)

            # Init engine instances
            loc_engine = SimpleLocEngine(self.j_data_sensing_info, rugged_manager_tmp, j_tile_updater_tmp)
        else:
            self.rugged_manager.resetIntersectionAlgorithm()

        # Change shape, leave 2 components at the end
        flat_pixels = flatten_array(pixels, 2)

        # Flip the asgard (col,row) into the sxgeo (row,col) convention
        # then convert the python integer list into a java double array
        pixels_jarray = np.flip(flat_pixels, 1).astype(np.double).jarray()

        # Call the java direct location
        grounds_jarray = loc_engine.computeDirectLoc(self.sensors[sensor], pixels_jarray)

        # Convert the java double array into a python double numpy array
        grounds = np.array(grounds_jarray.tuple("double"))  # "double" = Java type

        # Line datation
        j_line_datation = self.sensors[sensor].getLineDatation()

        # Get the acquisition date for each pixel line.
        # Express it as an offset from the reference date.
        times = np.array(
            [j_line_datation.getDate(float(row)).durationFrom(self.j_ref_date) for _, row in flat_pixels],
            np.double,
        )

        # reshape outputs
        grounds = restore_array(grounds, pixels.shape[:-1], last_dim=3)
        times = restore_array(times, pixels.shape[:-1])

        return grounds, times

    def inverse_loc(
        self,
        ground_coordinates: np.ndarray,
        tolerance: float = 1e-4,
        geometric_unit: str = "default",
        max_iterations: int = 10,
        altitude: float | None = None,
    ) -> np.ndarray:
        """
        Implementation of the inverse location routine for S2 MSI

        :param ground_coordinates: n-dim numpy array of ground coordinates as double (lon,lat,alt)
                                   in (deg,deg,m)
        :param tolerance: not used
        :param geometric_unit: sensor ASGARD name as B0x/D0x
        :param max_iterations: not used
        :param float altitude: If given, set the constant altitude to perform inverse locations

        :return: n-dim numpy array of np.double (col,row) pixel coordinates.
        """

        # Use more convenient names
        grounds = ground_coordinates
        sensor = geometric_unit

        # check coordinates are integral
        assert np.issubdtype(grounds.dtype, np.double)

        # Check values
        if not self.sensors:
            raise RuntimeError("Sentinel-2 sensor list is empty")
        if not sensor:
            raise RuntimeError("Sensor name is mandatory")
        if sensor not in self.sensors:
            raise RuntimeError(
                f"Unknown sensor {sensor!r}, must be one of: {', '.join(str(key) for key in self.sensors)}"
            )

        # Change shape, leave 3 components at the end
        flat_grounds = flatten_array(grounds, 3)

        if altitude is not None:
            flat_grounds[:, 2] = altitude

        # Convert into a java double array
        grounds_jarray = flat_grounds.astype(np.double).jarray()

        # Call the java inverse location
        pixels_jarray = self.loc_engine.computeInverseLoc(self.sensors[sensor], grounds_jarray)

        # Convert the java double array into a python double numpy array.
        # Flip the sxgeo (row,col) into the asgard (col,row) convention.
        pixels = np.flip(pixels_jarray.tuple("double"), 1)

        # reshape outputs
        return restore_array(pixels, grounds.shape[:-1], last_dim=2)

    def sun_angles(self, ground_coordinates: np.ndarray, times: np.ndarray) -> np.ndarray:
        """
        Implementation of the sun_angles routine for S2 MSI

        :param ground_coordinates: n-dim numpy array of ground coordinates as double (lon,lat,alt) in (deg,deg,m)
        :param times: n-dim numpy array of acquisition times (for each coordinate) as double offsets in seconds
                      from the reference date (from the line datations)
        :return: Array of solar angles (azimuth + zenith angles)
        """

        flat_grounds = flatten_array(ground_coordinates, 3)
        flat_times = flatten_array(times)
        assert flat_grounds.shape[0] == flat_times.shape[0]

        # Convert the python integer list into a java double array
        grounds_jarray = flat_grounds.astype(np.double).jarray()

        # Absolute dates = reference date + offsets in seconds
        dates = [self.j_ref_date.shiftedBy(float(offset)) for offset in flat_times]

        # Call the java method
        angles_jarray = self.sun_engine.computeAzimuthAndZenithAngles(grounds_jarray, dates)

        # Convert the java double array into a python double numpy array
        angles = np.array(angles_jarray.tuple("double"))  # "double" = Java type

        # restore initial shape and drop last coord
        return restore_array(angles, ground_coordinates.shape[:-1], last_dim=2)

    def incidence_angles(self, ground_coordinates: np.ndarray, times: np.ndarray) -> np.ndarray:
        """
        Implementation of the incidence_angles routine for S2 MSI

        :param ground_coordinates: n-dim numpy array of ground coordinates as double (lon,lat,alt) in (deg,deg,m)
        :param times: n-dim numpy array of acquisition times (for each coordinate) as double offsets in seconds
                      from the reference date (from the line datations)
        :return: Array of incidence angles (azimuth + zenith angles)
        """

        # Use the calculation that doesn't need the sensor or band height
        sensor_name = None
        band_height = None

        flat_grounds = flatten_array(ground_coordinates, 3)
        flat_times = flatten_array(times)
        assert flat_grounds.shape[0] == flat_times.shape[0]

        # Convert the python integer list into a java double array
        grounds_jarray = flat_grounds.astype(np.double).jarray()

        # Absolute dates = reference date + offsets in seconds
        dates = [self.j_ref_date.shiftedBy(float(offset)) for offset in flat_times]

        # Call the java method
        angles_jarray = self.incidence_engine.computeAzimuthAndZenithAngles(
            sensor_name, band_height, grounds_jarray, dates
        )

        # Convert the java double array into a python double numpy array
        angles = np.array(angles_jarray.tuple("double"))  # "double" = Java type

        # restore initial shape and drop last coord
        return restore_array(angles, ground_coordinates.shape[:-1], last_dim=2)
