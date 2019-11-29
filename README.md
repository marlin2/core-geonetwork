# Build Health

[![Build Status](https://travis-ci.org/marlin2/core-geonetwork.svg?branch=develop)](https://travis-ci.org/marlin2/core-geonetwork)

# The is the CSIRO Marine and Atmospheric Research Fork of ANZMEST/GeoNetwork 3.x,x

Config overrides are no longer supported in 3.x.x (hmm) so the following files have been customised to include 
changes specific to Marlin:

web-ui/src/main/resources/catalog/locales/en-search.json 
web/src/main/webapp/WEB-INF/classes/setup/sql/data/data-db-default.sql

Other changes between 3.x.x and this fork can be found by doing a comparison between this fork and ANZMEST 3.x.x and between ANZMEST 3.x.x and GeoNetwork 3.x.x.

The basic idea of this fork is customise the MCP schema for Marlin requirements.

---

## GeoNetwork Development Environment Setup

To get a development environment working we have developed a containerised solution
You can either choose to build and run GeoNetwork on a Docker Swarm (which requires a Traefik edge-router), or more simply using Docker compose.

#### Retrieving the GeoNetwork codebase

```shell script
git clone --recursive https://github.com/marlin2/core-geonetwork.git -b workflow
git submodule update --init --recursive && cd schemas/iso19115-3 && git checkout 3.4.x
```

#### Building and running containerised GeoNetwork using Docker Compose

To start the GeoNetwork container with Docker Compose, just run:

```shell script
sudo ./bin/start-compose-dev.sh
```

This brings up the GeoNetwork container in detached mode. To view the container output, follow the Docker logs with:

```shell script
sudo docker logs coregeonetwork_jetty_1 -f
```

To view the website, go to: [http://localhost/](http://localhost/)

To shut down the container and remove the Docker network, run:

```shell script
sudo ./bin/stop-compose-dev.sh
```

#### Building and running containerised GeoNetwork using Docker Swarm

To start the GeoNetwork container on a Docker Swarm, run:

```shell script
sudo ./bin/start-swarm-dev.sh
```

This requires a Traefik edge router to be running. To view the container output, follow the Docker logs with:

```shell script
sudo docker service logs geonetwork_jetty -f
```

To view the website, go to: [geonetwork.localhost](geonetwork.localhost)

To shut down the container and remove the Docker stack, run:

```shell script
sudo ./bin/stop-swarm-dev.sh
```
