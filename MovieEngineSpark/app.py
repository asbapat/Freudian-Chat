from flask import Blueprint
import os, time, sys

import json
from engine import RecommendationEngine

import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

from flask import Flask, request
game = Flask(__name__)

from pyspark import SparkContext, SparkConf

@game.route("/<user_id>/ratings/top/10/<int:sentiment>", methods=["GET"])
def top_ratings(user_id, sentiment):
	logger.debug("User %s TOP ratings requested", user_id)
	top_ratings = recommendation_engine.get_top_ratings(user_id, 10, sentiment)
	return json.dumps(top_ratings)

@game.route("/<int:user_id>/ratings/<int:movie_id>", methods=["GET"])
def movie_ratings(user_id, movie_id):
	logger.debug("User %s rating requested for movie %s", user_id, movie_id)
	ratings = recommendation_engine.get_ratings_for_movie_ids(user_id, [movie_id])
	return json.dumps(ratings)

@game.route("/<int:user_id>/ratings", methods=["POST"])
def add_ratings(user_id):
	ratings_list = request.form.keys()[0].strip().split("\n")
	ratings_list = map(lambda x: x.split(","), ratings_list)
	ratings = map(lambda x: (user_id, int(x[0]), float(x[1])), ratings_list)
	recommendation_engine.add_ratings(ratings)

	return json.dumps(ratings)

if __name__ == '__main__':
	global recommendation_engine
	conf = SparkConf().setAppName("movie-server")
	spark_context = SparkContext(conf=conf, pyFiles=['engine.py'])
	dataset_path = os.getcwd()
	recommendation_engine = RecommendationEngine(spark_context, dataset_path)
	game.run(host='0.0.0.0', port=8080)