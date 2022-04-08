# -*- coding: utf-8 -*-
import json
import statistics
import sys
import time
from functools import partial
import os

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

#####models##########
from IPython.core.display import display
from dtreeviz.trees import dtreeviz
from sklearn.naive_bayes import BernoulliNB, GaussianNB
from sklearn.tree import DecisionTreeClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier
from xgboost import XGBClassifier
from sklearn.dummy import DummyClassifier

#####my modules#########
import tese_func
from myds import evaluation as eval
from myds import optimize
from bic_functions import get_patterns, transform_data, get_data, complete_transform

from tqdm import tqdm
from imblearn.over_sampling import SVMSMOTE, RandomOverSampler, SMOTE
from imblearn.combine import SMOTETomek
from sklearn.metrics import roc_curve, accuracy_score, precision_recall_curve
from sklearn.metrics import recall_score, f1_score, precision_score, roc_auc_score
from sklearn import svm, metrics
from sklearn.model_selection import StratifiedKFold, LeaveOneOut
from sklearn.utils import resample

import warnings

warnings.filterwarnings('ignore')
rs = 23456

# quality of graphics
dpi = 150
# id variable
id = "id"
################################
# Datasets and preprocessing####
################################
res_dir = "./files/results"
datasets = ["dataset_union1", "dataset_forbic_9_1.3_250_10_True"]
datasets2 = ["dataset_union1.csv", "dataset_forbic.csv"]

legends = ["No space transformation", "With space transformation"]

#targets = ["Outcome"]
targets = ["ipet2"]
targets = targets * len(datasets)

# to measure
to_measure = {"AUC": roc_auc_score,"Precision": partial(precision_score, zero_division=0), "Recall": recall_score
               ,"Specificity": eval.specificity}


################################
# Classifiers###################
################################
classifs = [DummyClassifier, GaussianNB, KNeighborsClassifier, SVC,
            DecisionTreeClassifier, RandomForestClassifier, XGBClassifier]

################################
# Aux functions#################
################################
def get_name(model):
    return type(model()).__name__.replace("Classifier", "")

whole_data = pd.read_csv("./data/data_ipet.csv").set_index(id)

param_dir = "./params/"
file_dir = "./files/"


alpha = 0.85
names_replace = {'Dummy': "Random", 'GaussianNB': 'Naive Bayes', 'KNeighbors': "KNN", 'SVC': "SVM",
                 'DecisionTree': "Decision Tree", 'RandomForest': "Random Forest", 'XGB': "XGBoost"}

###################################################################################################################
plt.style.use('ggplot')
#figsize=(14, 12)
fig, axes = plt.subplots(2, 2, figsize=(15, 10), constrained_layout=True)
axes = axes.flatten()

ind = np.arange(len(classifs))
width = 0.15
s = {}

for i in range(0, len(datasets)):
    #Results
    file_name = datasets[i] + "_results_dict.json"
    file_dict = open(file_dir + 'results/' + file_name, "r")
    results = json.load(file_dict)['0']
    file_dict.close()
    dummy_name = datasets2[i].replace(".csv", f"_dummy_dict.json")
    dummy_dict = open(file_dir + 'results/' + dummy_name, "r")
    dummy_results = json.load(dummy_dict)['0']
    dummy_dict.close()

    results["Dummy"] = dummy_results["Dummy"]
    print(results)

    for b, m in enumerate(to_measure):
        temp_v = [results[a][m] for a in list(results.keys())]
        smean = [t[0] for t in temp_v]
        sstd = [t[1] for t in temp_v]
        bars = axes[b].bar(ind + width * i, smean, width, bottom=0, label=legends[i], capsize=10, yerr=sstd, alpha=alpha)
        axes[b].axhline(y = smean[0], linewidth=1, linestyle="--")
        axes[b].set_xticks(ind + width/2)
        names = list(results.keys())
        names = [names_replace[n] for n in names]
        axes[b].set_xticklabels(names, fontsize="x-large", rotation=15)
        axes[b].tick_params(axis='y', which='major', labelsize="medium")
        axes[b].set_yticks(np.linspace(0, 1, 11))
        axes[b].set_ylim(0, 1)
        axes[b].set_title(m, fontsize="x-large")
        #axes[b].legend(fontsize="x-large")

        axes[i].autoscale_view()

handles, labels = axes[0].get_legend_handles_labels() # single centered label
fig.legend(handles, labels, loc="upper center" ,ncol=len(datasets), fontsize="x-large") # single centered label
fig.savefig(f"./images/bic_vars/f_compare.png", format='png', dpi=250)
fig.show()







