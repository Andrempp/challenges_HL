import math
import numpy as np
import pandas as pd

from sklearn.metrics import recall_score, f1_score, cohen_kappa_score

from . import evaluation as eval

import xgboost
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import SVC
from sklearn import svm, metrics
from sklearn import tree
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.neural_network import MLPClassifier
from xgboost import XGBClassifier
# from lightgbm import LGBMClassifier
import random
from sklearn.model_selection import cross_val_score, cross_validate

import pyspark

# !pip install hyperopt
from hyperopt import STATUS_OK, space_eval
from hyperopt import tpe
from hyperopt import Trials, SparkTrials
from hyperopt import fmin
from hyperopt import hp
from hyperopt.early_stop import no_progress_loss
from hyperopt.pyll import scope

import warnings

warnings.filterwarnings('ignore')

rs = 1

#####DEFINE SEARCH SPACES###############################################################################################
# TODO: implement overrinding of search space in optimize_model
#space_dummy = {'strategy': 'uniform'}

space_knn = {'n_neighbors': scope.int(hp.quniform('n_neighbors', 2, 40, 1)),
             'weights': hp.choice('weights', ['distance', 'uniform'])}

space_svm = {'C': hp.uniform('C', 1, 100),
             'shrinking': hp.choice('shrinking', [False, True]),
             'kernel': hp.choice('kernel', [
                 {'kernel': 'linear', },
                 {'kernel': 'rbf',
                  'gamma': hp.choice('rbf_gamma', ['scale', 'auto']), },
                 {'kernel': 'poly',
                  'gamma': hp.choice('poly_gamma', ['scale', 'auto']),
                  'coef0': scope.int(hp.quniform('poly_coef0', -20, 20, 1)), },
                 {'kernel': 'sigmoid',
                  'gamma': hp.choice('sigmoid_gamma', ['scale', 'auto']),
                  'coef0': scope.int(hp.quniform('sigmoid_coef0', -20, 20, 1)), }
             ])}

space_dt = {'criterion': hp.choice('criterion', ['gini', 'entropy']),
            'splitter': hp.choice('splitter', ['best', 'random']),
            'max_features': hp.choice('max_features', [None, "sqrt", "log2"]),
            'max_depth': scope.int(hp.quniform('max_depth', 1, 40, 1)),
            'min_samples_split': scope.int(hp.quniform('min_samples_split', 2, 20, 1)),
            'min_samples_leaf': scope.int(hp.quniform('min_samples_leaf', 1, 20, 1)),
            'ccp_alpha': hp.loguniform('ccp_alpha', np.log(0.0001), np.log(0.2))}

space_rf = {'n_estimators': scope.int(hp.quniform('n_estimators', 20, 200, 10)),
            'criterion': hp.choice('criterion', ['gini', 'entropy']),
            'max_depth': scope.int(hp.quniform('max_depth', 1, 40, 2)),
            'min_samples_split': scope.int(hp.quniform('min_samples_split', 2, 20, 1)),
            'min_samples_leaf': scope.int(hp.quniform('min_samples_leaf', 1, 20, 1)),
            'ccp_alpha': hp.loguniform('ccp_alpha', np.log(0.0001), np.log(0.2))}

space_xgb = {  # TODO conditional max_depth and gamma (in gbtree)
    'learning_rate': hp.loguniform('learning_rate', np.log(0.0001), np.log(0.9)),
    'booster': hp.choice('booster', ['gbtree', 'gbtree', 'dart']),
    'reg_alpha': hp.loguniform('reg_alpha', np.log(0.0001), np.log(0.1)),
    # 'gamma': hp.loguniform('gamma', np.log(0.0001), np.log(5.0)),
    'n_estimators': scope.int(hp.quniform('n_estimators', 20, 500, 10)),
    'eval_metric': hp.choice('eval_metric', ['logloss'])
}

space_lgbm = {'max_depth': scope.int(hp.quniform('max_depth', 1, 12, 1)),
              'learning_rate': hp.loguniform('learning_rate', np.log(0.0001), np.log(0.9)),
              'boosting_type': hp.choice('boosting_type', ['gbdt', 'goss']),
              'reg_alpha': hp.loguniform('reg_alpha', np.log(0.0001), np.log(0.1)),
              'n_estimators': scope.int(hp.quniform('n_estimators', 20, 400, 20))}

space_lr = {'fit_intercept': hp.choice('fit_intercept', [True, False]),
            'C': hp.uniform('C', 0.0, 2.0),
            'solver': hp.choice('solver',
                                [{'solver': 'liblinear',
                                  'penalty': hp.choice('penalty',
                                                       [{'penalty': 'l1'},
                                                        {'penalty': 'l2',
                                                         'dual': hp.choice('l2_dual', [True, False])}])},
                                 {'solver': 'newton-cg',
                                  'penalty': hp.choice('newton_penalty', ['l2', 'none'])},
                                 {'solver': 'sag',
                                  'penalty': hp.choice('sag_penalty', ['l2', 'none'])},
                                 {'solver': 'lbfgs',
                                  'penalty': hp.choice('lbfgs_penalty', ['l2', 'none'])},
                                 {'solver': 'saga',
                                  'penalty': hp.choice('saga_penalty',
                                                       [{'penalty': 'l2'},
                                                        {'penalty': 'none'},
                                                        {'penalty': 'elasticnet',
                                                         'l1_ratio': hp.uniform('l1_ratio', 0.0, 1.0)}])}])}

space_mlp = {'alpha': hp.loguniform('alpha', np.log(0.0001), np.log(0.2)),
             'learning_rate_init': hp.loguniform('learning_rate_init', np.log(0.001), np.log(0.2)),
             'batch_size': scope.int(hp.quniform('batch_size', 50, 300, 50)),
             'early_stopping': hp.choice('early_stopping', [True, False]),
             'learning_rate': hp.choice('learning_rate', ['constant', 'invscaling', 'adaptive']),
             'hidden_layer_sizes': hp.choice('hidden_layer_sizes',
                                             [(50, 50, 50,), (100, 100,), (50, 50,), (50, 25,), (50,), (25)])}

########################################################################################################################
#####PAIR MODEL WITH SPACE##############################################################################################
# key corresponds to type(model()).__name__
codex = {
    'KNeighborsClassifier': space_knn,
    'SVC': space_svm,
    'DecisionTreeClassifier': space_dt,
    'RandomForestClassifier': space_rf,
    'XGBClassifier': space_xgb
}


########################################################################################################################
#####AUX FUNCTIONS######################################################################################################
def flatten_params(params: dict):
    """
    Function that flattens parameter dicts generated by conditional spaces in hyperopt

    :param params: Dictionary with keys corresponding do parameters names and values to its values.
        Example value:
        {'C': 66.52, 'kernel': {'coef0': -10, 'gamma': 'auto', 'kernel': 'poly'}, 'shrinking': False}

    :return: Same dictionary flatten.
        Example value:
        {'C': 66.52, 'coef0': -10, 'gamma': 'auto', 'kernel': 'poly', 'shrinking': False}
    """
    inner_dicts = []
    for key in params:
        if isinstance(params[key], dict):
            inner_dicts.append(key)  # mark inner dict to remove

    for id in inner_dicts:
        t = params.pop(id)
        params.update(t)
    return params


########################################################################################################################
def optimize_model(model, dataset, target, metrics_dict, bal, norm, to_norm, max_evals,
                   space_restriction=None, k=5, early_stop=None, rs = 42, verbose=True):
    bayes_trials = Trials()

    def objective(params):
        params = flatten_params(params)  # to flatten nested params generated by conditional search space
        res = eval.cross_val(dataset, target, [model(**params)], metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k,
                             verbose=0, rs=rs)
        estimator = list(res.keys())[0]
        res = - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    model_name = type(model()).__name__
    if model_name in ['GaussianNB']:  # for null cases
        return {}
    elif model_name == "DummyClassifier":
        return {'strategy': 'uniform'}
    else:
        space = codex[model_name]

    #apply space restriction
    if space_restriction != None:
        try:
            temp_space = space.copy()
            for key in space_restriction.keys():
                temp_space[key] = space_restriction[key]
            space = temp_space
        except KeyError:
            print("\n\nSPACE RESTRICTION NOT APPLIED DUE TO ERROR IN KEY\n\n")

    if early_stop != None: early_stop = no_progress_loss(iteration_stop_count=early_stop, percent_increase=0.0)

    np.random.seed(rs)
    random.seed(rs) #necessary for certain algorithms (DT, KNN)
    rstate = np.random.RandomState(rs)  # <== Use any number here but fixed
    params = (fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials,
                   early_stop_fn=early_stop, rstate=rstate, verbose=verbose))

    # necessary normalizations of the output
    # turn indexes into correct values for hp.choice AND turns to int
    params = space_eval(space, params)
    # flattens params again
    params = flatten_params(params)
    return params







####DEPRECATED########################################################################################################

# GaussianNaiveBayes######################################################################
def optimize_nb(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5, early_stop=None):
    return {}


# KNN######################################################################
def optimize_knn(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5, early_stop=None):
    bayes_trials = Trials()

    def objective(params):
        params['n_neighbors'] = int(params['n_neighbors'])
        est = [KNeighborsClassifier(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        print(res)
        estimator = list(res.keys())[0]
        res = - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {'n_neighbors': scope.int(hp.quniform('n_neighbors', 2, 40, 1)),  # TODO: MAXIMO A 50!!!!!!!!1
             'weights': hp.choice('weights', ['distance', 'uniform'])}

    if early_stop != None: early_stop = no_progress_loss(iteration_stop_count=early_stop, percent_increase=0.0)
    res = (fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials,
                early_stop_fn=early_stop))
    # necessary normalizations of the output
    weight_dic = {0: "distance", 1: "uniform"}
    res["n_neighbors"] = int(res["n_neighbors"])
    res["weights"] = weight_dic[res["weights"]]
    return res


# SVM######################################################################
def optimize_svm(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5):
    tpe_algorithm = tpe.suggest
    bayes_trials = Trials()

    def objective(params):
        est = [SVC(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        estimator = list(res.keys())[0]
        res = 1 - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {'C': hp.uniform('C', 1, 100),
             'kernel': hp.choice('kernel', ['rbf', 'poly']),
             'gamma': hp.choice('gamma', ['scale', 'auto']),
             'shrinking': hp.choice('shrinking', [False, True])}

    res = (fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials))
    # necessary normalizations of the output
    kernel_dic = {0: "linear", 1: "rbf", 2: "poly"}
    kernel_dic = {0: "rbf", 1: "poly"}

    gamma_dic = {0: "scale", 1: "auto"}
    shrinking_dic = {0: False, 1: True}
    res["kernel"] = kernel_dic[res["kernel"]]
    res["gamma"] = gamma_dic[res["gamma"]]
    res["shrinking"] = shrinking_dic[res["shrinking"]]

    return res


# Decision Tree#####################################################################
def optimize_dt(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5):
    tpe_algorithm = tpe.suggest
    bayes_trials = Trials()

    def objective(params):
        params['max_depth'] = int(params['max_depth'])
        params['min_samples_split'] = int(params['min_samples_split'])
        params['min_samples_leaf'] = int(params['min_samples_leaf'])
        # params['max_features']=int(params['max_features'])
        est = [tree.DecisionTreeClassifier(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        estimator = list(res.keys())[0]
        res = 1 - res[estimator]["score"][0]
        # res = 1-cross_validate(tree.DecisionTreeClassifier(**params), X, y, cv=5, scoring="recall")['test_score'].mean()
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {'criterion': hp.choice('criterion', ['gini', 'entropy']),
             'splitter': hp.choice('splitter', ['best', 'random']),
             'max_depth': hp.quniform('max_depth', 3, 105, 3),
             'min_samples_split': hp.quniform('min_samples_split', 2, 20, 2),
             'min_samples_leaf': hp.quniform('min_samples_leaf', 2, 20, 2),
             'min_weight_fraction_leaf': hp.loguniform('min_weight_fraction_leaf', np.log(0.0001), np.log(0.5)),
             # 'max_features': hp.quniform('max_features', 10, 100, 10),
             'ccp_alpha': hp.loguniform('ccp_alpha', np.log(0.0001), np.log(0.2))}

    res = fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials)

    # necessary normalizations of the output
    crit_dic = {0: "gini", 1: "entropy"}
    splitter_dic = {0: "best", 1: "random"}

    res["max_depth"] = int(res["max_depth"])
    res["min_samples_split"] = int(res["min_samples_split"])
    res["min_samples_leaf"] = int(res["min_samples_leaf"])

    res["criterion"] = crit_dic[res["criterion"]]
    res["splitter"] = splitter_dic[res["splitter"]]
    return res


# Random Forest###########################################################33
def optimize_rf(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5):
    tpe_algorithm = tpe.suggest
    bayes_trials = Trials()

    def objective(params):
        params['n_estimators'] = int(params['n_estimators'])
        params['max_depth'] = int(params['max_depth'])
        params['min_samples_split'] = int(params['min_samples_split'])
        params['min_samples_leaf'] = int(params['min_samples_leaf'])
        # params['max_features']=int(params['max_features'])
        est = [RandomForestClassifier(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        estimator = list(res.keys())[0]
        res = 1 - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {'n_estimators': hp.quniform('n_estimators', 20, 200, 10),
             'criterion': hp.choice('criterion', ['gini', 'entropy']),
             'max_depth': hp.quniform('max_depth', 5, 100, 5),
             'min_samples_split': hp.quniform('min_samples_split', 2, 20, 2),
             'min_samples_leaf': hp.quniform('min_samples_leaf', 2, 20, 2),
             'min_weight_fraction_leaf': hp.loguniform('min_weight_fraction_leaf', np.log(0.0001), np.log(0.5)),
             # 'max_features': hp.quniform('max_features', 10, 100, 10),
             # bootstrap
             'ccp_alpha': hp.loguniform('ccp_alpha', np.log(0.0001), np.log(0.2))}

    res = fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials)

    # necessary normalizations of the output
    crit_dic = {0: "gini", 1: "entropy"}

    res["max_depth"] = int(res["max_depth"])
    res["min_samples_split"] = int(res["min_samples_split"])
    res["min_samples_leaf"] = int(res["min_samples_leaf"])
    res["n_estimators"] = int(res["n_estimators"])
    res["criterion"] = crit_dic[res["criterion"]]
    return res


# XGB
def optimize_xgb(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5):
    tpe_algorithm = tpe.suggest
    bayes_trials = Trials()

    def objective(params):
        params['n_estimators'] = int(params['n_estimators'])
        params['max_depth'] = int(params['max_depth'])
        params['verbosity'] = 0
        est = [XGBClassifier(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        estimator = list(res.keys())[0]
        res = 1 - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {
        'max_depth': hp.quniform('max_depth', 1, 35, 1),
        'learning_rate': hp.loguniform('learning_rate', np.log(0.0001), np.log(0.9)),
        'booster': hp.choice('booster', ['gbtree', 'gblinear', 'dart']),
        'reg_alpha': hp.loguniform('reg_alpha', np.log(0.0001), np.log(0.1)),
        'gamma': hp.loguniform('gamma', np.log(0.0001), np.log(5.0)),
        'n_estimators': hp.quniform('n_estimators', 20, 500, 10)
    }

    res = fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials)
    booster_dic = {0: "gbtree", 1: "gblinear", 2: "dart"}

    res["max_depth"] = int(res["max_depth"])
    res["n_estimators"] = int(res["n_estimators"])
    res["booster"] = booster_dic[res["booster"]]
    return res


# LightGBM
def optimize_lgbm(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5):
    tpe_algorithm = tpe.suggest
    bayes_trials = Trials()

    def objective(params):
        params['n_estimators'] = int(params['n_estimators'])
        params['max_depth'] = int(params['max_depth'])
        params['num_leaves'] = int(2 ** params['max_depth'] + 1)
        params['verbose'] = -1
        est = [LGBMClassifier(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        estimator = list(res.keys())[0]
        res = 1 - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {
        'max_depth': hp.quniform('max_depth', 1, 12, 1),
        'learning_rate': hp.loguniform('learning_rate', np.log(0.0001), np.log(0.9)),
        'boosting_type': hp.choice('boosting_type', ['gbdt', 'goss']),
        'reg_alpha': hp.loguniform('reg_alpha', np.log(0.0001), np.log(0.1)),
        'n_estimators': hp.quniform('n_estimators', 20, 400, 20)
    }

    res = fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials)
    booster_dic = {0: "gbdt", 1: "goss"}

    res["max_depth"] = int(res["max_depth"])
    res["n_estimators"] = int(res["n_estimators"])
    res["boosting_type"] = booster_dic[res["boosting_type"]]
    return res


#   LOGISTIC
def optimize_lr(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5):
    tpe_algorithm = tpe.suggest
    bayes_trials = Trials()

    def objective(params):
        # print(params)
        temp = params['solver']
        params['solver'] = temp['solver']
        if (isinstance(temp['penalty'], dict)):
            temp2 = temp['penalty']
            params['penalty'] = temp2['penalty']
            if ('dual' in temp2.keys()):
                params['dual'] = temp2['dual']
            elif ('l1_ratio' in temp2.keys()):
                params['l1_ratio'] = temp2['l1_ratio']
        else:
            params['penalty'] = temp['penalty']

        est = [LogisticRegression(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        estimator = list(res.keys())[0]
        res = 1 - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {'fit_intercept': hp.choice('fit_intercept', [True, False]),
             'C': hp.uniform('C', 0.0, 2.0),
             'solver': hp.choice('solver',
                                 [{'solver': 'liblinear',
                                   'penalty': hp.choice('penalty',
                                                        [{'penalty': 'l1'},
                                                         {'penalty': 'l2',
                                                          'dual': hp.choice('l2_dual', [True, False])}])},
                                  {'solver': 'newton-cg',
                                   'penalty': hp.choice('newton_penalty', ['l2', 'none'])},
                                  {'solver': 'sag',
                                   'penalty': hp.choice('sag_penalty', ['l2', 'none'])},
                                  {'solver': 'lbfgs',
                                   'penalty': hp.choice('lbfgs_penalty', ['l2', 'none'])},
                                  {'solver': 'saga',
                                   'penalty': hp.choice('saga_penalty',
                                                        [{'penalty': 'l2'},
                                                         {'penalty': 'none'},
                                                         {'penalty': 'elasticnet',
                                                          'l1_ratio': hp.uniform('l1_ratio', 0.0, 1.0)}])}])}

    res = fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials)
    inter_dic = {0: True, 1: False}
    solver_dic = {0: "liblinear", 1: "newton-cg", 2: "sag", 3: "lbfgs", 4: "saga"}

    res["fit_intercept"] = inter_dic[res["fit_intercept"]]
    res["solver"] = solver_dic[res["solver"]]
    if res["solver"] == "liblinear":
        penalty = {0: "l1", 1: "l2"}
        dual = {0: True, 1: False}
        res["penalty"] = penalty[res["penalty"]]
        if res["penalty"] == "l2":
            res["dual"] = dual[res["l2_dual"]]
            res.pop("l2_dual")
    elif res["solver"] == "saga":
        penalty = {0: "l2", 1: "none", 2: "elasticnet"}
        res["penalty"] = penalty[res["saga_penalty"]]
        res.pop("saga_penalty")
    else:
        pen = [s for s in list(res.keys()) if "penalty" in s][0]
        penalty = {0: "l2", 1: "none"}
        res["penalty"] = penalty[res[pen]]
        res.pop(pen)
    return res


# MLP
def optimize_mlp(dataset, target, metrics_dict, bal, norm, to_norm, max_evals, k=5):
    tpe_algorithm = tpe.suggest
    bayes_trials = Trials()

    def objective(params):
        params['batch_size'] = int(params['batch_size'])
        est = [MLPClassifier(**params)]
        res = eval.cross_val(dataset, target, est, metrics_dict, bal=bal, norm=norm, to_norm=to_norm, k=k, verbose=0)
        estimator = list(res.keys())[0]
        res = 1 - res[estimator]["score"][0]
        return {'loss': res, 'params': params, 'status': STATUS_OK}

    space = {
        'alpha': hp.loguniform('alpha', np.log(0.0001), np.log(0.2)),
        'learning_rate_init': hp.loguniform('learning_rate_init', np.log(0.001), np.log(0.2)),
        'batch_size': hp.quniform('batch_size', 50, 300, 50),
        'early_stopping': hp.choice('early_stopping', [True, False]),
        'learning_rate': hp.choice('learning_rate', ['constant', 'invscaling', 'adaptive']),
        'hidden_layer_sizes': hp.choice('hidden_layer_sizes',
                                        [(50, 50, 50,), (100, 100,), (50, 50,), (50, 25,), (50,), (25)])}

    res = fmin(fn=objective, space=space, algo=tpe.suggest, max_evals=max_evals, trials=bayes_trials)

    early_dic = {0: True, 1: False}
    lrate_dic = {0: "constant", 1: "invscaling", 2: "adaptive"}
    hidden_dic = {0: (50, 50, 50,), 1: (100, 100,), 2: (50, 50,), 3: (50, 25,), 4: (50,), 5: (25)}

    res["batch_size"] = int(res["batch_size"])
    res["early_stopping"] = early_dic[res["early_stopping"]]
    res["learning_rate"] = lrate_dic[res["learning_rate"]]
    res["hidden_layer_sizes"] = hidden_dic[res["hidden_layer_sizes"]]

    return res
