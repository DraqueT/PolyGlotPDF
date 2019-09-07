#!/bin/bash
# zips for (non)modular distribution

read_from="dist/"
dist_dest="java_8_bridge.zip"

echo "Zipping completed distribution..."
zip -r "${dist_dest}" "${read_from}"
