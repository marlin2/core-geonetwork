#!/bin/bash

docker build --tag=docker-registry.it.csiro.au/idc/geonetwork:dev --file=Dockerfile .
#docker build --tag=docker-registry.it.csiro.au/idc/geonetwork:dev --target dev . --file=Dockerfile --no-cache
