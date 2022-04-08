import pickle
import numpy as np
from IPython.core.display import display
from sklearn import inspection, model_selection, ensemble, feature_selection
import matplotlib.pyplot as plt
import pandas as pd
import tqdm

np.random.seed(42)

def feat_select_miwi(threshold=0.05, prev_sel=None):
    file_mi = open("pvalues.pkl", "rb")
    file_wi = open("pvalues_wil.pkl", "rb")
    pvalues_mi = pickle.load(file_mi)
    pvalues_wi = pickle.load(file_wi)

    pvalues_mi = { k: v for k, v in pvalues_mi.items() if v <= threshold }
    print("Features mi with p-value <= {}: {}".format(threshold,len(pvalues_mi)))

    pvalues_wi = { k: v for k, v in pvalues_wi.items() if v <= threshold }
    print("Features wi with p-value <= {}: {}\n".format(threshold,len(pvalues_wi)))
    union = set(pvalues_mi)
    union.update(pvalues_wi)
    print("Union of features from MI and Wilcoxon has len: {} ".format(len(union)))

    inter = list( set(pvalues_mi) & set(pvalues_wi) )
    l_inter = len(inter)
    print("Intersection of features from MI and Wilcoxon has len: {} ".format(len(inter)))
    if len(inter)<=15:
        print("Intersection of both methods: {}\n".format(inter))

    if prev_sel!=None:
        sel_union = list(union & set(prev_sel))
        print("\nPreviously selected in union: {}".format(sel_union))
        sel_inter = list(set(inter) & set(prev_sel))
        print("Previously selected in intersection: {}".format(sel_inter))
    return list(union), inter, pvalues_mi, pvalues_wi


def model_rfe(model, df, target, path=None ,cv=5, scoring="f1", n_features = -1, verbose=0):
    y = df[target]
    columns = df.drop(columns=[target]).columns
    X = df.drop(columns=[target]).values
    
    if n_features<=0:
    ###n_features#########################################
        rfecv = feature_selection.RFECV(estimator=model, step=1, cv=cv, scoring=scoring, min_features_to_select=1)
        rfecv.fit(X, y)
        n_features = rfecv.n_features_
        if verbose >= 2:
        ###graphics#########################################
            plt.figure(figsize=(12,6))
            plt.xlabel("Number of features selected")
            plt.ylabel("Cross validation score (nb of correct classifications)")
            plt.plot(range(1, len(rfecv.grid_scores_) + 1), rfecv.grid_scores_)
            plt.show()
        if verbose >= 1: print("Number of features: {}".format(n_features))
            
    ####calculate_ranking#################################
    rfe = feature_selection.RFE(estimator=model, n_features_to_select=1, step=1)
    rfe.fit(X, y)
    ranking = rfe.ranking_
    svm_rfe = pd.DataFrame()
    svm_rfe["var"] = columns
    svm_rfe["imp"] = ranking
    svm_rfe = svm_rfe.sort_values(by="imp")
    selected = svm_rfe.head(n_features)["var"].tolist()
    #if verbose >= 2: display(svm_rfe)
    if verbose >= 1: print(selected)
    if path != None: svm_rfe.to_csv(path, index=False)
    return svm_rfe, selected

    
def rf_feature_selection(df, target, path_imp, path_sets, pre_importances=False, u=1, cv=5, scoring="f1", 
                         n_repeats=30, verbose=1):
    rs = 1
    y = df[target]
    columns = df.drop(columns=[target]).columns
    X = df.drop(columns=[target]).values
    
    ###importances############################################3
    if not pre_importances:
        skf = model_selection.StratifiedKFold(n_splits=cv, shuffle=True, random_state = rs)
        results = []
        for train_index, test_index in tqdm.tqdm(skf.split(X, y), total=cv):
            X_train, X_test = X[train_index], X[test_index]
            y_train, y_test = y[train_index], y[test_index]
            clf = ensemble.RandomForestClassifier(random_state=rs).fit(X_train, y_train)
            r = inspection.permutation_importance(clf, X_test, y_test, scoring=scoring, n_repeats=n_repeats, random_state=rs)
            results.append(r.importances_mean)

        final = sum(results)/len(results)
        rf_based = pd.DataFrame()
        rf_based["var"] = columns
        rf_based["imp"] = final
        rf_based.to_csv(path_imp, index=False)
        if verbose >= 1:
            print("non-zero variables: {} of {} - {:.2f}%".format(np.count_nonzero(final>0), len(columns), np.count_nonzero(final>0)/len(final)*100))    
    ###out-of-bag error sets######################################
    d = pd.read_csv(path_imp)
    d = d[d["imp"]>0].sort_values("imp")
    oobs = pd.DataFrame(columns=["set", "size", "oob"])
    for i in tqdm.tqdm(range(d.shape[0])):
        cols = d["var"]
        y = df[target]
        X = df[cols].values
        rf = ensemble.RandomForestClassifier(oob_score=True, random_state=rs).fit(X,y)
        oobs = oobs.append({"set":cols.tolist(),"size": len(cols) ,"oob": 1 - rf.oob_score_}, ignore_index=True)
        d = d[1:]
    ###choose set according to u#################################
    print("with u = {}".format(u))
    min_oob = oobs["oob"].min()
    std = oobs["oob"].std()
    t = oobs[(oobs["oob"] <= min_oob+ std*u) & (oobs["oob"] >= min_oob- std*u)]
    min_oob = t["oob"].min()
    if verbose>=2:
        display(t[ t["oob"] == min_oob])

    rf_select = t[ t["oob"] == min_oob]["set"].tolist()[0]
    return rf_select

    
def logfc_select(data, target, threshold=None, log=False, constant=0.001):
    #fold change of means!
    neg = data[target] == 0
    pos = data[target] == 1
    data = data.drop(columns=[target]) + constant
    
    if log:
        means_neg = (np.log2(data[neg])).mean()
        means_pos = (np.log2(data[pos])).mean()
    else:
        means_neg = data[neg].mean()
        means_pos = data[pos].mean()

    proportion = means_pos - means_neg
    if threshold == None: 
        return proportion
    threshold = np.log2(threshold)
    fc_genes = proportion[(proportion >= threshold) | (proportion <= (-threshold))]
    fc_genes = fc_genes.sort_values()
    return fc_genes

def permutation_test(x, y, corr_val, func, args={}, side="two", list_ind=None, rounds=1000, rs=42):
    np.random.seed(rs)
    rv = np.empty(rounds)
    for i in range(0,rounds):
        x = np.random.permutation(x)
        corr = func(x, y, **args)
        if list_ind!=None:
            corr = corr[list_ind]
        rv[i] = corr
    
    if side=="right":
        p_val = len(np.where(rv>=corr_val)[0])/rounds #one-sided right
    elif side=="two":
        mean, std = rv.mean(), rv.std()
        rv = (rv-mean)/std
        corr_val = (corr_val-mean)/std
        p_val = len(np.where(abs(rv) >= abs(corr_val))[0]) / rounds
    return p_val, rv

def save_gene_set(genes, file_name):
    f = open(file_name, "w")
    for g in genes:
        f.write(g+'\n')
    f.close()