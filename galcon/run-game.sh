#!/bin/bash

bot1=$1
bot2=$2
map=map7.txt

java -jar tools/PlayGame.jar maps/$map 1000 1000 log.txt "java -jar dist/$bot1.jar" "java -jar dist/$bot2.jar" | java -jar tools/ShowGame.jar