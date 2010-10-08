#!/bin/bash

bot1=dist/$1.jar
bot2=dist/$2.jar
map=$3

echo "$bot1 vs $bot2"
java -jar tools/PlayGame.jar maps/map${map}.txt 1000 200 log.txt "java -jar $bot1" "java -jar $bot2" | java -jar tools/ShowGame.jar