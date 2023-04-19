#!/bin/bash
# ugly solution but it works
SCRIPT_PATH=`readlink -f "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_PATH"`
cd $SCRIPT_DIR
time=`date  +%Y%m%d`
git add .
git commit -m ${time}
git push -u origin master
