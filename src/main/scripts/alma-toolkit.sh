#!/bin/bash

APP=${project.artifactId}-${project.version}.jar

JAVA_OPTS=""

java $JAVA_OPTS -jar $APP $*
