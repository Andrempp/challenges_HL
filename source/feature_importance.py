import json
import os
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

n_feat = 5
n_feat_mult = 10
dpi = 300
figsize = (12, 6)
data_dir = "./data/"
file_dir = "./files/feat_importance/"
id = "id"

datasets = ["dataset_union1.csv"]
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

#####Calculate models###############################################33
var_name = {"ASTH": "Asthma"}

plt.style.use('ggplot')

models = [xgb.XGBClassifier]
model_names = ["xgb"]
decimals = [3]
show_neg = False

for k in range(len(models)):
    feat_imps = {}
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

        for z in [0]:#range(0, len(metric)):
            param_file_name = datasets[i].replace(".csv", f"_{model_name}_param_calc.json")
            try:
                f = open(file_dir + param_file_name, "r")
                params = json.load(f)
                print("Using already calculated parameters")
            except FileNotFoundError:
                print("Calculating new parameters")
                space_restrit = {'kernel': "linear"} if model == SVC else None
                params = optimize.optimize_model(model, data, target, metric[z], balancing, scaling, feature_scale,
                                                 MAX_EVALS, space_restriction=space_restrit,k=5, early_stop=early,
                                                 rs=rs, verbose=True)
                f = open(file_dir + param_file_name, "w")
                json.dump(params, f)
            finally:
                f.close()


            params["random_state"] = rs
            #params = {} #DEBUG

            model = model(**params)
            model.fit(X_train, y_train)

            try:
                feat_imps[i] = model.feature_importances_
                lim = True
                print("feat_imp")
            except AttributeError:
                feat_imps[i] = np.std(X_train, axis=0) * model.coef_[0]
                if not show_neg:
                    feat_imps[i] = abs(feat_imps[i])
                lim = False
                print("coef")


            #feat_imps[i][0] = - feat_imps[i][0]

            tops = abs(feat_imps[i]).argsort()[-n_feat:]
            fig, ax = plt.subplots(figsize=figsize, constrained_layout=True)
            vals = feat_imps[i][tops]
            colors = ["tab:blue" if a>=0 else "tab:purple" for a in vals]
            rects = plt.barh(np.arange(n_feat), abs(vals), color=colors, alpha=0.9)
            ax.set_yticks(np.arange(n_feat))
            names = columns[tops].tolist()
            #names = [var_name[n] for n in names]
            ax.set_yticklabels(names, fontsize="large")
            ax.set_title('')
            #if lim: ax.set_xlim(0, 1)
            ax.set_xlim(0, max(abs(feat_imps[i])) * 1.1)
            ax.set_ylabel("Variable", fontsize="x-large")
            ax.set_xlabel("Importance", fontsize="x-large")


            #v = [round(i, 4) for i in vals]
            plot.autolabel(ax, rects, vals, decimal=decimal, threshold=-100, percentage=False, orient="h")

            path = "./images/feature_importance/" + datasets[i].replace(".csv", f"_{model_name}_plot.png")
            #plt.tight_layout()
            plt.show()
            fig.savefig(path, format='png', dpi=dpi)

            #save parameters in json file


    #save dict with importances
    path = data_dir + datasets[0]
    data = pd.read_csv(path)
    data = data.set_index(id)
    target = targets[0]
    columns = data.drop(columns=[target]).columns

    to_save = {}
    for k in feat_imps.keys():
        to_save[k] = feat_imps[k].tolist()
    file_name = f"feature_importances_{model_name}.json"
    file_dict = open(file_dir  + file_name, "w")
    json.dump(to_save, file_dict)
    file_dict.close()
