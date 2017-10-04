@echo off

set APP=${project.artifactId}-${project.version}.jar

set JAVA_OPTS=

java %JAVA_OPTS% -jar "%APP%" %*
