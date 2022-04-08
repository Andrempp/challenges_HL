# -*- coding: utf-8 -*-
import json
import statistics
from functools import partial
import os

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
from myds import evaluation as eval
from myds import optimize
from bic_functions import get_patterns, transform_data, get_data, complete_transform

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
# number of folds
K = 10
# number of repetitions
N = 20
# number of max evaluations when looking for optimal parameters
MAX_EVALS = 300
# number of max evaluations when looking for optimal parameters
early = MAX_EVALS//2
# quality of graphics
dpi = 300
# for ROC curves
base_fpr = np.linspace(0, 1, 101)
# id variable
id = "id"
roc = False
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
#preprocessing = [(RandomOverSampler(), None)]
#preprocessing = [(SVMSMOTE(), None)]
preprocessing = [(SVMSMOTE(), None)]
preprocessing = preprocessing * len(datasets)


to_scale = [[]]
to_scale = to_scale * len(datasets)

# to optimize
to_optimize = [({"score": f1_score},)]
to_optimize = to_optimize * len(datasets)


# to measure
to_measure = [{"Precision": partial(precision_score, zero_division=0), "Recall": recall_score, "Specificity": eval.specificity}]
to_measure = to_measure * len(datasets)

#if bootstrap or cross_validation
#bootstraps = [True, True]

#if feature selection with SVMRFE
#feature_selections = [True, False]

################################
# Classifiers###################
################################
classifs = [DummyClassifier]
################################
# Aux functions#################
################################
def get_name(model):
    return type(model()).__name__.replace("Classifier", "")


param_dir = "./params/"
cheat = False
feature_selection = True
plot_tree = False
save_figs = True
bootstrap = False
use_bics = False
###################################################################################################################
for i in range(0, len(datasets)):
    #feature_selection = feature_selections[i]
    #bootstrap = bootstraps[i]
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
    balancing, scaling = preprocessing[i]
    feature_scale = to_scale[i]

    # generate data structures
    results = {}
    for m in to_measure[i]:
        results[m] = {j: [] for j in range(N)}

    file_dir = "./files/"
    param_file_name = datasets[i].replace(".csv", "_param.txt")
    param_file = open(file_dir + param_file_name, "w")
    param_file.write(target + "\n")
    param_file.close()
    for n in range(N):
        skf = StratifiedKFold(n_splits=K, shuffle=True, random_state=rs+n)
        for fold, (train_index, test_index) in enumerate(skf.split(X, y)):
            print(fold)
            df_train = data.iloc[train_index]
            df_test = data.iloc[test_index]

            #if using bootstrap ignores previous dfs and generates new ones with bootstraping
            if bootstrap:
                n_samples = int(data.shape[0] * 0.7)    #train size of 0.7
                df_train = resample(data, n_samples=n_samples, random_state=rs+fold, stratify=data[target], replace=False) #Com True, rows repetidas dentro do train
                df_test = pd.concat([df_train,data]).drop_duplicates(keep=False)

            if use_bics:
                # DIFFERENCE HERE, TRANSFORMATION OF THE DATA USING BICPAMS######################################################
                nrbics = 100
                nrIterations = 3
                minlift = 1.25
                out_train_file = "../bicpams_5.0/output/result_temp1.txt"
                out_test_file = "../bicpams_5.0/output/result_temp2.txt"
                in_file = "../bicpams_5.0/data/result_temp.csv"
                classes_file = "./data/classes.csv"

                # get patterns and transformed data from train dataset
                df_train.to_csv(in_file)
                dir_command = "cd ../bicpams_5.0/"
                jar_command = f"java -jar bic.jar {nrbics} {nrIterations} {minlift} {1}"
                os.system(dir_command + ";" + jar_command)
                patterns = get_patterns(out_train_file)
                data_disc = get_data(out_train_file)
                df_train = transform_data(data_disc, patterns, classes_file)

                # get only transformed data from test dataset (variables dont matter)
                df_test.to_csv(in_file)
                jar_command = f"java -jar bic.jar {nrbics} {nrIterations} {minlift} {2}"
                os.system(dir_command + ";" + jar_command)
                data_disc = get_data(out_test_file)
                df_test = transform_data(data_disc, patterns, classes_file)

                ################################################################################################################

            if not use_bics and feature_selection:  # perform feature selection using SVM-RFE
                svc = SVC(kernel="linear")
                _, selected = tese_func.model_rfe(svc, df_train, target, scoring='f1', verbose=1)
                df_train = df_train[selected + [target]]
                df_test = df_test[selected + [target]]

            #nao usar balancing aqui porque depois vai ser usado para testar no inner cv
            X_train, y_train, X_test, y_test, ids = eval.get_train_and_test(df_train, df_test, target, None, None,
                                                                            [], id=True)

            optm_classifs = {0: [], 1: []}
            tprs = []  # true positive rates for ROC

            name = get_name(classifs[0])
            print(name)
            if cheat:
                bal_name = str(balancing)[:-2]
                m_name = str(metric[z]["score"]).split(" ")[1]
                t = datasets[i].replace(".csv", f"_{name}_{m_name}_{bal_name}.json")
                t_file = open(param_dir + t, "r")
                params = json.load(t_file)
            else:
                model = classifs[0]
                params = optimize.optimize_model(model, df_train, target, metric[0], balancing, scaling, feature_scale,
                                                 MAX_EVALS, k=5, early_stop=early, rs=rs, verbose=True)
            # params = {}  # DEBUG
            # print(params)

            # save parameters in txt file
            param_file = open(file_dir + param_file_name, "a")
            to_write = "{} - {}\n{}\n\n".format(name, metric[0], str(params))
            param_file.write(to_write)
            param_file.close()

            # prediction
            classif = classifs[0](**params)
            if "random_state" in classif.get_params().keys(): classif.set_params(**{"random_state": rs+n+fold}) #set random_state in models that support it
            classif.fit(X_train, y_train)
            pred = classif.predict(X_test)

            #draw trees
            if plot_tree and classif.__class__.__name__=="DecisionTreeClassifier":
                score = f1_score(y_test, pred)
                feats = df_train.drop(target, axis=1).columns
                viz = dtreeviz(classif, X_train, y_train, target_name=target, feature_names=feats,
                               title=f'F1-score: {score}',class_names=["Negative", "Positive"])
                viz.save(f"./images/plot_tree/tree{fold}_{feature_selection}.svg")

            if nclasses == 2 and roc:
                # ROC values
                pred_prob = classif.predict_proba(X_test)
                fpr, tpr, _ = roc_curve(y_test, pred_prob[:, 1])
                tpr = np.interp(base_fpr, fpr, tpr)
                tpr[0] = 0.0

            measures = []
            for measure in to_measure[i]:
                res = to_measure[i][measure](y_test, pred)
                results[measure][n].append(res)


    print(results)
    # plotting
    plt.style.use('ggplot')
    fig, axes = plt.subplots(len(to_measure[i]), 1, figsize=(12, 10))
    axes = axes.flatten()
    for ax, measure in zip(axes, to_measure[i].keys()):
        print(measure)
        ind = np.arange(N)
        width = 0.15
        s = {}

        means = []
        stdevs = []
        for b, m in enumerate(results[measure].keys()):
            vals = []
            for key in range(b+1):
                vals += results[measure][key]
            means.append(statistics.mean(vals))
            stdevs.append(statistics.stdev(vals))
        ax.bar(ind, means, width, bottom=0, capsize=10, yerr=stdevs)

        print(f"Final mean {means[-1]}")
        ax.axhline(y=means[-1], color='b', linestyle='--', linewidth=1, label=f"Convergence at {round(means[-1],2)}")
        #ax.text(x=10, y=means[-1]+0.01, s='Holiday in US', alpha=0.9, color='#334f8d')

        metric_name = str(metric[0]).split("<")[-1].split(" at")[0]
        ax.set_title(measure)
        ax.set_xticks(ind)
        names = list(range(1, N+1))
        ax.set_xticklabels(names)
        ax.set_yticks(np.linspace(0, 1, 11))
        ax.set_ylim(0, means[-1]+stdevs[-1] + 0.15)
        ax.legend(fontsize=15, loc="upper center")
        ax.autoscale_view()

    symbol="_"
    symbol += "fs" if feature_selection else ""
    symbol += "boot" if bootstrap else ""
    if use_bics: symbol += str(nrIterations) + str(minlift)[-2:]
    path = "./images/classifiers/" + datasets[i].replace(".csv", f"_dummyconv.png")
    plt.tight_layout()
    plt.show()
    if save_figs: fig.savefig(path, format='png', dpi=dpi)

