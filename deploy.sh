#!/usr/bin/env bash
set -euo pipefail

BACKEND_DIR="/home/slavi/uni/diplomna/MyDrive/02_Development/backend/MYDrive"
FRONTEND_DIR="/home/slavi/uni/diplomna/MyDrive/02_Development/frontend/web"
TOMCAT_HOME="/home/slavi/uni/tomcat/apache-tomcat-10.1.55"
WAR_NAME="MYDrive-0.0.1-SNAPSHOT.war"

echo "=== Stopping Tomcat (if running) ==="
if "$TOMCAT_HOME/bin/shutdown.sh" 2>/dev/null; then
    sleep 3
else
    echo "Tomcat was not running, continuing."
fi

echo ""
echo "=== Building backend ==="
cd "$BACKEND_DIR"
mvn clean install -e -U -DskipTests

echo ""
echo "=== Deploying WAR ==="
rm -rf "$TOMCAT_HOME/webapps/${WAR_NAME%.war}"
cp "$BACKEND_DIR/target/$WAR_NAME" "$TOMCAT_HOME/webapps/$WAR_NAME"
echo "WAR copied to $TOMCAT_HOME/webapps/"

echo ""
echo "=== Starting Tomcat ==="
"$TOMCAT_HOME/bin/startup.sh"

echo ""
echo "=== Building frontend ==="
cd "$FRONTEND_DIR"
npm run build

echo ""
echo "=== Starting frontend ==="
exec npm run start
