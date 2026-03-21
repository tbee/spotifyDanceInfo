#!/usr/bin/env bash

# toolchain takes care of the correct JVM

mvnw clean package -DskipTests -Pcontainer

