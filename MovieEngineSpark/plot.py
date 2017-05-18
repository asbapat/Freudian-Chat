import os, time
from pyspark import SparkContext
from pyspark.mllib.recommendation import ALS
import math
import re
import plotly.plotly as py
import plotly.graph_objs as go
import pandas as pd
import matplotlib.pyplot as plt

import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

sc = SparkContext('local', 'test')
datasets_path = os.getcwd()
ratings_file_path = os.path.join(datasets_path, 'ratingsbig.csv')
movies_file_path = os.path.join(datasets_path, 'ratings.csv')
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
'''
training_RDD, validation_RDD, test_RDD = ratings_data.randomSplit([6, 2, 2], seed=0L)
validation_for_predict_RDD = validation_RDD.map(lambda x: (x[0], x[1]))
test_for_predict_RDD = test_RDD.map(lambda x: (x[0], x[1]))
seed = 5L
iterations = 10
regularization_parameter = 0.1
ranks = [2, 4, 6, 8, 10, 12]
errors = [0,0,0,0,0,0]
train_times = [0,0,0,0,0,0]
err = 0
tolerance = 0.02

min_error = float('inf')
best_rank = -1
best_iteration = -1
for rank in ranks:
	start_time = time.time()
	model = ALS.train(training_RDD, rank, seed=seed, iterations=iterations, lambda_=regularization_parameter)
	train_times[err] = time.time() - start_time
	start_time = 0
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

plt.figure()
plt.bar(ranks,errors,align='center')
plt.xlabel('Ranks')
plt.ylabel('RMSE values')
plt.axis([0,14,0.94,0.97])
plt.show()
'''
best_rank = 2
seed = 5L
regularization_parameter = 0.1
iterations = 10
train_times = [0, 0]
dataset_sizes = [20,1]
start_time = time.time()
model = ALS.train(ratings_data, best_rank, seed=seed, iterations=iterations,lambda_=regularization_parameter)
train_times[0] = int(time.time() - start_time)
start_time = time.time()
model = ALS.train(movies_data, best_rank, seed=seed, iterations=iterations,lambda_=regularization_parameter)
train_times[1] = int(time.time() - start_time)
plt.figure()
plt.bar(dataset_sizes,train_times,align='center')
plt.xlabel('Dataset Sizes in Millions')
plt.ylabel('Time for Training ALS Model')
plt.show()