import shutil
from pathlib import Path
import os

path = Path('E:\Ai_Data')
data = [x for x in path.iterdir() if x.is_dir()]
for i in data:
    path = path / i
    Classification = [x for x in path.iterdir() if x.is_dir()]
    for j  in Classification:
        path = path / j
        type_data = [x for x in path.iterdir() if x.is_dir()]
        for t in type_data:
            path = path / t
            file_name = [x for x in path.iterdir() if x.is_file()]
            for l in file_name:
                move_file = j / l.parts[-1]
                shutil.move(l, move_file)
            os.rmdir(t)
        