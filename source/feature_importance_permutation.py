import json
import os
import time
import warnings
from functools import partial
from operator import itemgetter

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from imblearn.over_sampling import SVMSMOTE, RandomOverSampler
from sklearn import metrics
from sklearn.ensemble import RandomForestClassifier
from sklearn.exceptions import ConvergenceWarning
import xgboost as xgb
import pickle

from sklearn.inspection import permutation_importance
from sklearn.naive_bayes import GaussianNB
from sklearn.svm import SVC

from myds import evaluation as eval
from myds import datapreprocessing as datapp
from myds import optimize, plot

import tqdm
from sklearn.impute import KNNImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import f1_score, cohen_kappa_score, roc_auc_score, recall_score, precision_score
from sklearn.neighbors import KNeighborsClassifier
from sklearn.neural_network import MLPClassifier
from sklearn.tree import DecisionTreeClassifier
import random
import numpy as np

#warnings.filterwarnings("ignore", category=ConvergenceWarning)
warnings.filterwarnings('ignore')

def custom_recall_join(y, ypred):
    y = np.where(y==2, 1, y)
    ypred = np.where(ypred == 2, 1, ypred)
    return metrics.recall_score(y, ypred, zero_division=0)

rs = 2345


# number of max evaluations when looking for optimal parameters
MAX_EVALS = 300
# number of max evaluations when looking for optimal parameters
early = MAX_EVALS//2
repeats = 1000

n_feat = 20
n_feat_mult = 10
dpi = 300
data_dir = "./data/"
file_dir = "./files/feat_importance/"
id = "id"

datasets = ["dataset_forbic_9_1.3_250_10_True.csv"]
for f in datasets:
    if not os.path.exists(data_dir + f): raise Exception(f"File {f} not found")


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


plt.style.use('ggplot')

models = [GaussianNB]
model_names = ["nb"]
decimals = [8]
show_neg = False
lim = False

for k in range(len(models)):
    model_name = model_names[k]
    decimal = decimals[k]
    for i in range(0, len(datasets)):
        model = models[k]

        #get data
        path = data_dir + datasets[i]
        print("\n\n"+datasets[i]+"\n\n")
        data = pd.read_csv(path)
        data = data.set_index(id)
        target = targets[i]
        columns = data.drop(columns=[target]).columns
        nclasses = len(data[target].value_counts())
        y = data[target].values
        X = data.drop([target], axis=1).values

        if n_feat == -1 or n_feat > X.shape[1]:
            n_feat = X.shape[1]

        metric = to_optimize[i]
        balancing, scaling = preprocessing[i]
        feature_scale = to_scale[i]
        optm_classifs = {0: [], 1: []}

        X_train, y_train = eval.transform_data(data, target, None, None, [])

        param_file_name = datasets[i].replace(".csv", f"_{model_name}_param_calc.json")
        try:
            f = open(file_dir + param_file_name, "r")
            params = json.load(f)
            print("Using already calculated parameters")
        except FileNotFoundError:
            print("Calculating new parameters")
            space_restrit = {'kernel': "linear"} if model == SVC else None
            params = optimize.optimize_model(model, data, target, metric[0], balancing, scaling, feature_scale,
                                             MAX_EVALS, space_restriction=space_restrit,k=5, early_stop=early,
                                             rs=rs, verbose=True)
            f = open(file_dir + param_file_name, "w")
            json.dump(params, f)
        finally:
            f.close()


        model = model(**params)
        if "random_state" in model.get_params().keys():
            model.set_params(**{"random_state": rs})  # set random_state in models that support it
        model.fit(X_train, y_train)


        fi_file_name = datasets[i].replace(".csv", f"_{model_name}_featimp.json")
        try:
            f = open(file_dir + fi_file_name, "r")
            featimp = json.load(f)
            for k in featimp:
                featimp[k] = np.array(featimp[k])
            print("Using already calculated importances")
        except FileNotFoundError:
            print("Calculating new importances")
            a = time.time()
            featimp = permutation_importance(model, X_train, y_train, n_repeats=repeats, random_state=rs, scoring="f1")
            b = time.time()
            print("time: ", b - a)

            temp = {}
            for k in featimp:
                temp[k] = featimp[k].tolist()
            f = open(file_dir + fi_file_name, "w")
            json.dump(temp, f)
        finally:
            f.close()


        featimp = featimp["importances_mean"]
        tops = featimp.argsort()[-n_feat:]
        fig, ax = plt.subplots(figsize=(8, 8))
        vals = featimp[tops]
        colors = ["tab:blue" if a>=0 else "tab:purple" for a in vals]
        rects = plt.barh(np.arange(n_feat), abs(vals), color=colors, alpha=0.9)
        ax.set_yticks(np.arange(n_feat))
        names = columns[tops].tolist()
        #names = [var_name[n] for n in names]
        ax.set_yticklabels(names)
        ax.set_title('')
        if lim: ax.set_xlim(0, 1)
        else: ax.set_xlim(0, max(abs(featimp)) * 1.1)
        ax.set_ylabel("Variable")
        ax.set_xlabel("Importance")


        #v = [round(i, 4) for i in vals]
        plot.autolabel(ax, rects, vals, decimal=decimal, threshold=-100, percentage=False, orient="h")

        path = "./images/feature_importance/" + datasets[i].replace(".csv", f"_{model_name}_plot.png")
        plt.tight_layout()
        plt.show()
        fig.savefig(path, format='png', dpi=dpi)

        #save parameters in json file



