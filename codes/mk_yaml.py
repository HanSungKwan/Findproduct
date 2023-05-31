import yaml
from pathlib import Path

names = {}
train = []
validation = []
path = Path('images')
Classification = [x for x in path.iterdir() if x.is_dir()]
for i in Classification:
    type_data = [x for x in i.iterdir() if x.is_dir()]
    for j in type_data:
        types = j.parts[-1]
        if types == "images":
            Class_names = [x for x in j.iterdir() if x.is_dir()]
            for k in range(len(Class_names)):
                if 'Training' in Class_names[k].parts[-3]:
                    train.append(str(Class_names[k]))
                elif 'Validation' in Class_names[k].parts[-3]:
                    validation.append(str(Class_names[k]))
                temp = Class_names[k].parts[-1]
                names[k] = temp
Yaml = {}
Yaml['path'] = '../images'
Yaml['val'] = validation
Yaml['train'] = train
Yaml['names'] = names


with open('test.yaml', 'w') as f:
    yaml.safe_dump(Yaml, f, allow_unicode=True)