#!/bin/bash -e

if ! [ -e conf/Catalina/localhost/ROOT.xml ]; then
  echo >&2 "lsFusion client is not configured - configuring..."

    if [ -z "$LSFUSION_SERVER_HOST" ]; then
  	echo >&2 'error: missing required LSFUSION_SERVER_HOST environment variable'
  	exit 1
    fi

    if [ -z "$LSFUSION_SERVER_PORT" ]; then
  	echo >&2 'error: missing required LSFUSION_SERVER_PORT environment variable'
  	exit 1
    fi

  mkdir -p conf/Catalina/localhost
  rm -rf webapps

  cat > conf/Catalina/localhost/ROOT.xml <<EOF
<?xml version='1.0' encoding='utf-8'?>
<Context path="" docBase="../deploy/client.war">
  <Parameter name="host" value="$LSFUSION_SERVER_HOST" override="false"/>
  <Parameter name="port" value="$LSFUSION_SERVER_PORT" override="false"/>
</Context>
EOF
fi

exec bin/catalina.sh run