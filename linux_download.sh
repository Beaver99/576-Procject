#!/bin/bash
SCRIPT_PATH=`readlink -f "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_PATH"`
cd $SCRIPT_DIR
git pull origin master
