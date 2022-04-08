import matplotlib.pyplot as plt

def autolabel(ax, rects, labels, decimal=2 ,threshold=0, percentage=True, orient="v", size=12):
    """Attach a text label above each bar in *rects*, displaying its height."""
    symbol = "%" if percentage else ''
    if orient == "v":
        for rect, label in zip(rects,labels):
            if type(label) == str or label > threshold:
                height = rect.get_height()
                if height >= 0.95: height -=0.06
                if type(label)==str: l=f'{label}'
                else: l = f'{label:.{decimal}f}{symbol}'
                ax.annotate(l,
                            xy=(rect.get_x() + rect.get_width() / 2, height),
                            xytext=(0, 3),  # 3 points vertical offset
                            textcoords="offset points",
                            ha='center', va='bottom', size=size)
    elif orient == "h":
        for rect, label in zip(rects, labels):
            if type(label) == str or label > threshold:
                width = rect.get_width()
                if width >= 0.95: width -= 0.06
                #print(rect.get_y())
                #print(rect.get_height()/2)
                ax.annotate(f'{label:.{decimal}f}{symbol}',
                            xy=(width, rect.get_y() + rect.get_height()/2),
                            xytext=((size/2)*decimal + (size/4), -size/2),  # 3 points vertical offset
                            textcoords="offset points",
                            ha='center', va='bottom', size=size)
                
                
def choose_grid(nr, l):
    if nr%l == 0: i = 0
    else: i = 1
    return nr // l + i, l