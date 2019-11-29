#!/bin/bash

if [ -f web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by.css -a ! -f web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by-v2.css ]; then
    mv web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by.css web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by-v2.css
fi

docker-compose  -f docker-compose.yml -f docker-compose.compose.yml up -d
