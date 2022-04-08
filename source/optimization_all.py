# -*- coding: utf-8 -*-
import json
import statistics
from functools import partial

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

#####models##########
from sklearn.naive_bayes import BernoulliNB, GaussianNB
from sklearn.tree import DecisionTreeClassifier
from sklearn.svm import SVC
from sklearn.neighbors import KNeighborsClassifier
from sklearn.ensemble import RandomForestClassifier
from xgboost import XGBClassifier

#####my modules#########
from myds import evaluation as eval
from myds import optimize

from tqdm import tqdm
from imblearn.over_sampling import SVMSMOTE
from imblearn.combine import SMOTETomek
from sklearn.metrics import roc_curve, accuracy_score
from sklearn.metrics import recall_score, f1_score, precision_score, roc_auc_score
from sklearn import svm, metrics
from sklearn.model_selection import StratifiedKFold

import warnings

warnings.filterwarnings('ignore')


# number of folds
K = 5
# number of max evaluations when looking for optimal parameters
MAX_EVALS = 300
# number of max evaluations without improvement until early stopping
early = MAX_EVALS//2
# quality of graphics
dpi = 300
# for ROC curves
base_fpr = np.linspace(0, 1, 101)
# id variable
id = "id"

################################
# Datasets and preprocessing####
################################
data_dir = "./data/"
# Order followed (in all data structures): hospitalization, ic without hospitalization, ic with hospitalization,
# outcome withou hospitalization, outcome with hospitalization, outcome with ic, respiratory support with hospitalization
datasets = ["dataset_svm.csv"]
#datasets = ["dataset_union1.csv"]

targets = ["ipet2"]

# balancing and scaling (respectively) for each case
#preprocessing = [(SVMSMOTE(), None)]
preprocessing = [(SMOTETomek(), None)]

to_scale = [[]]

# to optimize
to_optimize = [({"score": roc_auc_score},)]

################################
# Classifiers###################
################################
classifs = [GaussianNB, KNeighborsClassifier, SVC,
            DecisionTreeClassifier, RandomForestClassifier, XGBClassifier]


################################
# Aux functions#################
################################
def get_name(model):
    return type(model()).__name__.replace("Classifier", "")

###################################################################################################################

for i in range(0, len(datasets)):
    # get data
    path = data_dir + datasets[i]
    print("\n\n" + datasets[i] + "\n\n")
    data = pd.read_csv(path)  # .head(n=500)        #BATOTA
    #data = data.append([data] * 5, ignore_index=True)
    data = data.set_index(id)

    target = targets[i]
    nclasses = len(data[target].value_counts())
    y = data[target].values
    X = data.drop([target], axis=1).values

    metric = to_optimize[i]
    balancing, scaling = preprocessing[i]
    feature_scale = to_scale[i]

    param_dir = "./params/"

    for j in range(0, len(classifs)):
        for z in range(0, len(metric)):
            name = get_name(classifs[j])

            m_name = str(metric[z]["score"]).split(" ")[1]
            bal_name = str(balancing)[:-2]
            model = classifs[j]
            params = optimize.optimize_model(model, data, target, metric[z], balancing, scaling, feature_scale,
                                             MAX_EVALS, k=K, early_stop=early)
            #params = {}  # DEBUG


            # save parameters in txt file
            param_file_name = datasets[i].replace(".csv", f"_{name}_{m_name}_{bal_name}.json")
            print(param_file_name)
            param_file = open(param_dir + param_file_name, "w")
            json.dump(params, param_file)
            param_file.close()
