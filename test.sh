rm src/test/resources/TDS1_SmallIsland/L1B/GRANULE/S2A_OPER_MSI_L1B_GR_DPRM_20140630T140000_S20200816T1202*/GEO* -r
rm src/test/resources/TDS1_SmallIsland/L1B/DATASTRIP/*/GEO* -r

mvn compile
mvn test