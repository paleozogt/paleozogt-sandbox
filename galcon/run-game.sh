#!/bin/bash

bot1=$1
bot2=$2
map=$3

java -jar tools/PlayGame.jar maps/map${map}.txt 1000 200 log.txt "java -jar dist/$bot1.jar" "java -jar dist/$bot2.jar" | java -jar tools/ShowGame.jar