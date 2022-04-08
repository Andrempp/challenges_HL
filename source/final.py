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
# number of folds
K = 10
# number of max evaluations when looking for optimal parameters
MAX_EVALS = 300
# number of max evaluations when looking for optimal parameters
early = MAX_EVALS//2
# quality of graphics
dpi = 200
# for ROC curves
base_fpr = np.linspace(0, 1, 51)
# id variable
id = "id"
################################
# Datasets and preprocessing####
################################
data_dir = "./data/"
datasets = ["dataset_svm.csv", "dataset_union1.csv"]
#check if all files exist
for f in datasets:
    if not os.path.exists(data_dir + f): raise Exception(f"File {f} not found")


#targets = ["Outcome"]
targets = ["ipet2"]
targets = targets * len(datasets)

# balancing and scaling (respectively) for each case
#preprocessing = [(RandomOverSampler(), None)]
#preprocessing = [(SVMSMOTE(), None)]
preprocessing = [(None, None)]
preprocessing = preprocessing * len(datasets)


to_scale = [[]]
to_scale = to_scale * len(datasets)

# to optimize
to_optimize = [({"score": f1_score},)]
to_optimize = to_optimize * len(datasets)


# to measure
to_measure = [{"AUC": roc_auc_score, "Precision": partial(precision_score, zero_division=0), "Recall": recall_score,"Specificity": eval.specificity}]

to_measure = to_measure * len(datasets)

#if bootstrap or cross_validation
bootstraps = [False, False]

#if feature selection with SVMRFE
feature_selections = [False, False]

#best predictors
best_pred = ["SVC", "XGB"]

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

cheat = False
plot_tree = False
save_figs = False
roc = True
bicpams = False

use_file = True
write_file = False

##bicpams parameters#######################
l_nrbics = [250]
l_nrIterations = [9]
l_minlift = [1.3]
l_labels = [10]
l_filter_by_lift = [True]
###################################################################################################################
plt.style.use('ggplot')
fig, axes = plt.subplots(len(datasets), 1, figsize=(14, 4), constrained_layout=True)
fig_roc, axes_roc = plt.subplots(1, len(datasets), figsize=(14, 8), constrained_layout=True)
fig_prc, axes_prc = plt.subplots(1, len(datasets), figsize=(9, 4), constrained_layout=True)
fig_wp, axes_wp = plt.subplots(len(datasets)*2, 2, figsize=(10, 4), constrained_layout=True)
alpha = 0.85
names_replace = {'Dummy': "Random", 'GaussianNB': 'Naive Bayes', 'KNeighbors': "KNN", 'SVC': "SVM",
                 'DecisionTree': "Decision Tree", 'RandomForest': "Random Forest", 'XGB': "XGBoost"}

if len(datasets) == 1:
    axes = [axes]
    axes_roc = [axes_roc]
    axes_prc = [axes_prc]

    #axes_wp = [axes_wp]
else:
    axes = axes.flatten()
    axes_roc = axes_roc.flatten()
    axes_prc = axes_prc.flatten()

for i in range(0, len(datasets)):
    feature_selection = feature_selections[i]
    bootstrap = bootstraps[i]
    # get data
    path = data_dir + datasets[i]
    print("\n\n" + datasets[i] + "\n\n")
    data = pd.read_csv(path)  # .head(n=500)        #BATOTA
    data = data.set_index(id)
    print(data.shape)
    target = targets[i]
    nclasses = len(data[target].value_counts())

    file_name = datasets[i].replace(".csv", "_results_dict.json")

    metric = to_optimize[i]
    balancing, scaling = preprocessing[i]
    feature_scale = to_scale[i]

    if bicpams == True:
        nrbics = l_nrbics[i]
        nrIterations = l_nrIterations[i]
        minlift = l_minlift[i]
        labels = l_labels[i]
        filter_by_lift = l_filter_by_lift[i]
        sufix = f"_{nrIterations}_{minlift}_{nrbics}_{labels}_{filter_by_lift}"
        if feature_selection: sufix += "_fs"
        if balancing != None: sufix += "_svmsmote"
    else:
        sufix = ""

    if not use_file:
        print("*"*30)
        print("Not using precalculated values")
        print("*"*30)

        y = data[target].values
        X = data.drop([target], axis=1).values

        # generate data structures
        results = {j: {get_name(c): [] for c in classifs} for j in range(len(metric))}
        roc_vals = {j: {get_name(c): [] for c in classifs} for j in range(len(metric))}
        prc_vals = {j: {get_name(c): [] for c in classifs} for j in range(len(metric))}
        wrong_preds = {j: {get_name(c): [] for c in classifs} for j in range(len(metric))}

        param_file_name = datasets[i].replace(".csv", "_param.txt")
        param_file = open(file_dir + param_file_name, "w")
        param_file.write(target + "\n")
        param_file.close()

        skf = StratifiedKFold(n_splits=K, shuffle=True, random_state=rs)
        for fold, (train_index, test_index) in enumerate(skf.split(X, y)):
            print(f"\n######Fold: {fold}######\n")
            df_train = data.iloc[train_index]
            df_test = data.iloc[test_index]

            #if using bootstrap ignores previous dfs and generates new ones with bootstraping##########################
            if bootstrap:
                n_samples = int(data.shape[0] * 0.7)    #train size of 0.7
                df_train = resample(data, n_samples=n_samples, random_state=rs+fold, stratify=data[target], replace=False) #Com True, rows repetidas dentro do train
                df_test = pd.concat([df_train,data]).drop_duplicates(keep=False)

            #use BICPAMS to transform the feature space into patterns##############################################
            if bicpams:
                out_train_file = "../bicpams_5.0/output/result_temp1.txt"
                out_test_file = "../bicpams_5.0/output/result_temp2.txt"
                in_file = "../bicpams_5.0/data/result_temp.csv"
                classes_file = "./data/classes.csv"

                print("###Parameters used##")
                print(f"Nr. Iter:  {nrIterations}\t#\nMin Lift: {minlift}\t#\nMin Bics: {nrbics}\t#\nNr. Labels: {labels}\t#\nFilter lift: {filter_by_lift}\t#")
                print("####################")

                # get patterns and transformed data from train dataset
                df_train.to_csv(in_file)
                dir_command = "cd ../bicpams_5.0/"
                jar_command = f"java -jar bic.jar {nrbics} {nrIterations} {minlift} {1} {labels} {filter_by_lift}"
                os.system(dir_command + ";" + jar_command)
                patterns = get_patterns(out_train_file, get_lifts=filter_by_lift)
                if filter_by_lift:
                    patterns = sorted(patterns.items(), key=lambda item: item[1][2], reverse=True)[:nrbics] #order by lift
                    patterns = {k: v for k, v in patterns}
                data_disc = get_data(out_train_file)
                df_train = transform_data(data_disc, patterns, classes_file)

                # get only transformed data from test dataset (variables dont matter)
                df_test.to_csv(in_file)
                jar_command = f"java -jar bic.jar {nrbics} {nrIterations} {minlift} {2} {labels} {filter_by_lift}"
                os.system(dir_command + ";" + jar_command)
                data_disc = get_data(out_test_file)
                df_test = transform_data(data_disc, patterns, classes_file)

                print(f"\n\nShape: {df_train.shape}\n\n")


            # perform feature selection using SVM-RFE###################################################################
            if feature_selection:
                svc = SVC(kernel="linear")
                _, selected = tese_func.model_rfe(svc, df_train, target, scoring='f1', verbose=1)
                df_train = df_train[selected + [target]]
                df_test = df_test[selected + [target]]

            #nao usar balancing aqui porque depois vai ser usado para testar no inner cv
            X_train, y_train, X_test, y_test, ids = eval.get_train_and_test(df_train, df_test, target, None, None,
                                                                            [], id=True)

            optm_classifs = {0: [], 1: []}
            tprs = []  # true positive rates for ROC
            for j in range(0, len(classifs)):
                name = get_name(classifs[j])
                print(name)
                for z in range(0, len(metric)):
                    if not cheat:
                        model = classifs[j]
                        params = optimize.optimize_model(model, df_train, target, metric[z], balancing, scaling, feature_scale,
                                                         MAX_EVALS, k=5, early_stop=early, rs=rs, verbose=True)
                    else:
                        params = {}  # DEBUG
                    # print(params)

                    # save parameters in txt file
                    param_file = open(file_dir + param_file_name, "a")
                    to_write = "{} - {}\n{}\n\n".format(name, metric[z], str(params))
                    param_file.write(to_write)
                    param_file.close()

                    # prediction
                    classif = classifs[j](**params)
                    if "random_state" in classif.get_params().keys(): classif.set_params(**{"random_state": rs}) #set random_state in models that support it
                    classif.fit(X_train, y_train)
                    pred = classif.predict(X_test)
                    wrong_ids = ids[pred != y_test].tolist()
                    wrong_preds[z][name] += wrong_ids
                    try:                                     #need predict_proba or decision_function results to calculate AUC
                        for_auc = classif.predict_proba(X_test)[:, 1]
                    except AttributeError:
                        for_auc = classif.decision_function(X_test)


                    #draw trees
                    if plot_tree and classif.__class__.__name__=="DecisionTreeClassifier":
                        score = f1_score(y_test, pred)
                        feats = df_train.drop(target, axis=1).columns
                        viz = dtreeviz(classif, X_train, y_train, target_name=target, feature_names=feats,
                                       title=f'F1-score: {score}',class_names=["Negative", "Positive"])
                        viz.save(f"./images/plot_tree/tree{fold}_{feature_selection}.svg")

                    # ROC and Prec-Recall values
                    if nclasses == 2:
                        #ROC
                        fpr, tpr, trs = roc_curve(y_test, for_auc)
                        tpr = np.interp(base_fpr, fpr, tpr)
                        roc_vals[z][name].append(tpr.tolist())

                        #Precision-Recall curve
                        fpr, tpr, _ = precision_recall_curve(y_test, for_auc)
                        tpr = np.interp(base_fpr, fpr, tpr)
                        prc_vals[z][name].append(tpr.tolist())

                    measures = []
                    for measure in to_measure[i]:
                        if (to_measure[i][measure] == roc_auc_score):
                            p = for_auc
                        else:
                            p = pred
                        res = to_measure[i][measure](y_test, p)
                        measures.append(res)
                    results[z][name].append(tuple(measures))


        # end of dataset
        # save results in file
        for z in range(0, len(metric)):
            temp_res = results[z]
            for j in range(0, len(classifs)):
                name = str(classifs[j]).split('.')[-1].replace('Classifier\'>', '').replace('\'>', '')
                temp_dict = {}
                for mi, m in enumerate(to_measure[i]):
                    vals = [t[mi] for t in temp_res[name]]
                    mean_v = statistics.mean(vals)
                    std_v = statistics.stdev(vals)
                    temp_dict[m] = (mean_v, std_v)
                temp_res[name] = temp_dict

        if write_file:
            file_name = datasets[i].replace(".csv", f"{sufix}_results_dict.json")
            file_dict = open(file_dir + 'results/' + file_name, "w")
            json.dump(results, file_dict)
            file_dict.close()

            # wrong preds
            file_name = datasets[i].replace(".csv", f"{sufix}_wrongpred_dict.json")
            file_dict = open(file_dir + 'wrongpred/' + file_name, "w")
            json.dump(wrong_preds, file_dict)
            file_dict.close()

            # ROC and Prec-Recall values
            if nclasses == 2:
                #ROC
                file_name = datasets[i].replace(".csv", f"{sufix}_roc_dict.json")
                file_dict = open(file_dir + "roc/" + file_name, "w")
                json.dump(roc_vals, file_dict)
                file_dict.close()
                # Precision-Recall
                file_name = datasets[i].replace(".csv", f"{sufix}_prc_dict.json")
                file_dict = open(file_dir + "prc/" + file_name, "w")
                json.dump(prc_vals, file_dict)
                file_dict.close()

    else:
        print("*"*30)
        print("Using precalculated values")
        print("*"*30)
        #Results
        file_name = datasets[i].replace(".csv", f"{sufix}_results_dict.json")
        file_dict = open(file_dir + 'results/' + file_name, "r")
        results = json.load(file_dict)
        file_dict.close()
        results = {int(k): v for k, v in results.items()}
        dummy_name = datasets[i].replace(".csv", f"_dummy_dict.json")
        dummy_dict = open(file_dir + 'results/' + dummy_name, "r")
        dummy_results = json.load(dummy_dict)
        dummy_dict.close()
        dummy_results = {int(k): v for k, v in dummy_results.items()}

        results[0]["Dummy"] = dummy_results[0]["Dummy"]

        #ROC values
        file_name = datasets[i].replace(".csv", f"{sufix}_roc_dict.json")
        file_dict = open(file_dir + 'roc/' + file_name, "r")
        roc_vals = json.load(file_dict)
        file_dict.close()
        roc_vals = {int(k): v for k, v in roc_vals.items()}
        #PRC values
        file_name = datasets[i].replace(".csv", f"{sufix}_prc_dict.json")
        file_dict = open(file_dir + 'prc/' + file_name, "r")
        prc_vals = json.load(file_dict)
        file_dict.close()
        prc_vals = {int(k): v for k, v in prc_vals.items()}
        #wrong preds
        file_name = datasets[i].replace(".csv", f"{sufix}_wrongpred_dict.json")
        file_dict = open(file_dir + 'wrongpred/' + file_name, "r")
        wrong_preds = json.load(file_dict)
        file_dict.close()
        wrong_preds = {int(k): v for k, v in wrong_preds.items()}


    # plotting
    for z in range(0, len(metric)):
        #Classifiers
        temp_res = results[z]
        ind = np.arange(len(classifs))
        width = 0.15
        s = {}

        for b, m in enumerate(to_measure[i]):
            temp_v = [temp_res[a][m] for a in list(temp_res.keys())]
            smean = [t[0] for t in temp_v]
            sstd = [t[1] for t in temp_v]
            bars = axes[i].bar(ind + width * b, smean, width, bottom=0, label=m, capsize=10, yerr=sstd, alpha=alpha)
            axes[i].axhline(y = smean[0], linewidth=1, linestyle="--", color = bars.patches[0].get_facecolor())

        metric_name = str(metric[z]).split("<")[-1].split(" at")[0]
        #axes[i].set_title('Optimized {} in {}'.format(metric_name, target))
        axes[i].set_xticks(ind + ( width * len(to_measure[0])/2 ) - width/2)
        names = list(temp_res.keys())
        names = [names_replace[n] for n in names]
        axes[i].set_xticklabels(names, fontsize="x-large")
        axes[i].set_yticks(np.linspace(0, 1, 11))
        axes[i].set_ylim(0, 1)
        axes[i].legend(fontsize="x-large", loc="best")
        axes[i].autoscale_view()


        # ROC curve
        if nclasses == 2:
            # for prettier ROC curve
            temp_base_fpr = np.insert(base_fpr, 0, 0)
            temp_base_fpr[1] = 0.01
            lines = []
            # colors = ["r", "b", "g", "darkorange", "grey","magenta"]
            names = list(roc_vals[z].keys())
            names.remove("Dummy")       #dont draw Dummy line
            names.remove("KNeighbors")    # dont draw KNN line
            for name in names:
                tprs = np.array(roc_vals[z][name])
                mean_tprs = tprs.mean(axis=0)
                mean_tprs = np.insert(mean_tprs, 0, 0)
                axes_roc[i].plot(temp_base_fpr, mean_tprs, label=names_replace[name])
            axes_roc[i].plot([0, 1], [0, 1], 'r--')
            axes_roc[i].set_xlim([-0.01, 1.01])
            axes_roc[i].set_ylim([-0.01, 1.01])
            axes_roc[i].set_ylabel('True Positive Rate')
            axes_roc[i].set_xlabel('False Positive Rate')
            axes_roc[i].legend(loc="lower right", fontsize="large")
            roc_path = "./images/roc/" + datasets[i].replace(".csv", "_roc{}.png".format(z))

        # PRC curve
        if nclasses == 2:
            lines = []
            # colors = ["r", "b", "g", "darkorange", "grey","magenta"]
            names = list(prc_vals[z].keys())
            names.remove("Dummy")  # dont draw Dummy line
            names.remove("KNeighbors")    # dont draw KNN line
            for name in names:
                tprs = np.array(prc_vals[z][name])
                mean_tprs = tprs.mean(axis=0)
                axes_prc[i].plot(base_fpr, mean_tprs, label=names_replace[name])
            axes_prc[i].plot([1, 0], [0, 1], 'r--')
            axes_prc[i].set_xlim([-0.01, 1.01])
            axes_prc[i].set_ylim([-0.01, 1.01])
            axes_prc[i].set_ylabel('Precision')
            axes_prc[i].set_xlabel('Recall')
            axes_prc[i].legend(loc="lower left", fontsize="medium")
            prc_path = "./images/prc/" + datasets[i].replace(".csv", "_prc{}.png".format(z))

        # Wrong predictions
        wp = wrong_preds[z][best_pred[i]]
        df = whole_data[["gender", "stage", "lmr>2.1", "age"] + [target]]
        df["gender"] = df["gender"].replace({0: "F", 1: "M"})
        df["stage"] = df["stage"].replace({1: "I A", 2:"I B", 3:"II A", 4:"II B", 5:"III A", 6:"III B", 7:"IV A", 8:"IV B"})
        df["lmr>2.1"] = df["lmr>2.1"].replace({0: "False", 1: "True"})


        df["type"] = "NA"
        for row in df.iterrows():
            uid = row[0] in wp
            t = row[1][target] == 1
            if uid and t:
                val = "FN"
            elif uid and not t:
                val = "FP"
            elif not uid and t:
                val = "TP"
            elif not uid and not t:
                val = "TN"
            df.loc[row[0], "type"] = val

        hue_order = ["TN", "FN", "TP", "FP"]
        #sns.countplot(x="gender", hue="type", data=df, ax=axes_wp[i*2][0]).set_title("Gender")
        x, y = 'gender', 'type'
        df1 = df.groupby([x])[y].value_counts(normalize=True).mul(100).rename('Percentage').reset_index()
        sns.barplot(x=x, y="Percentage", hue=y, data=df1, hue_order=hue_order, ax=axes_wp[i*2][0], alpha=alpha).set_title("Gender")
        axes_wp[i*2][0].set_ylim(0, 100)
        axes_wp[i * 2][0].set_xlabel("")

        #sns.countplot(x="lmr>2.1", hue="type", data=df, ax=axes_wp[i*2 + 1][0]).set_title("LMR>2.1")
        x = "lmr>2.1"
        df1 = df.groupby([x])[y].value_counts(normalize=True).mul(100).rename('Percentage').reset_index()
        sns.barplot(x=x, y="Percentage", hue=y, data=df1, hue_order=hue_order, ax=axes_wp[i * 2][1], alpha=alpha).set_title("LMR>2.1")
        axes_wp[i * 2][1].set_ylim(0, 100)
        axes_wp[i * 2][1].set_xlabel("")

        # sns.countplot(x="stage", hue="type", data=df, ax=axes_wp[i*2][1]).set_title("Stage")
        x = "stage"
        df1 = df.groupby([x])[y].value_counts(normalize=True).mul(100).rename('Percentage').reset_index()
        df1 = df1[(df1["stage"] != "I A") & (df1["stage"] != "III B")]  # remove IA and IIIB due to low number of cases
        sns.barplot(x=x, y="Percentage", hue=y, data=df1, hue_order=hue_order, ax=axes_wp[i * 2 + 1][0], alpha=alpha).set_title("Stage")
        axes_wp[i * 2 + 1][0].set_ylim(0, 100)
        axes_wp[i * 2 + 1][0].set_xlabel("")

        bins = [10, 20, 30, 40, 50, 60, 70, 80]
        sns.histplot(x="age",hue="type", data=df, ax=axes_wp[i*2 + 1][1], hue_order=hue_order, bins=bins,
                     common_norm=False, stat="probability", multiple="stack").set_title("Age")
        axes_wp[i * 2 + 1][1].set_xlabel("")


        axes_wp[i*2][0].legend(fontsize="medium")
        axes_wp[i*2][1].legend(fontsize="medium")
        axes_wp[i*2 + 1][0].legend(fontsize="medium")
        axes_wp[i*2 + 1][1].legend_.set_title(None)


img_path = "./images/classifiers/final_" + datasets[i].replace(".csv", f"{sufix}_results.png")
#fig.show()
if save_figs: fig.savefig(img_path, format='png', dpi=dpi)

if nclasses == 2:
    roc_path = "./images/roc/final_" + datasets[i].replace(".csv", f"{sufix}_roc.png")
    #fig_roc.show()
    if save_figs: fig_roc.savefig(roc_path, format='png', dpi=dpi)

    prc_path = "./images/prc/final_" + datasets[i].replace(".csv", f"{sufix}_prc.png")
    fig_prc.show()
    if save_figs: fig_prc.savefig(prc_path, format='png', dpi=dpi)

wp_path = "./images/wrong_predictions/final_" + datasets[i].replace(".csv", f"{sufix}_wp.png")
#fig_wp.show()
if save_figs: fig_wp.savefig(wp_path, format='png', dpi=dpi)





