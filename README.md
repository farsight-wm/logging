# farsight-wm/logging
[![License: MIT](https://img.shields.io/badge/License-MIT-silver.svg)](https://opensource.org/licenses/MIT)

A more elegant logging than using pub.flow:debugLog ;)

Configure log files, levels, loggers with startup services.

## TODO
 - Add a maven reactor project to build all at once.

## Install

 1. Clone git repositories and build the maven projects (mvn clean install)
    1. farsight-wm:parent
    1. farsight-wm:utils
    1. farsight-wm:logging-log4jplugin
    1. farsight-wm:is-package-maven-plugin
    1. farsight-wm:logging (this one)
 1. Copy the generated IS package to your IntegrationServer (from farsight-wm:logging target/is/FarsightWmLogging)
 1. Load the package

## Usage

TODO

