import os, time
from pyspark import SparkContext
from pyspark.mllib.recommendation import ALS
import math
import re

import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def get_counts_and_averages(ID_and_ratings_tuple):
		nratings = len(ID_and_ratings_tuple[1])
		return ID_and_ratings_tuple[0], (nratings, float(sum(x for x in ID_and_ratings_tuple[1]))/nratings)


class RecommendationEngine:
	"""
	A Movie Recommendation Engine
	------------------------------
	"""
	def _count_and_average_ratings(self):
		"""
		Update the movies ratings counts 
		-------------------------------- 
		from the current data self.ratings_RDD
		"""
		logger.info("Counting the movie ratings")
		movie_ID_with_ratings_RDD = self.ratings_RDD.map(lambda x: (x[1], x[2])).groupByKey()
		movie_ID_with_avg_ratings_RDD = movie_ID_with_ratings_RDD.map(get_counts_and_averages)
		self.movies_rating_counts_RDD = movie_ID_with_avg_ratings_RDD.map(lambda x: (x[0], x[1][0]))


	def _train_model(self):
		"""
		Train the ALS model
		-------------------
		with the current dataset
		"""
		logger.info("Train the ALS model")
		start_time = time.time()
		self.model = ALS.train(self.ratings_RDD, self.rank, seed=self.seed, iterations=self.iterations, lambda_=self.regularization_parameter)
		self.train_time = time.time() - start_time
		logger.info("ALS model built")

	def _predict_ratings(self, user_and_movie_RDD, sentiment):
		"""
		Gets predictions for a given (userID, movieID)
		"""
		sadgenre = 'Comedy'
		mildsadgenre = 'Animation'
		mildhappy = 'Sci-Fi'
		happygenre = 'Adventure'
		if sentiment == 1:
			genre = sadgenre
		elif sentiment == 3:
			genre = mildsadgenre
		elif sentiment = 7:
			genre = mildhappy
		else:		
			genre = happygenre
		predicted_RDD = self.model.predictAll(user_and_movie_RDD)
		predicted_rating_RDD = predicted_RDD.map(lambda x: (x.product, x.rating))
		if sentiment != 5:
			predicted_rating_title_and_count_RDD = \
				predicted_rating_RDD.join(self.movies_titles_RDD.filter(lambda x: genre in x[2])).join(self.movies_rating_counts_RDD)
		else:
			predicted_rating_title_and_count_RDD = \
				predicted_rating_RDD.join(self.movies_titles_RDD).join(self.movies_rating_counts_RDD)		
		#Add for genre filter .filter(lambda tokens: tokens[2] in sadgenres)	
		predicted_rating_title_and_count_RDD = \
			predicted_rating_title_and_count_RDD.map(lambda r: (r[1][0][1], r[1][0][0], r[1][1]))

		return predicted_rating_title_and_count_RDD

	def add_ratings(self, ratings):
		"""
		Add additional movie ratings
		----------------------------
		"""
		new_ratings_RDD = self.sc.parallelize(ratings)
		self.ratings_RDD = self.ratings_RDD.union(new_ratings_RDD)
		self._count_and_average_ratings()
		self._train_model()

		return ratings

	def get_ratings_for_movie_ids(self, user_id, movie_ids):
		"""
		Given the parameters, predict the ratings
		                      -------------------
		"""	
		requested_movies_RDD = self.sc.parallelize(movie_ids).map(lambda x: (user_id, x))
		ratings = self._predict_ratings(requested_movies_RDD).collect()

		return ratings


	def print_movie_RDD(self, movie_titles_RDD):
		'''
		Prints the movie_titles_RDD
		---------------------------
		'''
		movies_with_genre = self.movies_titles_RDD.take(1)[0]
		print movies_with_genre


	def get_top_ratings(self, user_id, movies_count, sentiment):
		"""
		Recommends top movies
		---------------------
		"""
		user_unrated_movies_RDD = self.ratings_RDD.filter(lambda rating: not rating[0] == user_id) \
			.map(lambda x: (user_id, x[1])).distinct()
		ratings = self._predict_ratings(user_unrated_movies_RDD, sentiment).filter(lambda r: r[2]>=25).takeOrdered(movies_count, key=lambda x: -x[1])
		self.print_movie_RDD(self.movies_titles_RDD)

		return ratings


	def __init__(self, sc, dataset_path):
		"""
		Init the recommendation engine given a SparkContext and a dataset path
		"""
		logger.info("Starting the recommendation engine: ")

		self.sc = sc

		logger.info("Loading Ratings data....")
		ratings_file_path = os.path.join(dataset_path, 'ratings.csv')
		ratings_raw_RDD = self.sc.textFile(ratings_file_path)
		ratings_raw_data_header = ratings_raw_RDD.take(1)[0]
		self.ratings_RDD = ratings_raw_RDD.filter(lambda line: line!=ratings_raw_data_header)\
			.map(lambda line: line.split(",")).map(lambda tokens: (int(tokens[0]),int(tokens[1]), float(tokens[2]))).cache()

		logger.info("Loading movies data....")
		movies_file_path = os.path.join(dataset_path, 'movies.csv')
		movies_raw_RDD = self.sc.textFile(movies_file_path)
		movies_raw_data_header = movies_raw_RDD.take(1)[0]
		self.movies_RDD = movies_raw_RDD.filter(lambda line: line!=movies_raw_data_header)\
			.map(lambda line: line.split(",")).map(lambda tokens: (int(tokens[0]), tokens[1], tokens[2])).cache()
		#Add x[2], the genres	
		self.movies_titles_RDD = self.movies_RDD.map(lambda x: (x[0], x[1], x[2].split("|"))).cache()
		self._count_and_average_ratings()

		self.train_time = 0
		self.rank = 8
		self.seed = 5L
		self.iterations = 10
		self.regularization_parameter = 0.1
		self._train_model()

'''
	sc = SparkContext('local', 'test')
	datasets_path = os.getcwd()
	ratings_file_path = os.path.join(datasets_path, 'ratings.csv')
	movies_file_path = os.path.join(datasets_path, 'movies.csv')
	ratings_raw_data = sc.textFile(ratings_file_path)
	movies_raw_data = sc.textFile(movies_file_path)


	ratings_raw_data_header = ratings_raw_data.take(1)[0]
	ratings_data = ratings_raw_data.filter(lambda line: line!=ratings_raw_data_header)\
	.map(lambda line: line.split(",")).map(lambda tokens: (tokens[0],tokens[1],tokens[2])).cache()
	print ratings_data.take(3)

	movies_raw_data_header = movies_raw_data.take(1)[0]
	movies_data = movies_raw_data.filter(lambda line: line!=movies_raw_data_header)\
	.map(lambda line: line.split(",")).map(lambda tokens: (tokens[0],tokens[1],tokens[2])).cache()
	print movies_data.take(3)
	
	training_RDD, validation_RDD, test_RDD = ratings_data.randomSplit([6, 2, 2], seed=0L)
	validation_for_predict_RDD = validation_RDD.map(lambda x: (x[0], x[1]))
	test_for_predict_RDD = test_RDD.map(lambda x: (x[0], x[1]))
	seed = 5L
	iterations = 10
	regularization_parameter = 0.1
	ranks = [4,8,12]
	errors = [0,0,0]
	err = 0
	tolerance = 0.02

	min_error = float('inf')
	best_rank = -1
	best_iteration = -1
	for rank in ranks:
		model = ALS.train(training_RDD, rank, seed=seed, iterations=iterations, lambda_=regularization_parameter)
		predictions = model.predictAll(validation_for_predict_RDD).map(lambda r: ((r[0], r[1]), r[2]))
		rates_and_preds = validation_RDD.map(lambda r: ((int(r[0]), int(r[1])), float(r[2]))).join(predictions)
		error = math.sqrt(rates_and_preds.map(lambda r: (r[1][0] - r[1][1])**2).mean())
		errors[err] = error
		err += 1
		print 'For rank %s the RMSE is %s' % (rank, error)
		if error < min_error:
			min_error = error
			best_rank = rank
	print 'The best model was trained with rank %s' % best_rank	
	model = ALS.train(training_RDD, best_rank, seed=seed, iterations=iterations,lambda_=regularization_parameter)
	predictions = model.predictAll(test_for_predict_RDD).map(lambda r: ((r[0], r[1]), r[2]))
	rates_and_preds = test_RDD.map(lambda r: ((int(r[0]), int(r[1])), float(r[2]))).join(predictions)
	error = math.sqrt(rates_and_preds.map(lambda r: (r[1][0] - r[1][1])**2).mean())
	print 'For testing data the RMSE is %s' % (error)

	
	complete_ratings_filepath = os.path.join(datasets_path, 'ratingsbig.csv')
	complete_movies_filepath = os.path.join(datasets_path, 'moviesbig.csv')
	complete_movies_rawdata = sc.textFile(complete_movies_filepath)
	complete_ratings_rawdata = sc.textFile(complete_ratings_filepath)

	complete_ratings_data_header = complete_ratings_rawdata.take(1)[0]
	complete_ratings_data = complete_ratings_rawdata.filter(lambda line: line!=complete_ratings_data_header)\
	.map(lambda line: line.split(',')).map(lambda tokens: (int(tokens[0]),int(tokens[1]),float(tokens[2]))).cache()
	print complete_ratings_data.take(3)

	complete_movies_data_header = complete_movies_rawdata.take(1)[0]
	complete_movies_data = complete_movies_rawdata.filter(lambda line: line!=complete_movies_data_header)\
	.map(lambda line: line.split(',')).map(lambda tokens: (tokens[0], tokens[1], tokens[2])).cache()
	print "There are %s recommendations in the dataset" % (complete_ratings_data.count())

	
	training_RDD, test_RDD = complete_ratings_data.randomSplit([7,3], seed=0L)

	complete_model = ALS.train(training_RDD, best_rank,seed=seed, iterations=iterations, lambda_=regularization_parameter)

	test_for_predict_RDD = test_RDD.map(lambda x: (x[0], x[1]))

	predictions = complete_model.predictAll(test_for_predict_RDD).map(lambda r: ((r[0], r[1], r[2])))
	rates_and_preds = test_RDD.map(lambda r: (int(r[0]), int(r[1]), float(r[2]))).join(predictions)
	error = math.sqrt(rates_and_preds.map(lambda r: (r[1][0]-r[1][1])**2).mean())

	print "For testing data the RMSE is %s" % (error)
	

	complete_movies_titles = complete_movies_data.map(lambda x: (int(x[0]), x[1], x[2]))

	print "There are %s movies in the complete dataset" % (complete_movies_titles.count())
'''