#!/bin/bash
set +e

cd /workspace/DATA/S2A_OPER_PRD_MSIL1B_PDMC_20251205T082306_R071_V20251203T200333_20251203T200413.SAFE

OUT_ORTHO="/workspace/DATA/GDAL_OUTPUT_ORTHO"
OUT_MOSAIC="/workspace/DATA/GDAL_OUTPUT_MOSAIC"

mkdir -p "$OUT_ORTHO"
mkdir -p "$OUT_MOSAIC"

XML="./S2A_OPER_MTD_L1B_DS_2APS_20251203T202829_S20251203T200331.xml"

# =====================================================
# Function to get band resolution in meters 
# Note: This returns the resampling resolution in meters, not the grid step in pixels
# =====================================================
get_band_resolution() {
    local vrt_name="$1"
    # Extract band from VRT name (e.g., ..._D09_B01 -> B01)
    local band=$(echo "$vrt_name" | grep -oE '_B[0-9]{1,2}[A]?' | sed 's/_//')

    case "$band" in
        B02|B03|B04|B08)
            echo 10  # 10m bands -> 10 meters resolution
            ;;
        B05|B06|B07|B8A|B11|B12)
            echo 20  # 20m bands -> 20 meters resolution
            ;;
        B01|B09|B10)
            echo 60  # 60m bands -> 60 meters resolution
            ;;
        *)
            echo 10  # Default to 10 meters
            ;;
    esac
}

echo "=== ORTHORECTIFICATION ==="

for VRT in "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D12_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D10_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D08_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D07_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D03_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D04_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D06_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D01_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D11_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D02_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D05_B04" "S2A_OPER_GEO_L1B_DS_2APS_20251203T202829_S20251203T200331_D09_B04"; do
    BASENAME="$VRT"
    OUT="$OUT_ORTHO/${BASENAME}_ortho.tif"

    echo "----------------------------------------"
    echo "Processing VRT: $VRT"
    echo "----------------------------------------"

    rm -f "$OUT"

    # Get resolution for this band (Issue #61)
    RESOLUTION=$(get_band_resolution "$VRT")
    echo "Band resolution: $RESOLUTION m"

    # Orthorectification with explicit resolution and nodata
    gdalwarp \
        SENTINEL2_L1B_WITH_GEOLOC:./$XML:$VRT \
        "$OUT" \
        -t_srs EPSG:32636 \
        -te 210000 3296817 436397 3504541 \
        -r cubic \
        -tr $RESOLUTION -$RESOLUTION \
        -dstnodata 0 \
        -co COMPRESS=LZW \
        -co TILED=YES \
        -overwrite

    echo ""
done

echo ""
echo "=== MOSAIC GENERATION ==="
echo ""

for BAND in B04; do
    echo "----------------------------------------"
    echo "Creating mosaic for band: $BAND"
    echo "----------------------------------------"

    INPUT_FILES=($(ls $OUT_ORTHO/*_${BAND}_ortho.tif 2>/dev/null))

    if [ ${#INPUT_FILES[@]} -eq 0 ]; then
        echo "No ortho images found for band $BAND"
        continue
    fi

    OUTPUT="$OUT_MOSAIC/ORTHO_mosaic_${BAND}.tif"

    # Use gdal_merge.py instead of gdalwarp to avoid double resampling 
    # All ortho images should have the same resolution and geometry
    # No resampling needed, just assembly
    gdal_merge.py \
        -o "$OUTPUT" \
        -of GTiff \
        -co COMPRESS=LZW \
        -co TILED=YES \
        -ot UInt16 \
        -n 0 \
        -a_nodata 0 \
        "${INPUT_FILES[@]}"

    echo " Mosaic written -> $OUTPUT"
    echo ""
done

echo "=== GDAL processing complete ==="
