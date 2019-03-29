#!/bin/bash

# Export all metadata records from from_host instance as a zip archive
# containing mefs. 
#
# Note: By default you should use the tomcat port (eg. 8080) as apache reverse proxies 
# drop the connection to the curl client after 900 seconds (5 minutes) with
# a 502 (this is important on the xml.mef.export service)
#
export FROM_HOST=
export FROM_CRED=
export SEARCH_TERM=
export FROM_COOK=from_cookies
export OUTPUTFILE=

while getopts ":c:h:o:s:" opt; do
  case $opt in
    o)
      echo "MEFS will be in output file: $OPTARG" >&2
			OUTPUTFILE=$OPTARG
      ;;
    h)
      echo "Dumping from host: $OPTARG" >&2
			FROM_HOST=${OPTARG}/srv/eng
      ;;
    c)
      echo "GeoNetwork credentials are: $OPTARG" >&2
			FROM_CRED=$OPTARG
      ;;
    s)
      echo "GeoNetwork search term is: $OPTARG" >&2
			SEARCH_TERM=$OPTARG
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

if [ -z $FROM_HOST ] || [ -z $FROM_CRED ] || [ -z $OUTPUTFILE ] || [ -z $SEARCH_TERM ]
then
  echo "Usage: $0 -h <geonetwork_host_url> -c <credentials> -o <outputfile> -s <search_term>" >&2
	echo >&2
	echo "eg. $0 -h http://localhost:8080/geonetwork -c admin:admin -s '_groupPublished=forPublic&_status=2' -o exportfull.zip" >&2
	exit 1
fi

set -x

# Really important that any old cookies get deleted as can interfere with things
rm from_cookies

# Extract all metadata as mef files from FROM_HOST GeoNetwork instance
curl -o /dev/null --cookie-jar $FROM_COOK -i -w "%{http_code}" -H 'Accept:application/xml' -u $FROM_CRED ${FROM_HOST}/xml.search\?${SEARCH_TERM}

curl --cookie $FROM_COOK -w "%{http_code}" -H 'Accept:application/xml' ${FROM_HOST}/xml.metadata.select?selected=add-all

curl --connect-timeout 90000000 --max-time 90000000 --cookie $FROM_COOK -w "%{http_code}" -H 'Accept:application/zip' ${FROM_HOST}/xml.mef.export?format=full\&version=2\&resolveXlink=false\&removeXlinkAttribute=false -o $OUTPUTFILE

exit 0
