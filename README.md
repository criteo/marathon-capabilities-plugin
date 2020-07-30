# Marathon Capabilities Plugin

This plugin appends linux capabilities to the tasks of an application
according to MESOS_BOUNDING_CAPABILITIES and MESOS_EFFECTIVE_CAPABILITIES
labels.


## Objectives

This plugin has been written to add capabilities to certain tasks which
need to be granted with network related capabilities mainly in order
to forge packets for performing efficient network scans with nmap and/or
probe specific kind of vulnerabilities. It can surely be used to grant
any capabilities supported by Mesos in the future.


## Getting Started

In order to install this plugin, you should follow the official documentation
https://mesosphere.github.io/marathon/docs/plugin.html

A template of plugin descriptor is located in
src/main/resources/com/criteo/marathon


## Contribute

You can use SBT to build and test this project.

    sbt compile
    sbt test

### Building using Docker sbt image

Assuming you have docker installed, the Makefile targets can be used to clean, build and experiment with the sbt command line.

Clean the target directory

    make clean

Build the plugin jar file

    make build

Open a sbt command prompt to experiment with the build process

    make sbt-shell

## TODO

* Add all capabilities supported by Mesos.
