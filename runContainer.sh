export PROJECT_VERSION=`mvnw help:evaluate -Dexpression=project.version -q -DforceStdout`
echo PROJECT_VERSION=$PROJECT_VERSION
podman compose -f web/src/main/container/compose.yaml up
