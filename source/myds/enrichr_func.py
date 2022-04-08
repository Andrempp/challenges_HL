import json
import requests
import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
from IPython.display import display 
from . import plot
import tese_func

ADDLIST_URL = 'http://maayanlab.cloud/Enrichr/addList'
GETLIST_URL = 'http://maayanlab.cloud/Enrichr/view?userListId=%s'
ENRICH_URL = 'http://maayanlab.cloud/Enrichr/enrich'
query_string = '?userListId=%s&backgroundType=%s'
columns = ["rank", "term", "p_value", "odds_ratio", "c_score", "over_genes", "adjusted_p_value",
           "old_p_value", "adjusted_old_p_value"]

def create_list(gene_list, description=None):
    genes_str = '\n'.join(gene_list)
    payload = {
    'list': (None, genes_str),
    'description': (None, description)
    }
    response = requests.post(ADDLIST_URL, files=payload)
    if not response.ok:
        raise Exception('Error CREATING gene list')
    data = json.loads(response.text)
    return data["userListId"]

def get_genes(listId):
    response = requests.get(GETLIST_URL % listId)
    if not response.ok:
        raise Exception('Error getting gene list')

    data = json.loads(response.text)
    return data["genes"], data["description"]



def get_enrichment(listId, library, as_dataset=False):
    response = requests.get(
    ENRICH_URL + query_string % (listId, library)
     )
    if not response.ok:
        raise Exception('Error fetching enrichment results')
    data = json.loads(response.text)[library]
    
    if as_dataset:
        data = pd.DataFrame.from_records(data, columns=columns)

    return data

def term_graphics(sets:dict, library, score = "c_score", bar_score = "p_value", n_single=10, n_mult=10, 
                  figsize=(12,6), orient="v", font_size=10, rot=20, titles=[], return_data=False, verbose=3,
                  alpha=0.85, save=None):

    ascending = False if score=="c_score" else True
    ids = {k: create_list(v) for k,v in sets.items()}
    datasets = {k: get_enrichment(ids[k], library, as_dataset=True).sort_values(score, ascending=ascending) for k in ids.keys()}
    for k in datasets:
        datasets[k]["-log10(p-value)"] = - np.log10(datasets[k]["adjusted_p_value"])

    if score == "adjusted_p_value": score = "-log10(p-value)"
    columns = datasets[list(datasets.keys())[0]].columns
    x = "term"
    y = score
    #single graphics
    for i, k in enumerate(datasets.keys()):
        d = datasets[k]

        d["set"] = k
        df = d.sort_values(score, ascending=False).head(n_single)

        if verbose > 0:
            if orient=="h":
                figsize = (figsize[1], figsize[0])
                x = score
                y = "term"
                rot=0

            plt.figure(figsize=figsize)
            ax_r = sns.barplot(x=x, y=y, data=df, orient=orient, alpha=alpha)
            ymin, ymax = ax_r.get_ylim()
            ax_r.set_ylim(ymin, ymax*1.05)
            ##annotation###################
            rects = ax_r.patches
            v = df[bar_score].apply(lambda x: '{:.2e}'.format(x)).tolist()
            plot.autolabel(ax_r, rects, v, threshold=0, percentage=False, orient=orient)
            ###############################
            if orient=="v":
                ax_r.set_xticklabels(ax_r.get_xticklabels(), fontsize=font_size, rotation=-3, ha="left")
                ax_r.set_xlabel("")
                for xlabel_i in ax_r.axes.get_xticklabels()[1:]:
                    xlabel_i.set_visible(False)
                a = ax_r.get_xticks().tolist()
                a[0] = 'positive regulation of Th1 immune response'
                ax_r.set_xticklabels(a, fontsize=font_size, rotation=-5, ha="left")
            if len(titles)>0: ax_r.set_title(titles[i])
            plt.tight_layout()
            if save!=None: plt.savefig(save, dpi=150)
            plt.show()

    #mult graphics
    terms = set()
    for k in datasets.keys():
        terms.update(datasets[k]["term"].tolist())
    sums = {}

    mult_bar = pd.DataFrame(columns=columns)
    for k in datasets.keys():
        mult_bar = mult_bar.append(datasets[k])

    for t in terms:
        sums[t] = mult_bar[mult_bar["term"] == t][score].sum()

    sums = {k: v for k, v in sorted(sums.items(), key=lambda item: item[1], reverse=True)}
    sorter = list(sums.keys())
    top = sorter[:n_mult]
    # Create the dictionary that defines the order for sorting
    sorterIndex = dict(zip(sorter, range(len(sorter))))

    # Generate a rank column that will be used to sort
    # the dataframe numerically
    mult_bar['term_rank'] = mult_bar['term'].map(sorterIndex)

    mult_bar.sort_values(['term_rank', 'set'], ascending = True, inplace = True)
    mult_bar.drop('term_rank', 1, inplace = True)
    mult_bar = mult_bar[mult_bar["term"].isin(top)]
    #########################33
    if verbose>1:
        plt.figure(figsize=figsize) # this creates a figure 8 inch wide, 4 inch high
        ax = sns.barplot(x="term", y=score, hue="set", data=mult_bar)
        ax.set_xticklabels(ax.get_xticklabels(), fontsize=font_size, rotation=rot, ha="right")
        plt.tight_layout()
        plt.show()

    #table with overlaps
    pd.set_option('display.max_colwidth', None)
    table = mult_bar[["term","set","over_genes", score]]
    table["over_genes"] = table["over_genes"].map(sorted).copy()
    table = table.reset_index(drop=True)
    if verbose>2:
        display(table)

    if return_data:
        return datasets
    

def scatter_genes_term(d, data, target, n_terms, diag_line=False, exp=False, use_axes=False, l=3):
    terms = d["term"][:n_terms].tolist()
    l1, l2 = np.array([0,0]), np.array([1,1]) #to define diagonal line
    pos_ind = data[target] == 1
    neg_ind = data[target] == 0
    cols = ["gene", "pos_val", "neg_val", "distance", "term"]
    final_df = pd.DataFrame(columns=cols)
    temp_dfs = []

    for i,term in enumerate(terms):
        t = pd.DataFrame(columns=cols)
        over_genes = d[d["term"]==term]["over_genes"].tolist()[0]
        t["gene"] = over_genes

        for _,r in t.iterrows():
            if exp:
                r["pos_val"] = np.exp(data[r["gene"]][pos_ind]).mean()
                r["neg_val"] = np.exp(data[r["gene"]][neg_ind]).mean()
            else:
                r["pos_val"] = data[r["gene"]][pos_ind].mean()
                r["neg_val"] = data[r["gene"]][neg_ind].mean()
            point = np.array([r["neg_val"], r["pos_val"]])
            r["distance"] = abs(np.cross(l2-l1,point-l1)/np.linalg.norm(l2-l1))
            r["term"] = term
        final_df = final_df.append(t, ignore_index=True)
        
    if not use_axes:
        plt.figure(figsize=(8,8))
        ax = sns.scatterplot(data=final_df, x="neg_val", y="pos_val", hue="term")  
        lim_l = min(final_df["pos_val"].min(), final_df["neg_val"].min()) * 0.8
        lim_u = max(final_df["pos_val"].max(), final_df["neg_val"].max()) * 1.05
        ax.set_ylim([lim_l, lim_u])
        ax.set_xlim([lim_l, lim_u])
        if diag_line:
            ax.plot([0, 1], [0, 1], ls="--", c=".6",transform=ax.transAxes)
        ax.legend(loc="best")
    else:
        a,b = plot.choose_grid(len(terms), l)
        fig, axs = plt.subplots(a, b, figsize=(b*6, a*4))
        axs = axs.flatten()
        for i,term in enumerate(terms):
            t_df = final_df[ final_df["term"]==term ]
            sns.scatterplot(data=t_df, x="neg_val", y="pos_val", ax=axs[i])
            lim_l = min(t_df["pos_val"].min(), t_df["neg_val"].min()) * 0.8
            lim_u = max(t_df["pos_val"].max(), t_df["neg_val"].max()) * 1.05
            axs[i].set_ylim([lim_l, lim_u])
            axs[i].set_xlim([lim_l, lim_u])
            min_distance = (lim_u - lim_l)/20
            for _, p in t_df.iterrows():
                if p["distance"] > min_distance:
                    axs[i].annotate(p["gene"],xy=(p["neg_val"], p["pos_val"]), xycoords='data')
            if diag_line:
                axs[i].plot([0, 1], [0, 1], ls="--", c=".6",transform=axs[i].transAxes)
            axs[i].set_title(term)
    plt.show()
        

def boxplot_genes_term(d, data, target, n_terms, exp=False, leg_size=12, title_size=16, axis_size=18,
                       xtick_size=14, rot=20, figsize=(12,4), max_genes=50):
    terms = d["term"][:n_terms].tolist()
    
    for t in terms:
        box = pd.DataFrame(columns=["values", "feature", "target"])

        genes = d[d["term"]==t]["over_genes"].tolist()[0]

        #get order of FC values
        fc = data[genes + [target]]
        fc_genes = tese_func.logfc_select(fc, target, None).sort_values()
        genes = list(fc_genes.index)
        values = list(fc_genes.values)

        if len(genes) > max_genes:
            split = max_genes//2
            genes = genes[:split] + genes[-split:]
            values = values[:split] + values[-split:]
        for g in genes:
            temp = pd.DataFrame()
            v = np.exp(data[g]) if exp else data[g]
            temp["values"] = v
            temp["feature"] = g
            temp["target"] = data[target]
            box = box.append(temp)
        box["target"] = box["target"].replace({0: "Negative iPET2", 1:"Positive iPET2"})
        fig, ax = plt.subplots(1,1, figsize=figsize)
        sns.boxplot(x="feature", y="values", hue="target", data=box, order=genes, ax=ax)

        #annotate FC values
        xtickslocs = ax.get_xticks()
        ymin, ymax = ax.get_ylim()
        cors = [(xtick, ymax) for xtick in xtickslocs]
        for c,v in zip(cors, values):
            ax.annotate(f'{v:.{2}f}',
                       xy=(0,0),
                       xytext=c,  # 3 points vertical offset
                       xycoords="data",
                        ha='center', va='center', size="x-large")


        handles, labels = ax.get_legend_handles_labels()
        ax.legend(handles=handles, labels=labels, fontsize=leg_size, loc="lower center")
        ax.set_xlabel("Genes", fontsize=axis_size)
        ax.set_ylabel("Expression values", fontsize=axis_size)
        ax.set_title(t, fontsize=title_size)
        ax.set_ylim(ymin,ymax*1.05)
        ax.set_xticklabels(ax.get_xticklabels(), fontsize=xtick_size, rotation=rot, ha="right")
        plt.tight_layout()
        plt.show()
    
    
def fc_genes_term(d, data, target, n_terms, title_size=16, axis_size=12, figsize=(16,3)):
    terms = d["term"][:n_terms].tolist()
    for t in terms:
        genes = d[d["term"]==t]["over_genes"].tolist()[0]
        
        df = data[genes + [target]]
        fc_genes = tese_func.logfc_select(df, target, None)
        fc_genes = fc_genes.reindex(fc_genes.abs().sort_values().index) #order by absolute

        fig, ax = plt.subplots(1,1, figsize=figsize)
        ax = sns.barplot(data=pd.DataFrame(fc_genes).T,  palette="Blues_d")
        ax.set_title(t, fontsize=title_size)
        ax.set_xticklabels(ax.get_xticklabels(), fontsize=axis_size, rotation=40, ha="right")
        u_lim = max([0.2,max(fc_genes)])
        l_lim = min([-0.2, min(fc_genes)])
        ax.set_ylim(l_lim*1.1, u_lim*1.1)
        plt.show()

    
    
    
    
def boxplot_fc_genes(data, genes, target, ax=None, leg_size=12, title_size=16, axis_size=18):
    box = pd.DataFrame(columns=["values", "feature", "target"])

    # get order of FC values
    fc = data[genes + [target]]
    fc_genes = tese_func.logfc_select(fc, target, None).sort_values()
    genes = list(fc_genes.index)
    values = list(fc_genes.values)

    for g in genes:
        temp = pd.DataFrame()
        v = data[g]
        temp["values"] = v
        temp["feature"] = g
        temp["target"] = data[target]
        box = box.append(temp)
    box["target"] = box["target"].replace({0: "Negative iPET2", 1: "Positive iPET2"})

    if ax==None: _, ax = plt.subplots(1,1, figsize=(12,12))
    sns.boxplot(x="feature", y="values", hue="target", data=box, order=genes, ax=ax)

    # annotate FC values
    xtickslocs = ax.get_xticks()
    ymin, ymax = ax.get_ylim()
    cors = [(xtick, ymax) for xtick in xtickslocs]
    for c, v in zip(cors, values):
        print(v)
        ax.annotate(f'{v:.{2}f}',
                    xy=(0, 0),
                    xytext=c,  # 3 points vertical offset
                    xycoords="data",
                    ha='center', va='center', size="large")

    handles, labels = ax.get_legend_handles_labels()
    ax.legend(handles=handles, labels=labels, fontsize=leg_size, loc="lower center")
    ax.set_xlabel("Genes", fontsize=axis_size)
    ax.set_ylabel("Values", fontsize=axis_size)
    ax.set_ylim(ymin, ymax * 1.05)
    return ax
    
    
    
    
    
    
    
