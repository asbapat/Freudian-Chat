#!/bin/bash

wget http://files.grouplens.org/datasets/movielens/ml-latest-small.zip
unzip ml-latest-small
mv ml-latest-small/ratings.csv ratings.csv
mv ml-latest-small/movies.csv movies.csv
rm -rf ml-latest-small.zip
rm -rf ml-latest-small
wget http://files.grouplens.org/datasets/movielens/ml-20m.zip
unzip ml-20m
mv ml-20m/ratings.csv ratingsbig.csv
mv ml-20m/movies.csv moviesbig.csv
rm -rf ml-20m.zip
rm -rf ml-20m