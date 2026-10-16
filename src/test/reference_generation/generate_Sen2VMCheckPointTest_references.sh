# TODO if modified on Windows: sed -i 's/\r$//' generate_Sen2VMCheckPointTest_references.sh
python3.11 generate_one_point_with_asgard.py ../resources/tests/input/TDS1/configuration_TDS1_direct.json D01 B01 "0.0" "0.0" | grep ground
python3.11 generate_one_point_with_asgard.py ../resources/tests/input/TDS1/configuration_TDS1_direct.json D01 B01 "700.5" "250.5" | grep ground


python3.11 generate_one_point_with_asgard.py ../resources/tests/input/TDS1/configuration_TDS1_inverse.json D01 B01 "0.0" "0.0" | grep ground
python3.11 generate_one_point_with_asgard.py ../resources/tests/input/TDS1/configuration_TDS1_inverse.json D01 B01 -18.919175317847085 33.79427774463745 42.539127849734236 | grep ground 
python3.11 generate_one_point_with_asgard.py ../resources/tests/input/TDS1/configuration_TDS1_inverse.json D01 B01 -18.919175317847085 33.79427774463745 | grep ground 
python3.11 generate_one_point_with_asgard.py ../resources/tests/input/TDS1/configuration_TDS1_inverse.json D01 B01 -18.821137292435186 33.635377815436044 42.87227050779195 | grep ground