#!/usr/bin/env python
# coding: utf-8
import statistics

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.metrics import accuracy_score, precision_score, recall_score, confusion_matrix
import random
from . import datapreprocessing as datapp
#from imblearn.over_sampling import SMOTE
from sklearn import metrics
from sklearn.model_selection import StratifiedKFold, train_test_split
#from proj import datapreprocessing as datapp
#from mlxtend.frequent_patterns import apriori, association_rules #for ARM

rs = 1


def general_eval(df, to_clf, col_describe=()):
    data = df.copy()
    columns = data.columns
    data_types = data.dtypes.value_counts()
    col_nulls = data.columns[data.isna().any()].tolist()
    counts = data[to_clf].dropna().value_counts(normalize=True)
    print("\n Total of columns: ", len(columns))
    print("\n Total of rows: ", data.shape[0])
    print("\n Data types:\n", data_types)
    print("\n Columns with null values:", col_nulls)
    print("\n Percentage of classes:\n", counts)
    if len(col_describe) > 0:
        print("Full describe:\n", data[col_describe].describe())


def outliers_index(df, columns, ratio=1.5, by="column"):
    data = df.copy()
    q1 = data[columns].quantile(q=0.25)
    q3 = data[columns].quantile(q=0.75)
    iqr = q3-q1
    lower = q1 - (ratio*iqr)
    upper = q3 + (ratio*iqr)
    if by=="column":
        a = ((data[columns]<lower) | (data[columns]>upper)) & ~data[columns].isna()
        return a

def outliers_category(df, columns, ratio=1.5, by="column"):
    data = df.copy()
    q1 = data[columns].quantile(q=0.25)
    q3 = data[columns].quantile(q=0.75)
    iqr = q3-q1
    lower = q1 - (ratio*iqr)
    upper = q3 + (ratio*iqr)
    if by=="column":
        out_ind = data.index[ ((data[columns]<lower) | (data[columns]>upper))]
        return out_ind
    elif by == "row":
        nout = []
        for index, row in data.iterrows():
            #r_upper = upper - row
            serie1 = row[row.gt(upper)]
            serie2 = row[row.lt(lower)]
            nout.append(len(serie1) + len(serie2))
        return nout
    else:
        raise ValueError("Argument by must be 'row' or 'column'")


def sensitivity(tstY, prdY, labels):
    cfm = metrics.confusion_matrix(tstY, prdY, labels)
    return cfm[0,0]/(cfm[0,0]+cfm[0,1])

def specificity(y, prdY, labels=(1,0)):
    cfm = confusion_matrix(y, prdY, labels=labels)
    return cfm[1,1]/(cfm[1,1]+cfm[1,0])

def weighted_rec_prec(y, prdY, rec_weight=0.7):
    prec = precision_score(y, prdY)
    rec = recall_score(y, prdY)
    return (rec_weight * rec) + ( (1-rec_weight) * prec )

def autolabel(ax, rects, labels, threshold=10, percentage=True, orient="v"):
    """Attach a text label above each bar in *rects*, displaying its height."""
    symbol = "%" if percentage else ''
    if orient == "v":
        for rect, label in zip(rects,labels):
            if label > threshold:
                height = rect.get_height()
                if height >= 0.95: height -=0.06
                ax.annotate('{}{}'.format(label,symbol),
                            xy=(rect.get_x() + rect.get_width() / 2, height),
                            xytext=(0, 3),  # 3 points vertical offset
                            textcoords="offset points",
                            ha='center', va='bottom', size=12)
    elif orient == "h":
        for rect, label in zip(rects, labels):
            if label > threshold:
                width = rect.get_width()
                #if width >= 0.95: width -= 0.06
                ax.annotate('{}{}'.format(label, symbol),
                            xy=(width, rect.get_y() + rect.get_height()/2),
                            xytext=(15, -2),  # 3 points vertical offset
                            textcoords="offset points",
                            ha='center', va='bottom', size=12)

def test_pretty(ttrain, ttest, target, estimators, metrics, bal=None, norm=None, to_norm=[], title ="Title"):
    nclasses = len(ttrain[target].value_counts())
    train = ttrain.copy()
    test = ttest.copy()
    if bal == "sub":
        train = datapp.subsample(train, target)
    elif bal == "over":
        train = datapp.oversample(train, target, nclasses-1)
    elif bal == "smote":
        train = datapp.smotesample(train, target)
    if norm == "mean":
        mean, std = train[to_norm].mean(), train[to_norm].std()
        train[to_norm] = (train[to_norm] - mean) / std
        test[to_norm] = (test[to_norm] - mean) / std
    elif norm == "minmax":
        minv, maxv = train[to_norm].min(), train[to_norm].max()
        train[to_norm] = (train[to_norm] - minv) / (maxv - minv)
        test[to_norm] = (test[to_norm] - minv) / (maxv - minv)
    y_train = train[target].values
    X_train = train.drop(target, axis=1).values
    y_test = test[target].values
    X_test = test.drop(target, axis=1).values
    ind = np.arange(len(estimators))
    s = {}
    names = []
    for clf in estimators:
        clf.fit(X_train, y_train)
        pred = clf.predict(X_test)
        name = str(clf).split('(')[0].replace('Classifier', '')
        names.append(name)
        out = name + ":"
        empty = 15 - len(out)
        out += " "*empty
        for m in metrics:
            f = metrics[m]
            value = f(y_test, pred)
            try:
                s[m] += [value]
            except KeyError:
                s[m] = [value]
            out += "\t{}: {:.5f}".format(m, value)
        out+="\n"+"-"*50
        print(out)
    print("\n\n\n")
    plt.style.use('ggplot')
    fig, ax = plt.subplots(figsize=(6, 6))
    fig.tight_layout()
    width = 0.20
    colors = ["#1B486B", "#B2DB34", "#FC7634"]
    colors = ["#403129", "#CC323F", "#F2CB54"]
    colors = ["#008099", "#FAF6ED", "#8AB312"]
    print("S:",s)
    for i, m in enumerate(metrics):
        rects = ax.bar(ind + width * i, s[m], width, bottom=0, label=m, capsize=10)
        v = [round(i * 100, 2) for i in s[m]]
        autolabel(ax, rects, v, threshold=0)
    ax.set_title(title, size= 15)
    ax.set_xticks(ind + width)
    ax.set_xticklabels(names, size=13)
    ax.set_yticks(np.linspace(0, 1, 6))
    ax.set_ylim(0, 1)
    ax.legend(fancybox = True, shadow = True, loc='center')
    ax.autoscale_view()
    path = "../images/" + title.replace(' ', '')+'.png'
    plt.tight_layout()
    fig.savefig(path, format='png', dpi=1200)
    plt.show()

def test(ttrain, ttest, target, estimators, metrics, bal=None, norm=None, to_norm=[]):
    nclasses = len(ttrain[target].value_counts())
    train = ttrain.copy()
    test = ttest.copy()
    if bal == "sub":
        train = datapp.subsample(train, target)
    elif bal == "over":
        train = datapp.oversample(train, target, nclasses-1)
    elif bal == "smote":
        train = datapp.smotesample(train, target)
    if norm == "mean":
        mean, std = train[to_norm].mean(), train[to_norm].std()
        train[to_norm] = (train[to_norm] - mean) / std
        test[to_norm] = (test[to_norm] - mean) / std
    elif norm == "minmax":
        minv, maxv = train[to_norm].min(), train[to_norm].max()
        train[to_norm] = (train[to_norm] - minv) / (maxv - minv)
        test[to_norm] = (test[to_norm] - minv) / (maxv - minv)
    y_train = train[target].values
    X_train = train.drop(target, axis=1).values
    y_test = test[target].values
    X_test = test.drop(target, axis=1).values
    plt.style.use('ggplot')
    fig, ax = plt.subplots(figsize=(8, 8))
    ind = np.arange(len(estimators))
    width = 0.15
    s = {}
    names = []
    for clf in estimators:
        clf.fit(X_train, y_train)
        pred = clf.predict(X_test)
        name = str(clf).split('(')[0].replace('Classifier', '').replace('Regression', '')
        names.append(name)
        out = name + ":"
        empty = 15 - len(out)
        out += " "*empty
        for m in metrics:
            f = metrics[m]
            value = f(y_test, pred)
            try:
                s[m] += [value]
            except KeyError:
                s[m] = [value]
            out += "\t{}: {:.5f}".format(m, value)
        out+="\n"+"-"*50
        print(out)
    print("\n\n\n")
    for i, m in enumerate(metrics):
        ax.bar(ind + width * i, s[m], width, bottom=0, label=m, capsize=10)
    ax.set_title('Scores by classfier')
    ax.set_xticks(ind + width)
    ax.set_xticklabels(names)
    ax.set_yticks(np.linspace(0, 1, 11))
    ax.set_ylim(0, 1)
    ax.legend()
    ax.autoscale_view()
    plt.show()

def cross_val(df,target ,estimators, metrics, bal=None, norm=None, to_norm=[], k=5, title="", bottom=0., 
              verbose=2, ax=None, line=None, rs=42):
    columns = df.columns        #assume que target esta no fim
    types = df.dtypes
    nclasses = len(df[target].value_counts())
    y = df[target].values
    X = df.drop([target], axis=1).values
    if to_norm == [-1]: to_norm = df.drop(target, axis=1).columns
    res = {}
    names = []
    s = {}
    for est in estimators:
        skf = StratifiedKFold(n_splits=k, shuffle=True, random_state=rs)
        for train_index, test_index in skf.split(X, y):
            X_train, X_test = X[train_index], X[test_index]
            y_train, y_test = y[train_index], y[test_index]
            train_df = pd.DataFrame(np.append(X_train, y_train.reshape(-1, 1), axis=1), columns=columns).astype(types)
            test_df = pd.DataFrame(np.append(X_test, y_test.reshape(-1, 1), axis=1), columns=columns).astype(types)
            if bal != None:
                #balancing
                train_df = datapp.imblearn_balancing(train_df, target, bal, rs=rs)
            if norm != None:
                if norm == "mean":
                    mean, std = train_df[to_norm].mean(), train_df[to_norm].std()
                    train_df[to_norm] = (train_df[to_norm] - mean) / std
                    test_df[to_norm] = (test_df[to_norm] - mean) / std
                elif norm == "minmax":
                    minv, maxv = train_df[to_norm].min(), train_df[to_norm].max()
                    train_df[to_norm] = (train_df[to_norm] - minv) / (maxv - minv)
                    test_df[to_norm] = (test_df[to_norm] - minv) / (maxv - minv)
            y_train = train_df[target].values
            X_train = train_df.drop([target], axis=1).values
            y_test = test_df[target].values
            X_test = test_df.drop([target], axis=1).values
            est.fit(X_train, y_train)
            pred = est.predict(X_test)
            for m in metrics:
                f = metrics[m]
                try:
                    s[m] += [f(y_test, pred)]
                except KeyError:
                    s[m] = [f(y_test, pred)]
        name = str(est).split('(')[0].replace('Classifier', '').replace('Regression', '')
        if name in res.keys(): name += 'c'
        names.append(name)
        out = ""
        res[name] = {}
        for m in metrics:
            meanv = round(statistics.mean(s[m]), 5)
            std = round(statistics.stdev(s[m]), 5)
            res[name][m] = (meanv, std)
            a = m + ": " + str(meanv) + '+-' + str(std) + '\t'
            out+=a
        #if verbose >= 1: print(name[:12], ":\t",out)

    #-------------------------------------------
    if verbose >= 2:
        means = [ [] for _ in metrics ]
        std = [ [] for _ in metrics ]
        for n in names:
            for i,m in enumerate(metrics):
                v = res[n][m]
                means[i].append(v[0])
                std[i].append(v[1])

        plt.style.use('ggplot')
        if ax == None: fig, ax = plt.subplots(figsize=(12, 12))
        ind = np.arange(len(estimators))
        width = 0.15
        for i,m in enumerate(metrics):
            ax.bar(ind + width*i, means[i], width, bottom=0, yerr=std[i],label=m, capsize=10)
        ax.set_title(title)
        ax.set_xticks(ind + width)
        ax.set_xticklabels(names)
        ax.set_ylim(bottom,1)
        divs = int(((1-bottom)*20 )+1)
        ax.set_yticks(np.linspace(bottom,1,divs))
        if line != None:  ax.axhline(y=line, linewidth=1, color='r', linestyle="--")
        ax.legend()
        ax.autoscale_view()
    return res

def train_predict_kfold(df, to_clf, classifier, k=2, bal=None, std=False,random_state=42):
    data = df.copy()
    columns = data.columns
    y: np.ndarray = data[to_clf].values
    data_X = data.drop(to_clf, axis=1)
    id_index = data_X.columns.get_loc("id")
    X: np.ndarray = data_X.values
    labels = pd.unique(y)

    meaned = datapp.mean_df(data, "id")
    yid: np.ndarray = meaned[to_clf].values.reshape((-1, 1))
    Xid: np.ndarray = meaned.drop(to_clf, axis=1).values

    accuracys = []
    sensitivities = []
    cfms = []
    #max_id = data["id"].max()
    skf = StratifiedKFold(n_splits=k)
    for train_index, test_index in skf.split(Xid, yid):
        func_train = np.vectorize(lambda t: t in train_index)
        func_test = np.vectorize(lambda t: t in test_index)
        ind_train, ind_test = func_train(X[:, id_index]), func_test(X[:, id_index])
        X_train, X_test = X[ind_train], X[ind_test]
        y_train, y_test = y[ind_train], y[ind_test]

        if bal =="smote":
            smote = SMOTE(ratio='minority', random_state=random_state)
            X_train, y_train = smote.fit_sample(X_train, y_train)
        elif bal == "oversample":
            np_train = np.concatenate((X_train, y_train.reshape((-1,1))), axis=1)
            df_train = pd.DataFrame(np_train, columns=columns)
            df_over = datapp.oversample(df_train, to_clf)
            y_train: np.ndarray = df_over[to_clf].values
            X_train: np.ndarray = df_over.drop(to_clf, axis=1).values

        classifier.fit(X_train, y_train)
        prdY = classifier.predict(X_test)
        accuracy = metrics.accuracy_score(y_test, prdY)
        sens = sensitivity(y_test, prdY, labels)
        cfm = metrics.confusion_matrix(y_test, prdY, labels)
        accuracys.append(accuracy)
        sensitivities.append(sens)
        cfms.append(cfm.astype("int"))
    acc = round(sum(accuracys)/len(accuracys), 3)
    sen = round(sum(sensitivities)/len(sensitivities), 3)
    cfm = sum(cfms)

    if std:
        stdacc = round(statistics.stdev(accuracys), 3)
        stdsen = round(statistics.stdev(sensitivities), 3)
        return (acc, sen, cfm, stdacc, stdsen)
    return (acc, sen, cfm)


def measures_cluster(pred, true, labels):
    pred2 = [0 if i==1 else 1 for i in pred]

    acc1 = metrics.accuracy_score(true, pred)
    acc2 = metrics.accuracy_score(true, pred2)
    acc = max(acc1, acc2)

    sens1 = sensitivity(true, pred, labels)
    sens2 = sensitivity(true, pred2, labels)

    sens = max(sens1, sens2)

    return (acc, sens)





def cut(df, bins, ig_classes, cut="cut"):
    labels = list(map(str,range(1,bins+1)))
    dfc = df.copy()
    if cut == "cut":
        for col in dfc:
            if col not in ig_classes:
                dfc[col] = pd.cut(dfc[col],bins,labels=labels)
    elif cut == "qcut":
         for col in dfc:
            if col not in ig_classes:
                dfc[col] = pd.qcut(dfc[col],bins,labels=labels)
    return dfc

def dummy(df,ig_classes):
    dfc = df.copy()
    dummylist = []
    for att in dfc:
        if att in ig_classes: dfc[att] = dfc[att].astype('category')
        dummylist.append(pd.get_dummies(dfc[[att]]))
    dummified_df = pd.concat(dummylist, axis=1)
    return dummified_df


def freq_itemsets(df, minpaterns=30):
    dfc = df.copy()
    frequent_itemsets = {}
    minsup = 1.0
    while minsup>0:
        minsup = minsup*0.9
        frequent_itemsets = apriori(dfc, min_support=minsup, use_colnames=True)
        if len(frequent_itemsets) >= minpaterns:
            print("\nMinimum support:",minsup)
            break
    print("Number of found patterns:",len(frequent_itemsets))
    return frequent_itemsets


def assoc_rules(fi, orderby="lift", inverse=False, min_confidence=0.7):
    rules = association_rules(fi, metric="confidence", min_threshold=min_confidence)
    rules["antecedent_len"] = rules["antecedents"].apply(lambda x: len(x))
    rules = rules[(rules['antecedent_len'] >= 2) & (rules['confidence'] > min_confidence)]
    if orderby != None:
        rules = rules.sort_values(by=[orderby], ascending=inverse)
    return rules


def conf_matrix():
    cfm = metrics.confusion_matrix(d[3], prdY, d[4])

#############################################################################################################

def get_train_and_test(ttrain, ttest, target, bal=None, norm=None, to_norm=[], id=False):
    nclasses = len(ttrain[target].value_counts())
    train = ttrain.copy()
    test = ttest.copy()
    if bal == "sub":
        train = datapp.subsample(train, target)
    elif bal == "over":
        train = datapp.oversample(train, target, nclasses-1)
    elif bal == "smote":
        train = datapp.smotesample(train, target)
    if norm == "mean":
        mean, std = train[to_norm].mean(), train[to_norm].std()
        train[to_norm] = (train[to_norm] - mean) / std
        test[to_norm] = (test[to_norm] - mean) / std
    elif norm == "minmax":
        minv, maxv = train[to_norm].min(), train[to_norm].max()
        train[to_norm] = (train[to_norm] - minv) / (maxv - minv)
        test[to_norm] = (test[to_norm] - minv) / (maxv - minv)
    y_train = train[target].values
    X_train = train.drop(target, axis=1).values
    y_test = test[target].values
    X_test = test.drop(target, axis=1).values

    if id:
        ids = test.index
        return X_train, y_train, X_test, y_test, ids
    else:
        return X_train, y_train, X_test, y_test


#igual a de cima mas só recebe um dataset
def transform_data(ttrain, target, bal=None, norm=None, to_norm=[]):
    nclasses = len(ttrain[target].value_counts())
    train = ttrain.copy()
    if bal == "sub":
        train = datapp.subsample(train, target)
    elif bal == "over":
        train = datapp.oversample(train, target, nclasses-1)
    elif bal == "smote":
        train = datapp.smotesample(train, target)
    if norm == "mean":
        mean, std = train[to_norm].mean(), train[to_norm].std()
        train[to_norm] = (train[to_norm] - mean) / std
    elif norm == "minmax":
        minv, maxv = train[to_norm].min(), train[to_norm].max()
        train[to_norm] = (train[to_norm] - minv) / (maxv - minv)
    y_train = train[target].values
    X_train = train.drop(target, axis=1).values

    return X_train, y_train

