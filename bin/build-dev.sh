#!/bin/bash

git submodule update --init --recursive \
&& git -C schemas/iso19115-3 checkout 3.4.x

if [ -f web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by.css -a ! -f web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by-v2.css ]; then
    mv web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by.css web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by-v2.css
fi

docker run -d -t -i --rm --name geonetwork_build -w /usr/src/geonetwork --volume /home/sha387/sourcecode/docker/core-geonetwork:/usr/src/geonetwork --volume $HOME/.m2:/root/.m2 docker-registry.it.csiro.au/idc/geonetwork:base bash

docker exec -w /usr/src/geonetwork -it geonetwork_build mvn clean install -DskipTests 

docker stop geonetwork_build
