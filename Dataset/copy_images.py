import shutil
import os
from pathlib import Path

path = Path('../datasets/images')
copy_path = Path('../datasets/half_images')
data = [x for x in path.iterdir() if x.is_dir()]
for i in data:
    Class = [x for x in i.iterdir() if x.is_dir()]
    for k in Class:
        product = [x for x in k.iterdir() if x.is_file()]
        copy_path = Path('/home2/findproduct/Capstone/datasets/half_images')
        copy_path = copy_path / i.parts[-1] / k.parts[-1]
        print(copy_path)
        os.makedirs(copy_path)
        for l in product:
            if '_s_' in l.parts[-1]:
                shutil.copy(l, str(copy_path))