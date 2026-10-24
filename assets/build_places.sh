#!/bin/sh
# Rebuild places.txt from OpenStreetMap (ODbL) via Overpass: city/town/village/suburb/neighbourhood/quarter nodes,
# name:en else name, ASCII only (cluster font). Denser than GeoNames in the valley (GeoNames gave "Milli" for 6 km).
set -e
curl -s -m 300 -A 'tvs-connect-oss/1.0' https://overpass-api.de/api/interpreter --data-urlencode \
  'data=[out:csv(::lat,::lon,"name:en",name;false;"\t")][timeout:250];area["ISO3166-1"="NP"][admin_level=2]->.a;node["place"~"^(city|town|village|suburb|neighbourhood|quarter)$"](area.a);out;' \
  | awk -F'\t' '{n=$3!=""?$3:$4} n!="" && n !~ /[^ -~]/ {printf "%s\t%s\t%s\n",$1,$2,n}' > places.txt
