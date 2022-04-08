#!/usr/bin/env python
# coding: utf-8
import warnings

import numpy as np
import pandas as pd
from sklearn.decomposition import PCA
from sklearn.feature_selection import SelectKBest, f_classif
from sklearn.model_selection import train_test_split
from sklearn import preprocessing
from imblearn.over_sampling import SMOTE, RandomOverSampler, KMeansSMOTE
from imblearn.combine import SMOTETomek, SMOTEENN
from imblearn.under_sampling import TomekLinks
from . import evaluation as eval
import random

rs = 1


def get_correlations(corr_matrix, tr):
    #print("TIHREWHJREW: ", tr)
    upper = corr_matrix.where(np.triu(np.ones(corr_matrix.shape), k=1).astype(np.bool))
    upper = np.absolute(upper)
    #print("upper: \n", upper)
    counter = 0
    conjuntos = []
    for index, row in upper.iterrows():
        if counter == 0:
            #print("\n\nrow:\n",row)
            #print("\nrow ge:\n", row.ge(tr))
            serie = row[row.ge(tr)]
            #print("\n\nserie greater than:\n", serie)
            size = len(serie)
            if size > 0:
                counter = size
                correlated = serie.axes[0].tolist()
                #print("\n\ncorrelated:\n", correlated)
                correlated.append(row.name)
                conjuntos.append(correlated)
        else:
            counter -= 1
    return conjuntos


def reduce(data, columns, name):
    e_col = []
    for c in columns:
        if c in data.columns:
            e_col.append(c)

    if len(e_col) > 3:
        pdf = data[e_col]

        mean = name + "Mean"
        std = name + "Std"
        median = name + "Median"

        data[mean] = pdf.mean(axis=1)
        data[std] = pdf.std(axis=1)
        data[median] = pdf.median(axis=1)
        data = data.drop(columns=list(pdf.columns))

    return data


def red_correlations(data, to_clf="class",tr=0.9, n=3):
    df = data.copy()
    df_class = df[to_clf]
    df = df.drop(to_clf, axis=1)
    corr_mtx = df.corr()
    correlations = get_correlations(corr_mtx, tr=tr)

    #print("correlations: \n", correlations)

    for c in correlations:
        if len(c) > n:
            df = reduce(df, c, c[0])
    df = df.join(df_class)
    return df


def split_dataset(df,to_clf, train_ratio=0.7, random_state=42):
    #fazer data split para dataset com train_test_split
    data = df.copy()
    y: np.ndarray = data[to_clf].values
    X: np.ndarray = data.drop(to_clf, axis=1).values
    labels = pd.unique(y)
    trnX1, tstX1, trnY1, tstY1 = train_test_split(X, y, train_size=train_ratio, stratify=y, random_state=random_state)

    return (trnX1, trnY1, tstX1, tstY1, labels)



def balance(df, to_clf, bal="smote", train_ratio=0.7, random_state=42):
    data = df.copy()
    if bal=="smote":
        columns = data.columns
        y: np.ndarray = data[to_clf].values
        X: np.ndarray = data.drop(to_clf, axis=1).values
        labels = pd.unique(y)
        trnX, tstX, trnY, tstY = train_test_split(X, y, train_size=train_ratio, stratify=y, random_state=random_state)

        smote = SMOTE(ratio='minority', random_state=random_state)
        smote_X, smote_y = smote.fit_sample(trnX, trnY)
        #smote_y = smote_y.reshape((-1, 1))
        #tstY = tstY.reshape((-1, 1))
        return (smote_X, smote_y, tstX, tstY, labels)

    elif bal=="undersample":
        target_count = data[to_clf].value_counts()
        min_class = target_count.idxmin()
        df_class_min = data[data[to_clf] == min_class]
        df_class_max = data[data[to_clf] != min_class]
        df_under = df_class_max.sample(len(df_class_min))
        df_under = pd.concat([df_under, df_class_min])
        result = split_dataset(df_under, to_clf, train_ratio=train_ratio, random_state=random_state)
        return result
    else:
        raise ValueError("Only smote balance e undersample is implemented. balance='smote' or 'undersample'")


def normalize(data, ignore,to_clf, norm="minmax"):
    flag=0
    df = data.copy()
    diff = ignore + [to_clf]
    df_nr = df[df.columns.difference(diff)]
    classdf = df[to_clf]
    try:
        df_ignored = df[ignore]
    except KeyError:
        flag=1
    x = df_nr.values  # returns a numpy array
    if norm == "minmax":
        normalizer = preprocessing.MinMaxScaler()
    elif norm == "standard":
        normalizer = preprocessing.StandardScaler()
    else:
        raise ValueError("Argument norm must be 'minmax' or 'standard'")
    x_scaled = normalizer.fit_transform(x)
    data_norm = pd.DataFrame(x_scaled, columns=df_nr.columns)
    if flag==0:
        data_norm = data_norm.join(df_ignored)
    data_norm = data_norm.join(classdf)
    return data_norm


def mean_df(data, by):
    df_mean = pd.DataFrame(columns=data.columns)
    max = int(data[by].max())
    for i in range(max+1):
        group = data.loc[(data[by] == i)]
        mean_group = group.mean()
        df_mean = df_mean.append(mean_group, ignore_index=True)
    return df_mean


def feature_reduction(df,to_clf ,to_save, n_features, as_int=False,alg="PCA"):
    data = df.copy()


    y: np.ndarray = data[to_clf].values
    X: np.ndarray = data.drop(to_save, axis=1).values
    save:  np.ndarray = data[to_save].values

    if not as_int:
        n_features = int(n_features * (len(X[1])))
    maxc = min(X.shape[0], X.shape[1])
    n_features = min(n_features, maxc)

    if alg=="PCA":
        X = PCA(n_components=n_features, copy=True).fit_transform(X)
    elif alg=="selectkbest":
        n_features = min(n_features, X.shape[1])
        X = SelectKBest(f_classif, k=n_features).fit_transform(X, y)
    else:
        raise ValueError("alg must be 'PCA' or 'selectkbest'")
    data_array = np.concatenate((X,save), axis=1)
    columns = list(range(n_features))
    columns = list(map(str, columns))
    columns += to_save
    dataframe = pd.DataFrame(data_array, columns=columns)
    return dataframe



def preprocess(df, to_clf, red_corr=True, tr=0.90, n=3, normalization=None ,ignore_classes=[],
               mean_by=None,remove_outliers=None, train_ratio=0.7, as_df =False,random_state=42):
    data = df.copy()
    if red_corr:
        data = red_correlations(data, tr=tr, n=n)
    if remove_outliers != None:
        outliers_row = eval.outliers_category(data, data.columns, ratio=1.5, by="row")
        remove = [n for n, i in enumerate(outliers_row) if i > remove_outliers]
        data = data.drop(remove)
        #data = data.reset_index()
        print(data.head())
    if mean_by != None:
        data = mean_df(data, mean_by)
    if normalization != None:
        data = normalize(data, ignore_classes,to_clf, norm=normalization)
    #if bal != None:
        #data = balance(data, to_clf, bal,train_ratio, random_state)
    if as_df:
        return data
    else:
        data = split_dataset(data, to_clf, train_ratio, random_state)
    return data

def preprocess_alt(df, to_clf, red_corr=True, tr=0.90, n=3, normalization=None ,ignore_classes=[],
               mean_by=None,remove_outliers=None, train_ratio=0.7, as_df =False,random_state=42):
    data = df.copy()
    if mean_by != None:
        data = mean_df(data, mean_by)
    if normalization != None:
        data = normalize(data, ignore_classes,to_clf, norm=normalization)
    if red_corr:
        data = red_correlations(data, to_clf,tr=tr, n=n)
    if remove_outliers != None:
        outliers_row = eval.outliers_category(data, data.columns, ratio=1.5, by="row")
        remove = [n for n, i in enumerate(outliers_row) if i > remove_outliers]
        data = data.drop(remove)
        #data = data.reset_index()
        print(data.head())
    #if bal != None:
        #data = balance(data, to_clf, bal,train_ratio, random_state)
    if as_df:
        return data
    else:
        data = split_dataset(data, to_clf, train_ratio, random_state)
    return data



def imblearn_balancing(df, target,resample, rs = 42):
    y = df[target].values.reshape(-1,1)
    X = df.drop(target, axis=1).values
    X, y = resample.fit_resample(X, y)
    df = pd.DataFrame(np.append(X, y.reshape(-1,1), axis=1), columns=df.columns).astype(df.dtypes)
    return df.sample(frac=1, random_state=rs)




def tvt_split(df, to_clf, train_ratio=0.7, val_ratio=0.5, random_state = 32):
    data = df.copy()
    y: np.ndarray = data[to_clf].values.astype("int")
    X: np.ndarray = data.drop(to_clf, axis=1).values

    trnX, tstX, trnY, tstY = train_test_split(X, y, train_size=train_ratio, stratify=y, random_state=random_state)

    valX, tstX, valY, tstY = train_test_split(tstX, tstY, train_size=val_ratio, stratify=tstY, random_state=random_state)

    return trnX, tstX, valX, trnY, tstY, valY

def subsample_split(df, to_clf, categoric, norm ,train_ratio=0.7, val_ratio=0.15, random_state=32):
    data = df.copy()
    ignore = categoric + [to_clf]

    columns_no_ignore = data.columns.difference(ignore)
    columns = data.columns

    trnX, tstX, valX, trnY, tstY, valY = tvt_split(data, to_clf,train_ratio, val_ratio/(1-train_ratio))
    trn_size = trnX.shape[0]
    val_size = valX.shape[0]
    tst_size = tstX.shape[0]


    trn_np = np.concatenate((trnX, trnY.reshape((-1,1))), axis=1)
    val_np = np.concatenate((valX, valY.reshape((-1,1))), axis=1)
    tst_np = np.concatenate((tstX, tstY.reshape((-1,1))), axis=1)
    trn_np = np.concatenate((trn_np, val_np), axis=0)

    trn_df = pd.DataFrame(trn_np, columns=columns)
    tst_df = pd.DataFrame(tst_np, columns=columns)

    trn_df = subsample(trn_df, to_clf)
    tst_df = tst_df.iloc[:20000]

    if norm == "minmax":
        normalizer = preprocessing.MinMaxScaler()
    elif norm == "standard":
        normalizer = preprocessing.StandardScaler()
    else:
        normalizer = preprocessing.MinMaxScaler()

    trn_norm = trn_df[trn_df.columns.difference(ignore)]
    trn_ignore = trn_df[ignore]
    tst_norm = tst_df[tst_df.columns.difference(ignore)]
    tst_ignore = tst_df[ignore]

    X_trn : np.ndarray = trn_norm.values
    X_tst: np.ndarray = tst_norm.values

    normalizer.fit(X_trn)
    X_trn_norm = normalizer.transform(X_trn)
    X_tst_norm = normalizer.transform(X_tst)

    if normalizer == None:
        X_trn_norm = X_trn
        X_tst_norm = X_tst

    trn_norm_df = pd.DataFrame(X_trn_norm, columns=columns_no_ignore)
    tst_norm_df = pd.DataFrame(X_tst_norm, columns=columns_no_ignore)

    trn_norm_df = trn_norm_df.join(trn_ignore)
    tst_norm_df = tst_norm_df.join(tst_ignore)

    tstX = tst_norm_df.drop(to_clf, axis=1).values
    tstY = tst_norm_df[to_clf].values

    Xtrn = trn_norm_df.drop(to_clf, axis=1).values
    Ytrn = trn_norm_df[to_clf].values
    trnX, valX, trnY, valY = train_test_split(Xtrn, Ytrn, train_size=1-val_ratio*(val_ratio+train_ratio),
                                              stratify=Ytrn, random_state=random_state)

    return trnX, tstX, valX, trnY, tstY, valY



#-----------------------------------------------------------------------------

def train_test_df(data, target, test_ratio=0.33, random_state=42):
    target_col = data.pop(target)
    data.insert(len(data.columns), target, target_col)
    y = data[target].values.reshape(-1, 1)
    X = data.drop(target, axis=1).values
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=test_ratio, random_state=random_state)

    data_train = pd.DataFrame(np.append(X_train, y_train, axis=1), columns=data.columns).astype(data.dtypes)
    data_test = pd.DataFrame(np.append(X_test, y_test, axis=1), columns=data.columns).astype(data.dtypes)
    return data_train, data_test


def knn_impute(imputer, df):
    index = df.index
    imputer = imputer.fit(df)
    matrix = imputer.transform(df)
    data = pd.DataFrame(matrix, columns=df.columns,index=index).astype(df.dtypes)
    return data, imputer





