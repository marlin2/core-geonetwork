#!/bin/bash

# Delete the metadata records in to_host and
# upload the zip archive containing the mefs dumped using dumprecords.sh
#
# Note: Use port direct to tomcat (eg. 8080) as apache reverse proxies 
# drop the connection to the curl client after 900 seconds (5 minutes) with
# a 502 (this is important on the xml.mef.export service)
#

export TO_HOST=
export TO_CRED=
export TO_COOK=to_cookies
export INPUTFILE=


while getopts ":c:h:i:" opt; do
  case $opt in
    i)
      echo "MEFS will be loaded from file: $OPTARG" >&2
			INPUTFILE=$OPTARG
      ;;
    h)
      echo "Wiping and loading host: $OPTARG" >&2
			TO_HOST=${OPTARG}
      ;;
    c)
      echo "GeoNetwork credentials are: $OPTARG" >&2
			TO_CRED=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

if [ -z $TO_HOST ] || [ -z $TO_CRED ] || [ -z $INPUTFILE ]
then
  echo "Usage: $0 -h <geonetwork_host_url> -c <credentials> -i <inputfile>" >&2
	echo >&2
	echo "eg. $0 -h http://localhost:8080/geonetwork -c admin:admin -i exportfull.zip" >&2
	exit 1
fi

set -x

# Really important that any old cookies get deleted as can interfere with things
rm to_cookies

set +x
read -p "About to destroy records in GeoNetwork on $TO_HOST, OK? (y/n) " RESPONSE 
if [ "$RESPONSE" != "y" ]; then
	echo "Exiting"
	exit 1
fi

set -x
# Load them into TO_HOST GeoNetwork instance with uuidProcessing set to overwrite so that any existing records will be
# replaced with existing records
curl --cookie-jar $TO_COOK ${TO_HOST}/geonetwork/srv/api/0.1/records -X POST
export XSRFTOKEN=`cat to_cookies | grep XSRF-TOKEN | cut -f7`
export XSRFHEADER="X-XSRF-TOKEN:$XSRFTOKEN"
#curl -X POST "http://140.79.17.39:8080/geonetwork/srv/api/0.1/records?metadataType=METADATA&uuidProcessing=OVERWRITE&group=2&rejectIfInvalid=false&publishToAll=true&assignToCatalog=false&transformWith=_none_" -H "accept: application/json" -H "Content-Type: application/json" -H "X-XSRF-TOKEN: a625f181-c007-4326-893c-2b30d6b2575d" -d "file=%5Bobject%20File%5D"
#curl -X POST --cookie $TO_COOK -w "%{http_code}" -H "accept: application/json" -H $XSRFHEADER -u $TO_CRED -F metadataType="METADATA" -F assignToCatalog=true -F file=@$INPUTFILE -F uuidProcessing=OVERWRITE -F publishToAll=true -F category=2 -F group=25495 ${TO_HOST}/geonetwork/srv/api/0.1/records
# publishToAll causes oracle to run out of cursors....
curl -X POST --cookie $TO_COOK -w "%{http_code}" -H "accept: application/json" -H $XSRFHEADER -u $TO_CRED -F metadataType="METADATA" -F assignToCatalog=true -F file=@$INPUTFILE -F uuidProcessing=OVERWRITE -F category=2 -F group=25495 ${TO_HOST}/geonetwork/srv/api/0.1/records

exit 0
