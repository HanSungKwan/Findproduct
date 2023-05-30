import xml.etree.ElementTree as ET
from pathlib import Path
import os

def convert_label(path ,lable_id ,image_id):
  def convert_box(size, box):
    dw, dh = 1. / size[0], 1. / size[1]
    x, y, w, h = (box[0] + box[1]) / 2.0 - 1, (box[2] + box[3]) / 2.0 - 1, box[1] - box[0], box[3] - box[2]
    return x * dw, y * dh, w * dw, h * dh

  path = path / f'{image_id}.xml'
  in_file = open(path,'rt', encoding='UTF8')
  out_file = open( lable_id / f'{image_id}.txt', 'w',encoding='UTF8')
  tree = ET.parse(in_file)
  root = tree.getroot()
  size = root.find('size')
  w = int(size.find('width').text)
  h = int(size.find('height').text)

  global names  # names list
  for obj in root.iter('object'):
    cls = obj.find('name').text
    if cls in names and int(obj.find('difficult').text) != 1:
      xmlbox = obj.find('bndbox')
      bb = convert_box((w, h), [float(xmlbox.find(x).text) for x in ('xmin', 'xmax', 'ymin', 'ymax')])
      cls_id = names.index(cls)  # class id
      out_file.write(" ".join([str(a) for a in (cls_id, *bb)]) + '\n')
    print(out_file)

def search():
  global names
  names = []
  path = Path('D:\Ai_Data\images')
  data = [x for x in path.iterdir() if x.is_dir()]
  for i in data:
    path = path / i
    Classification = [x for x in path.iterdir() if x.is_dir()]
    for j  in Classification:
      xml_name = j.parts[-1]
      if xml_name.startswith('[라벨]'):
        path = path / j
        type_data = [x for x in path.iterdir() if x.is_dir()]
        for k  in type_data:
          path = path / k
          temp = k.parts[-1]
          temp = temp.split('_')
          names.append(temp[1])
          list_1 = [x for x in path.iterdir() if x.is_file()]
          for t in list_1:
            if not str(t).endswith('meta.xml'):
              T = t.parts[0:6]
              file_path = Path('.')
              path = Path('.')
              for file in T:
                path = path / file
              file_path = path
              Lid = str(t.parts[-1])
              Lid = Lid[:-4]
              convert_label(path,file_path,Lid)
            elif str(t).endswith('meta.xml'):
              os.remove(t)
      else: pass

search()
