# Build Health

[![Build Status](https://travis-ci.org/marlin2/core-geonetwork.svg?branch=develop)](https://travis-ci.org/marlin2/core-geonetwork)

# The is the CSIRO Marine and Atmospheric Research Fork of ANZMEST/GeoNetwork 3.x,x

Config overrides are no longer supported in 3.x.x (hmm) so the following files have been customised to include 
changes specific to Marlin:

web-ui/src/main/resources/catalog/locales/en-search.json 
web/src/main/webapp/WEB-INF/classes/setup/sql/data/data-db-default.sql

Other changes between 3.x.x and this fork can be found by doing a comparison between this fork and ANZMEST 3.x.x and between ANZMEST 3.x.x and GeoNetwork 3.x.x.

The basic idea of this fork is customise the MCP schema for Marlin requirements.
