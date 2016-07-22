#!/bin/bash

curl -X POST -H 'Content-type: text/xml' -d @$1 http://geonetwork-dev-hba.it.csiro.au:6060/deegree-wfs/services > $1.out

