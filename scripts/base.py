import os
import pandas as pd
from PIL import Image
import matplotlib.pyplot as plt

def get_images(directory: str, label: str) -> list:
    """return all images in a directory as a list"""
    return [(os.path.join(directory,item), label) for item in os.listdir(directory)]

def create_dataframe(data: list[tuple]) -> pd.DataFrame:
    """create images from a list and return a dataframe"""
    df = pd.DataFrame()
    image = []
    label = []
    for item in data:
        image.append(plt.imread(Image.open(item[0])))
        label.append(item[1])
    df["image"] = image
    df["label"] = label
    
    return df

def shuffle_dataframe(df: pd.DataFrame, frac: float=1.0):
    """shuffles a dataset"""
    return df.sample(frac=frac)