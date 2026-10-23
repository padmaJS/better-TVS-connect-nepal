#!/bin/sh
# Rebuild places.txt from GeoNames (CC-BY 4.0): populated places only, ASCII names, no hamlets (PPLL).
set -e
curl -sSO https://download.geonames.org/export/dump/NP.zip && unzip -o -q NP.zip NP.txt
awk -F'\t' '$7=="P" && $8 ~ /^(PPL|PPLA|PPLA2|PPLA3|PPLA4|PPLC|PPLX|PPLS)$/ {printf "%s\t%s\t%s\n",$5,$6,$3}' NP.txt > places.txt
rm -f NP.zip NP.txt
