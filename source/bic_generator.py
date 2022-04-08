import json
import re
import pandas as pd
import numpy as np
import scipy.spatial.distance as distance
from bic_functions import *
import os

data_file = "dataset_forbic.csv"


data_dir = "./data/"
id = "id"
target = "ipet2"

nrbics = 250
nrIterations = 9
minlift = 1.3
labels = 10
filter_by_lift = True


data = pd.read_csv(data_dir + data_file).set_index(id)

out_train_file = "../bicpams_5.0/output/result_temp1.txt"
out_test_file = "../bicpams_5.0/output/result_temp2.txt"
in_file = "../bicpams_5.0/data/result_temp.csv"
classes_file = "./data/classes.csv"

print("###Parameters used##")
print(f"Nr. Iter:  {nrIterations}\t#\nMin Lift: {minlift}\t#\nMin Bics: {nrbics}\t#\nNr. Labels: {labels}\t#\nFilter lift: {filter_by_lift}\t#")
print("####################")

# get patterns and transformed data from train dataset
data.to_csv(in_file)
dir_command = "cd ../bicpams_5.0/"
jar_command = f"java -jar bic.jar {nrbics} {nrIterations} {minlift} {1} {labels} {filter_by_lift}"
res = os.system(dir_command + ";" + jar_command)
print("BicPAMS returns: ", res)
patterns = get_patterns(out_train_file, get_lifts=filter_by_lift)
if filter_by_lift:
    patterns = sorted(patterns.items(), key=lambda item: item[1][2], reverse=True)[:nrbics]  # order by lift
    patterns = {k: v for k, v in patterns}
data_disc = get_data(out_train_file)
df_train = transform_data(data_disc, patterns, classes_file)



sufix = f"_{nrIterations}_{minlift}_{nrbics}_{labels}_{filter_by_lift}"
file_name = data_file.replace(".csv", f"{sufix}.json")

with open(f"./data/bic_patterns/{file_name}", "w") as f:
    json.dump(patterns, f)

path = data_dir + data_file.replace(".csv", f'_{nrIterations}_{minlift}_{nrbics}_{labels}.csv')
df_train.to_csv(path, index=True)