#!/usr/bin/env bash

# TODO: configure java

mvnw versions:set clean package -DskipTests -Pcontainer

