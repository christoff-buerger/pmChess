#!/bin/sh

# This program and the accompanying materials are made available under the
# terms of the MIT license (X11 license) which accompanies this distribution.
# 
# Author: Christoff Bürger

script_directory="$( cd "$( dirname "$0" )" || exit; pwd )"
"${script_directory}/pmChess.app/Contents/MacOS/pmChess" "$@"
