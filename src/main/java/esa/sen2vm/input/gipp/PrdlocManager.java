/** Copyright 2024-2025, CS GROUP, https://www.cs-soprasteria.com/
*
* This file is part of the Sen2VM Core project
*     https://gitlab.acri-cwa.fr/opt-mpc/s2_tools/sen2vm/sen2vm-core
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.*/

package esa.sen2vm.input.gipp;

import java.util.HashMap;
import java.util.logging.Logger;

import esa.sen2vm.enums.BandInfo;
import esa.sen2vm.enums.DetectorInfo;
import esa.sen2vm.exception.Sen2VMException;

import generated.GS2_INIT_LOC_PROD_PARAMETERS;
import _int.esa.gs2.gs._1_0.gipp.AN_INIT_LOC_PROD_PARAMETERS;
import _int.esa.gs2.gs._1_0.gipp.AN_INIT_LOC_PROD_PARAMETERS.SEGMENT_CUTTING;

/**
 * Class used to manage PRDLOC GIPP data
 */
public class PrdlocManager
{
    private static final Logger LOGGER = Logger.getLogger(GIPPManager.class.getName());
    
    class BandDetector
    {
        final BandInfo band;
        final DetectorInfo detector;

        BandDetector (BandInfo band, DetectorInfo detector)
        {
            this.band = band;
            this.detector = detector;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if(!(o instanceof BandDetector)) return false;
            BandDetector that = (BandDetector) o;
            return band == that.band && detector == that.detector;
        }

        @Override
        public int hashCode()
        {
            return 31 * band.hashCode() + detector.hashCode();
        }
    }
    protected HashMap<BandDetector, Integer> rawShifs = new HashMap<BandDetector, Integer>();
    protected HashMap<DetectorInfo, Integer> minBeginLinesToCut = new HashMap<DetectorInfo, Integer>();

    /*
     * PrdlocManager constructor
     * @throws Sen2VMException
     */
    public PrdlocManager(GS2_INIT_LOC_PROD_PARAMETERS prdlocData) throws Sen2VMException
    {
        if (prdlocData != null)
        {
            // Init for computation of the minimum
            for (DetectorInfo detector: DetectorInfo.getAllDetectorInfo())
            {
                this.minBeginLinesToCut.put(detector,999999);
            }

            LOGGER.info("Reading GIP_PRDLOC");

            // Compute the minimum of line to per detector (taking all bands resolution into account)
            AN_INIT_LOC_PROD_PARAMETERS data = prdlocData.getDATA();
            for (SEGMENT_CUTTING.BAND_LIST.BAND bandGIPP: data.getSEGMENT_CUTTING().getBAND_LIST().getBAND())
            {
                BandInfo band = BandInfo.getBandInfoFromIndex(bandGIPP.getBand_Id());

                LOGGER.fine("    " + band.getNameWithB());
                for(SEGMENT_CUTTING.BAND_LIST.BAND.DETECTOR_LIST.DETECTOR detectorGIPP: bandGIPP.getDETECTOR_LIST().getDETECTOR())
                {
                    // rawShifs.put(new BandDetector(band, DetectorInfo.getDetectorInfoFromIndex(Integer.valueOf(detectorGIPP.getDetector_Id()))),
                    //             detectorGIPP.getBEGIN_NB_LINES_TO_CUT());

                    DetectorInfo detector = DetectorInfo.getDetectorInfoFromNumber(Integer.valueOf(detectorGIPP.getDetector_Id()));

                    LOGGER.fine("        " + detector.getNameWithD());
                    int detbandBeginLinesToCut = (int)(detectorGIPP.getBEGIN_NB_LINES_TO_CUT() * band.getPixelHeight() / 10);

                    LOGGER.fine("detbandBeginLinesToCut (" + band.getNameWithB() + "," + detector.getNameWithD() + "): " + detbandBeginLinesToCut);
                    // We search for the minimum, update rawShifts if new value is lower
                    if (detbandBeginLinesToCut < this.minBeginLinesToCut.get(detector))
                    {
                        detbandBeginLinesToCut -= detbandBeginLinesToCut % 6;
                        this.minBeginLinesToCut.put(detector,detbandBeginLinesToCut);
                    }
                }
            };

            // Get compressionMargin and update it to factor of 6 above
            int compressionMargin = data.getSEGMENT_CUTTING().getCOMPRESSION_MARGIN().getValue(); //From PRDLOC
            compressionMargin -= compressionMargin % 6;
            if (compressionMargin < data.getSEGMENT_CUTTING().getCOMPRESSION_MARGIN().getValue()) compressionMargin += 6;

            // Get radioMargin and update it to factor of 6 above
            int radioMargin = data.getSEGMENT_CUTTING().getRADIOMETRIC_MARGIN().getValue(); //From PRDLOC
            radioMargin -= radioMargin % 6;
            if (radioMargin < data.getSEGMENT_CUTTING().getRADIOMETRIC_MARGIN().getValue()) radioMargin += 6;

            // Compute for each Detector/Band
            for (DetectorInfo detector: DetectorInfo.getAllDetectorInfo())
            {
                for (BandInfo band: BandInfo.getAllBandInfo())
                {
                    int bandCompMargin = compressionMargin * 10 / (int) band.getPixelHeight();
                    int bandRadioMargin = radioMargin * 10 / (int) band.getPixelHeight();

                    int firstLineToDecode = (minBeginLinesToCut.get(detector) * 10 / (int)band.getPixelHeight())  + 1 - bandCompMargin - bandRadioMargin;
                    // int firstLineToDecode += granule_first_index*nb_lines_per_granules; Part is removed from the forumla as granules index is at 0 for the first ATF;

                    int firstSourcePacket = (int)(1 + (firstLineToDecode -1)/16); //truncation
                    int firstLineInSourcePacket = 1 + (firstSourcePacket - 1) * 16;

                    int nbLinesToCut = firstLineToDecode - firstLineInSourcePacket + bandCompMargin;
                    LOGGER.fine("INS-RAW, nbLinesToCut (" + band.getNameWithB() + "," + detector.getNameWithD() + "): " + nbLinesToCut);
                    this.rawShifs.put(new BandDetector(band, detector),nbLinesToCut);
                }
            }
        }
    }

    public Integer getRawShift(DetectorInfo detector, BandInfo band)
    {
        return rawShifs.get(new BandDetector(band,detector));
    }

    public HashMap<BandDetector, Integer> getRawShift()
    {
        return this.rawShifs;
    }
}