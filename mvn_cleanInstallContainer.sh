#!/usr/bin/env bash

# toolchain takes care of the correct JVM

mvnw versions:set
mvnw clean install -DskipTests -Pcontainer

