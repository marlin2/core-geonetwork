#!/bin/bash

git submodule update --init --recursive \
&& git -C schemas/iso19115-3 checkout 3.4.x
#&& git -C e2e-tests/chromedriver checkout master \
#&& git -C web-ui/src/main/resources/catalog/lib/style/bootstrap checkout master \
#&& git -C web-ui/src/main/resources/catalog/lib/style/font-awesome checkout master \
#&& git -C web-ui/src/main/resources/catalog/lib/bootstrap-table checkout master \
#&& git -C schemas/iso19139.mcp checkout master \
#&& git -C schemas/iso19139.anzlic checkout master \
#&& git -C schemas/iso19139.mcp-2.0 checkout master \

if [ -f web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by.css -a ! -f web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by-v2.css ]; then
    mv web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by.css web-ui/src/main/resources/catalog/lib/bootstrap-table/dist/extensions/group-by-v2/bootstrap-table-group-by-v2.css
fi

docker run -d -t -i --rm --name geonetwork_build -w /usr/src/geonetwork --volume `pwd`:/usr/src/geonetwork --volume `pwd`/m2:/root/.m2 docker-registry.it.csiro.au/idc/geonetwork:base bash

docker exec -it geonetwork_build mvn clean install -DskipTests

docker stop geonetwork_build
