#!/bin/sh
set -x
set -e

java -jar ./target/TIGGER.jar -r $1 -o bah.asm "./tiger-samples/codegen/$2"
QtSpim bah.asm
