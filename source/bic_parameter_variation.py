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
import ds_charts as ds


from tqdm import tqdm
from imblearn.over_sampling import SVMSMOTE, RandomOverSampler, SMOTE
from imblearn.combine import SMOTETomek
from sklearn.metrics import roc_curve, accuracy_score, confusion_matrix
from sklearn.metrics import recall_score, f1_score, precision_score, roc_auc_score
from sklearn import svm, metrics
from sklearn.model_selection import StratifiedKFold, LeaveOneOut
from sklearn.utils import resample

import warnings

warnings.filterwarnings('ignore')
rs = 2345
# number of folds
K = 10
# number of max evaluations when looking for optimal parameters
MAX_EVALS = 150
# number of max evaluations when looking for optimal parameters
early = MAX_EVALS//2
# quality of graphics
dpi = 150
# id variable
id = "id"

################################
# Datasets and preprocessing####
################################
data_dir = "./data/"
datasets = ["dataset_forbic.csv"]   #dataset to mine for biclusters
#check if all files exist
for f in datasets:
    if not os.path.exists(data_dir + f): raise Exception(f"File {f} not found")


#all preprocessing will be aplied on the already transformed (with patterns) dataset

targets = ["ipet2"]
targets = targets * len(datasets)

# balancing and scaling (respectively) for each case
#preprocessing = [(RandomOverSampler(), None)]
#preprocessing = [(SVMSMOTE(), None), (SMOTE(), None)]
preprocessing = [(None, None)]
preprocessing = preprocessing * len(datasets)


to_scale = [[]]
to_scale = to_scale * len(datasets)

# to optimize
to_optimize = [({"score": f1_score},)]
to_optimize = to_optimize * len(datasets)

def specificity(y, prdY, labels=(1,0)):
    cfm = confusion_matrix(y, prdY, labels=labels)
    return cfm[1,1]/(cfm[1,1]+cfm[1,0])
# to measure
to_measure = [{"AUC": roc_auc_score,"Precision": partial(precision_score, zero_division=0), "Recall": recall_score,
               "Specificity": eval.specificity},]
to_measure = to_measure * len(datasets)


################################
# Parameters to vary############
################################
variations = {"niter": [1, 3, 5, 7, 9, 11, 13, 15],
              "minlift": [1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7],
              "minbiclusters": [50, 100,  200, 300, 400, 500, 600, 700, 800, 900, 1000],
                "minbiclusters_old": [50, 100, 150, 200, 250, 300, 350],
              "labels": [3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13],
              "transform": ["binary 0.5", "binary 1", "distance"]
              }

#to_vary = "transform" #sys.argv[1]
#print(f"Varying: {to_vary}")

################################
# Aux functions#################
################################
def get_name(model):
    return type(model()).__name__.replace("Classifier", "")


param_dir = "./params/"
cheat = False
feature_selection = False
save_figs = True
bootstrap = False

recalculate_all = False

alpha=0.85
###################################################################################################################

for i in range(0, len(datasets)):
    for to_vary in ["minbiclusters_old"]:
        #defaults
        predictor = GaussianNB
        niter = 3
        minlift = 1.25
        minbiclusters = 100
        labels = 4
        transform = "distance"
        filter_by_lift = True if to_vary == "minbiclusters" else False

        # get data
        path = data_dir + datasets[i]
        print("\n\n" + datasets[i])
        data = pd.read_csv(path)  # .head(n=500)        #BATOTA
        data = data.set_index(id)
        print(f"{data.shape}\n\n")
        target = targets[i]
        nclasses = len(data[target].value_counts())

        y = data[target].values
        X = data.drop([target], axis=1).values

        metric = to_optimize[i]
        balancing, scaling = preprocessing[i]
        feature_scale = to_scale[i]

        values = {key: {} for key in to_measure[i]}
        jumping = False
        for v in variations[to_vary]:
            if jumping:
                break
            #Check if already calculated and load if this is the case
            if not recalculate_all:
                with open(f"./files/bic_var/f_{to_vary}.json", "r") as file:
                    d = json.load(file)
                    all_calc=True
                    for mtemp in to_measure[i]:
                        if f"{mtemp}_{v}" not in d.keys():
                            all_calc = False
                            break
                    if all_calc:
                        for mtemp in to_measure[i]:
                            values[mtemp][v] = d[f'{mtemp}_{v}']
                        print(f"{to_vary} = {v} for {list(to_measure[i].keys())} \tAlready calculated, continuing")
                        continue
                    else:
                        print(f"{to_vary} = {v} for {list(to_measure[i].keys())} \tNot calculated, calculating")


            temp_values = {key: [] for key in to_measure[i]}
            skf = StratifiedKFold(n_splits=K, shuffle=True, random_state=rs)
            for fold, (train_index, test_index) in enumerate(skf.split(X, y)):
                print(f"\n######Fold: {fold}######\n")
                df_train = data.iloc[train_index]
                df_test = data.iloc[test_index]

                #if using bootstrap ignores previous dfs and generates new ones with bootstraping
                if bootstrap:
                    n_samples = int(data.shape[0] * 0.7)    #train size of 0.7
                    df_train = resample(data, n_samples=n_samples, random_state=rs+fold, stratify=data[target], replace=False) #Com True, rows repetidas dentro do train
                    df_test = pd.concat([df_train,data]).drop_duplicates(keep=False)

                #DIFFERENCE HERE, TRANSFORMATION OF THE DATA USING BICPAMS######################################################
                out_train_file = "../bicpams_5.0/output/result_temp1.txt"
                out_test_file = "../bicpams_5.0/output/result_temp2.txt"
                in_file = "../bicpams_5.0/data/result_temp.csv"
                classes_file = "./data/classes.csv"

                if to_vary == "niter": niter = v
                elif to_vary == "minlift": minlift = v
                elif to_vary == "minbiclusters": minbiclusters = v
                elif to_vary == "minbiclusters_old": minbiclusters = v
                elif to_vary == "labels": labels = v
                elif to_vary == "transform": transform = v
                else: raise Exception(f"Parameter {to_vary} not valid.")
                print("###Parameters used##")
                print(f"Nr. Iter:  {niter}\t#\nMin Lift: {minlift}\t#\nMin Bics: {minbiclusters}\t#\nNr. Labels: {labels}\t#")
                print("####################")
                if transform != "distance":
                    transform, binary_threshold = transform.split(" ")
                    binary_threshold = float(binary_threshold)
                else:
                    binary_threshold = None

                #get patterns and transformed data from train dataset
                df_train.to_csv(in_file)
                dir_command = "cd ../bicpams_5.0/"
                jar_command = f"java -jar bic.jar {minbiclusters} {niter} {minlift} {1} {labels}"
                res = os.system(dir_command + ";" + jar_command)
                print("Result of train bicpams: ", res)
                if res != 0:
                    print(f"\n\nError at {to_vary}: {v}\n\n")
                    jumping = True
                    break
                patterns = get_patterns(out_train_file, get_lifts=filter_by_lift)
                if filter_by_lift:
                    patterns = sorted(patterns.items(), key=lambda item: item[1][2], reverse=True)[:minbiclusters] #order by lift
                    patterns = {k: v for k, v in patterns}
                data_disc = get_data(out_train_file)
                df_train = transform_data(data_disc, patterns, classes_file, transform=transform, binary_threshold=binary_threshold)
                print(f"\n\nShape: {df_train.shape}\n\n")

                #get only transformed data from test dataset (variables dont matter)
                df_test.to_csv(in_file)
                jar_command = f"java -jar bic.jar {minbiclusters} {niter} {minlift} {2} {labels}"
                res = os.system(dir_command + ";" + jar_command)
                print("Result of test bicpams: ", res)
                data_disc = get_data(out_test_file)

                df_test = transform_data(data_disc, patterns, classes_file, transform=transform, binary_threshold=binary_threshold)
                ################################################################################################################

                if feature_selection:  # perform feature selection using SVM-RFE
                    svc = SVC(kernel="linear")
                    _, selected = tese_func.model_rfe(svc, df_train, target, scoring='f1', verbose=2)
                    df_train = df_train[selected + [target]]
                    df_test = df_test[selected + [target]]

                #nao usar balancing aqui porque depois vai ser usado para testar no inner cv
                X_train, y_train, X_test, y_test, ids = eval.get_train_and_test(df_train, df_test, target, None, None,
                                                                                [], id=True)

                if not cheat:
                    params = optimize.optimize_model(predictor, df_train, target, metric[0], balancing, scaling, feature_scale,
                                                    MAX_EVALS, k=5, early_stop=early, rs=rs, verbose=True)
                else:
                    params = {}  # DEBUG

                # prediction
                classif = predictor(**params)
                if "random_state" in classif.get_params().keys(): classif.set_params(**{"random_state": rs}) #set random_state in models that support it
                classif.fit(X_train, y_train)
                pred = classif.predict(X_test)

                try:  # need predict_proba or decision_function results to calculate AUC
                    for_auc = classif.predict_proba(X_test)[:, 1]
                except AttributeError:
                    for_auc = classif.decision_function(X_test)

                for measure in to_measure[i]:
                    if (to_measure[i][measure] == roc_auc_score):
                        p = for_auc
                    else:
                        p = pred
                    res = to_measure[i][measure](y_test, p)
                    temp_values[measure].append(res)


            if not jumping:
                #write to file and final dict
                file = open(f"./files/bic_var/f_{to_vary}.json", "r+")
                d = json.load(file)
                for m in temp_values:
                    meanv = statistics.mean(temp_values[m])
                    values[m][v] = meanv
                    d[f"{m}_{v}"] = meanv
                    file.seek(0)
                    json.dump(d, file)
                    file.truncate()
                file.close()


    ### ploting ##########################################################3
        plt.style.use('ggplot')
        fig, axs = plt.subplots(1, 1, figsize=(12, 3), squeeze=False, constrained_layout=True)
        axs = axs.flatten()[0]
        if to_vary == "transform":
            width=0.15
            ind = np.arange(len(variations[to_vary]))
            for b,m in enumerate(values):
                vals = values[m].values()
                bars = axs.bar(ind + width * b, vals, width, bottom=0, label=m, capsize=10, alpha=alpha)
            axs.set_xticks(ind + (width * len(to_measure[0]) / 2) - width / 2)
            axs.set_xticklabels(variations[to_vary], fontsize="medium")

        else:
            for m in values:
                xvals = [xv for xv in values[m]]
                yvals = [values[m][x] for x in xvals]
                plt.plot(xvals, yvals, label=m)
            axs.set_xticks(xvals)
            axs.tick_params(axis='x', which='major', labelsize="medium")
            axs.tick_params(axis='y', which='major', labelsize="medium")
        axs.set_ylim([0,1])
        axs.legend(fontsize="large", loc="best")
        plt.show()
        print(to_vary)
        if save_figs: fig.savefig(f"./images/bic_vars/f_{to_vary}.png", format='png', dpi=dpi)


print("\n\n\n####################END##########################################\n\n\n")