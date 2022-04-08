# -*- coding: utf-8 -*-
import json
import statistics
from functools import partial
import os
from cairosvg import svg2png

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

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
from myds import evaluation as evalZ
from myds import optimize


from tqdm import tqdm
from imblearn.over_sampling import SVMSMOTE, RandomOverSampler, SMOTE
from imblearn.combine import SMOTETomek
from sklearn.metrics import roc_curve, accuracy_score
from sklearn.metrics import recall_score, f1_score, precision_score, roc_auc_score
from sklearn import svm, metrics
from sklearn.model_selection import StratifiedKFold, LeaveOneOut
from sklearn.utils import resample

import warnings

warnings.filterwarnings('ignore')
rs = 2345
# number of max evaluations when looking for optimal parameters
MAX_EVALS = 300
# number of max evaluations when looking for optimal parameters
early = MAX_EVALS//2
# quality of graphics
dpi = 300
# id variable
id = "id"
################################
# Datasets and preprocessing####
################################
data_dir = "./data/"
datasets = ["dataset_union1.csv"]
#check if all files exist
for f in datasets:
    if not os.path.exists(data_dir + f): raise Exception(f"File {f} not found")


#targets = ["Outcome"]
targets = ["ipet2"]
targets = targets * len(datasets)

# balancing and scaling (respectively) for each case
preprocessing = [(None, None)]
preprocessing = preprocessing * len(datasets)


to_scale = [[]]
to_scale = to_scale * len(datasets)

# to optimize
to_optimize = [({"score": f1_score},)]
to_optimize = to_optimize * len(datasets)


# to measure
to_measure = [{"AUC": roc_auc_score, "F1": f1_score, "Precision": partial(precision_score, zero_division=0),
               "Recall": recall_score}]
to_measure = to_measure * len(datasets)


################################
# Classifiers###################
################################
classifs = [DecisionTreeClassifier]
################################
# Aux functions#################
################################
def get_name(model):
    return type(model()).__name__.replace("Classifier", "")


param_dir = "./params/tree/"
save_figs = True
###################################################################################################################
for i in range(0, len(datasets)):
    # get data
    path = data_dir + datasets[i]
    print("\n\n" + datasets[i] + "\n\n")
    data = pd.read_csv(path)  # .head(n=500)        #BATOTA
    data = data.set_index(id)
    print(data.shape)
    target = targets[i]
    nclasses = len(data[target].value_counts())

    y = data[target].values
    X = data.drop([target], axis=1).values

    metric = to_optimize[i]
    _ , scaling = preprocessing[i]
    feature_scale = to_scale[i]

    # generate data structures
    results = {j: {get_name(c): [] for c in classifs} for j in range(len(metric))}


    #nao usar balancing aqui porque depois vai ser usado para testar no inner cv
    X_train = data.drop(target, axis=1).values
    y_train = data[target].values

    optm_classifs = {0: [], 1: []}
    for j in range(0, len(classifs)):
        name = get_name(classifs[j])
        print(name)
        for z in range(0, len(metric)):
            model = classifs[j]

            param_file_name = datasets[i].replace(".csv", "_treeparam.json")
            try:
                f = open(param_dir + param_file_name, "r")
                params = json.load(f)
                print("Using already calculated parameters")
            except FileNotFoundError:
                print("Calculating new parameters")
                params = optimize.optimize_model(model, data, target, metric[z], None, scaling, feature_scale,
                                                 MAX_EVALS, k=5, early_stop=early, rs=rs, verbose=True)
                f = open(param_dir + param_file_name, "w")
                json.dump(params, f)
            finally:
                f.close()

            # params = {}  # DEBUG
            # print(params)

            # prediction
            classif = classifs[j](**params)
            if "random_state" in classif.get_params().keys(): classif.set_params(**{"random_state": rs}) #set random_state in models that support it
            classif.fit(X_train, y_train)

            #draw trees
            feats = data.drop(target, axis=1).columns
            viz = dtreeviz(classif, X_train, y_train, target_name=target, feature_names=feats,
                           title=f'Decision Tree',class_names=["Negative", "Positive"])
            file_name = datasets[i].replace(".csv", "_tree")
            viz.save(f"./images/plot_tree/{file_name}.svg")
            with open(f"./images/plot_tree/{file_name}.svg", "rb") as f:
                svg2png(file_obj=f, write_to=f"./images/plot_tree/{file_name}.png")

            os.remove(f"./images/plot_tree/{file_name}.svg")
            os.remove(f"./images/plot_tree/{file_name}")
