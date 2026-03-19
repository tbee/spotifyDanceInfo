#!/usr/bin/env bash

# TODO: configure java

mvnw versions:set
mvnw clean package -DskipTests -Pcontainer

