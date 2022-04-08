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
import ds_charts as ds


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
K = 5
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
datasets = ["dataset_forbic.csv"]   #dataset to mine for biclusters
#check if all files exist
for f in datasets:
    if not os.path.exists(data_dir + f): raise Exception(f"File {f} not found")


#all preprocessing will be aplied on the already transformed (with patters) dataset

#targets = ["Outcome"]
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


# to measure
to_measure = [("F1", f1_score)]
to_measure = to_measure * len(datasets)

################################
# Classifiers###################
################################
#classifs = [DummyClassifier, SVC, DecisionTreeClassifier, RandomForestClassifier]
################################
# Aux functions#################
################################
def get_name(model):
    return type(model()).__name__.replace("Classifier", "")


param_dir = "./params/"
cheat = False
feature_selection = False
plot_tree = False
save_figs = True
bootstrap = False

param_file = "nriterations"
use_file = False
###################################################################################################################
for i in range(0, len(datasets)):
    # variables to change
    nrIterations = [3, 5, 7, 9, 11]  # x-axis
    classifs = [DummyClassifier, GaussianNB, KNeighborsClassifier, SVC,
                DecisionTreeClassifier, RandomForestClassifier, XGBClassifier]  # lines

    if not use_file:
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

        file_dir = "./files/"

        values = {}
        for j in range(len(classifs)):
            yvalues = []
            for nrIter in nrIterations:
                skf = StratifiedKFold(n_splits=K, shuffle=True, random_state=rs)
                temp_yvalues = []
                for fold, (train_index, test_index) in enumerate(skf.split(X, y)):
                    print(fold)
                    df_train = data.iloc[train_index]
                    df_test = data.iloc[test_index]

                    #if using bootstrap ignores previous dfs and generates new ones with bootstraping
                    if bootstrap:
                        n_samples = int(data.shape[0] * 0.7)    #train size of 0.7
                        df_train = resample(data, n_samples=n_samples, random_state=rs+fold, stratify=data[target], replace=False) #Com True, rows repetidas dentro do train
                        df_test = pd.concat([df_train,data]).drop_duplicates(keep=False)


                    #DIFFERENCE HERE, TRANSFORMATION OF THE DATA USING BICPAMS######################################################
                    nrbics = 100
                    minlift = 1.25
                    labels=4
                    out_train_file = "../bicpams_5.0/output/result_temp1.txt"
                    out_test_file = "../bicpams_5.0/output/result_temp2.txt"
                    in_file = "../bicpams_5.0/data/result_temp.csv"
                    classes_file = "./data/classes.csv"

                    #get patterns and transformed data from train dataset
                    df_train.to_csv(in_file)
                    dir_command = "cd ../bicpams_5.0/"
                    jar_command = f"java -jar bic.jar {nrbics} {nrIter} {minlift} {1} {labels}"
                    os.system(dir_command + ";" + jar_command)
                    patterns = get_patterns(out_train_file)
                    data_disc = get_data(out_train_file)
                    df_train = transform_data(data_disc, patterns, classes_file)

                    #get only transformed data from test dataset (variables dont matter)
                    df_test.to_csv(in_file)
                    jar_command = f"java -jar bic.jar {nrbics} {nrIter} {minlift} {2} {labels}"
                    os.system(dir_command + ";" + jar_command)
                    data_disc = get_data(out_test_file)
                    df_test = transform_data(data_disc, patterns, classes_file)

                    ################################################################################################################


                    if feature_selection:  # perform feature selection using SVM-RFE
                        svc = SVC(kernel="linear")
                        _, selected = tese_func.model_rfe(svc, df_train, target, scoring='f1', verbose=2)
                        df_train = df_train[selected + [target]]
                        df_test = df_test[selected + [target]]

                    #nao usar balancing aqui porque depois vai ser usado para testar no inner cv
                    X_train, y_train, X_test, y_test, ids = eval.get_train_and_test(df_train, df_test, target, None, None,
                                                                                    [], id=True)

                    name = get_name(classifs[j])
                    print(name)

                    model = classifs[j]
                    if not cheat:
                        params = optimize.optimize_model(model, df_train, target, metric[0], balancing, scaling, feature_scale,
                                                    MAX_EVALS, k=5, early_stop=early, rs=rs, verbose=True)
                    else:
                        params = {}
                    # params = {}  # DEBUG
                    # print(params)

                    # # save parameters in txt file
                    # param_file = open(file_dir + param_file_name, "a")
                    # to_write = "{} - {}\n{}\n\n".format(name, metric[0], str(params))
                    # param_file.write(to_write)
                    # param_file.close()

                    # prediction
                    classif = classifs[j](**params)
                    if "random_state" in classif.get_params().keys(): classif.set_params(**{"random_state": rs}) #set random_state in models that support it
                    classif.fit(X_train, y_train)
                    pred = classif.predict(X_test)
                    temp_yvalues.append(to_measure[0][1](y_test, pred))
                    # try:                                     #need predict_proba or decision_function results to calculate AUC
                    #     for_auc = classif.predict_proba(X_test)[:, 1]
                    # except AttributeError:
                    #     for_auc = classif.decision_function(X_test)

                yvalues.append(statistics.mean(temp_yvalues))

            values[get_name(classifs[j])] = yvalues

        f = open(f"./params/bic_var/{param_file}.json", "w")
        json.dump(values, f)
        f.close()
    else:
        f = open(f"./params/bic_var/{param_file}.json", "r")
        values = json.load(f)
        print(values)
        f.close()

    values["Dummy"] = [0.53] * len(nrIterations)
    plt.figure()
    fig, axs = plt.subplots(1, 1, figsize=(14, 6), squeeze=False)

    ds.multiple_line_chart(nrIterations, values, ax=axs[0, 0], title='AUC of classifiers per number of iterations',
                           xlabel='Number of iterations', ylabel='AUC', percentage=True)
    plt.tight_layout()
    #plt.show()
    fig.savefig(f"./images/bic_vars/{param_file}.png", format='png', dpi=dpi)