import re
import pandas as pd
import numpy as np
import scipy.spatial.distance as distance
from sklearn.metrics.pairwise import euclidean_distances
import time

realdata_file = "dataset_union1.csv"
data_dir = "./data/"
id = "id"
target = "ipet2"

def get_patterns(data_file, discretize=True, get_lifts=False):
    file = open(data_file, "r")
    real_data = pd.read_csv(data_dir + realdata_file).set_index("id")
    text = file.read()

    r = "\nFOUND BICS:#[0-9]+\n"

    foundbics = re.search("FOUND BICS:#([0-9]+)", text).group(1)
    if foundbics == "0":
        raise Exception("Found 0 bics")

    _, text2 = re.split(r, text)

    text2 = text2.split("\n\n")[0]

    patterns = {}
    for i, l in enumerate(text2.split("\n")):
        #print(l)
        cols = re.search("Y=\[([^\]]*)\]", l)
        cols = list(cols.group(1).split(","))
        if discretize:
            vals = re.search("I=\[([^\]]*)\]", l)
            vals = vals.group(1).split(",")
            vals = list(map(int, vals))
        else:
            x = re.search("X=\[([^\]]*)\]", l)
            x = x.group(1).split(",")
            sub_data = real_data[real_data.index.isin(x)][cols].mean()
            vals = sub_data.values.tolist()
            cols = sub_data.index.tolist()
        if get_lifts:
            lifts = re.search("Lifts=\[([^\]]*)\]", l)
            lifts = lifts.group(1).split(",")
            lifts = list(map(float, lifts))

            patterns[i] = (cols, vals, max(lifts))
        else:
            patterns[i] = (cols, vals)
    file.close()
    return patterns


def get_data(data_file, discretize=True):
    if not discretize:
        return pd.read_csv(data_dir + realdata_file).set_index("id")


    file = open(data_file, "r")

    text = file.read()
    r = "\n\nFOUND BICS:#[0-9]+\n"
    #text1, _ = re.split(r, text)
    text1 = re.split(r, text)[0] #always correct even when r doesnt exist


    splited = text1.strip().split("\n")
    rows = splited[0]
    cols = splited[1]
    cols = re.search("Courses: \[([^\]]*)\]", cols).group(1).replace(" ", "").split(',')
    text1 = splited[2:]

    df = []
    for l in text1:
        id_var, vals = l.split("=>")
        vals = vals[:-1].replace(",", ".").split("|")
        vals = list(map(float, vals))
        df.append([id_var] + vals)

    data = pd.DataFrame(df, columns=[id] + cols)
    data = data.set_index(id)
    file.close()
    return data


def transform_data(data, patterns, class_file, transform="distance", binary_threshold=None):
    classes = pd.read_csv(class_file)
    df = pd.DataFrame(columns=list(patterns.keys()), index=data.index)
    for col in df.columns:
        pat_cols = patterns[col][0]
        pat_vals = patterns[col][1]
        v = data[pat_cols].values
        if transform=="distance":
            new_vals = euclidean_distances(v, [pat_vals])
        elif transform=="binary":
            v = abs(v - pat_vals)
            new_vals = v > binary_threshold
            new_vals = np.any(new_vals, axis=1) == False # == False to negate values
        else:
            raise Exception(f"Transform '{transform}' not supported.")
        df[col] = new_vals

    df = pd.merge(df, classes, on=id)
    df = df.set_index(id)
    return df

def complete_transform(data_file, discretize=True):
    patterns = get_patterns(data_file, discretize=discretize)
    data = get_data(data_file, discretize=discretize)
    classes = pd.read_csv("data/classes.csv")
    df = transform_data(data, patterns, classes)
    return df

