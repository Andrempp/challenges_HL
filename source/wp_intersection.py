#import upsetplot
import os
from supervenn import supervenn
import matplotlib.pyplot as plt
import pandas as pd
import json


#################Chunck ordering########################3
# 'minimize gaps': default, use an optimization algorithm to find an order of columns with fewer gaps in each row;
# 'size': bigger chunks go first;
# 'occurrence': chunks that are in more sets go first;
# 'random': randomly shuffle the columns.
########################################################3

#################Sets ordering########################3
# None: default - keep the order of sets as passed into function;
# 'minimize gaps': use the same algorithm as for chunks to group similar sets closer together. The difference in the algorithm is that now gaps are minimized in columns instead of rows, and they are weighted by the column widths (i.e. chunk sizes), as we want to minimize total gap width;
# 'size': bigger sets go first;
# 'chunk count': sets that contain most chunks go first;
# 'random': randomly shuffle the rows.
######################################################3

names_replace = {'Dummy': "Random", 'GaussianNB': 'Naive Bayes', 'KNeighbors': "KNN", 'SVC': "SVM",
                 'DecisionTree': "Decision Tree", 'RandomForest': "Random Forest", 'XGB': "XGBoost"}

data_dir = "./data/"
file_dir = "./files/"
datasets = ["dataset_union1.csv"]
#check if all files exist
for f in datasets:
    if not os.path.exists(data_dir + f): raise Exception(f"File {f} not found")


side_plots = False
c_ordering = "occurrence"
s_ordering = "minimize gaps"
width_ratio = 0.01
width_annot = 1
for i in range(0, len(datasets)):
    file_name = datasets[i].replace(".csv", "_wrongpred_dict.json")
    with open(file_dir + 'wrongpred/' + file_name, "r") as file_dict:
        wrong_preds = json.load(file_dict)['0']

    wrong_preds.pop("Dummy")
    sets = []
    labels = []
    for item in wrong_preds.items():
        labels.append(names_replace[item[0]])
        sets.append(set(item[1]))

    print(labels)
    plt.figure(figsize=(20, 6))
    plt.title("Overlap of wrongly predicted individuals", fontsize=16)
    my_supvenn = supervenn(sets, labels, side_plots=side_plots, chunks_ordering=c_ordering,
                    sets_ordering=s_ordering,
                    #widths_minmax_ratio=width_ratio,
                    min_width_for_annotation=width_annot)

    my_supvenn.axes["main"].set_ylabel("Classifiers", fontsize=14)
    my_supvenn.axes["main"].set_xlabel("Wrongly predicted individuals", fontsize=14)

    chunk1 = my_supvenn.get_chunk([0,1,2,3,4,5]) #all wrong

    chunk2 = [] #only one wrong
    chunk2.append(my_supvenn.get_chunk([0])) #GaussianNB
    chunk2.append(my_supvenn.get_chunk([2])) #SVC
    chunk2.append(my_supvenn.get_chunk([5])) #XGB
    chunk2.append(my_supvenn.get_chunk([3])) #DT
    chunk2 = set().union(*chunk2)

    chunk3 = [] #only KNN or DT worng
    chunk3.append(my_supvenn.get_chunk([3]))  # DT
    chunk3.append(my_supvenn.get_chunk([1, 3])) # KNN and DT
    chunk3 = set().union(*chunk3)

    print(chunk1)
    print(chunk2)
    print(chunk3)
    plt.tight_layout()
    #plt.savefig(dir+"set_intersection.png")
    plt.show()

    with open('files/wrongpred/allwrong.txt', 'w') as f:
        f.write(json.dumps(list(chunk1)))

    with open('files/wrongpred/onewrong.txt', 'w') as f:
        f.write(json.dumps(list(chunk2)))

    with open('files/wrongpred/knndtwrong.txt', 'w') as f:
        f.write(json.dumps(list(chunk3)))
