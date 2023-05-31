import torch
from ultralytics import YOLO
import cv2

# print(torch.cuda.is_available())
# print(torch.cuda.current_device())
# print(torch.cuda.get_device_name())
# print(torch.cuda.device_count())

# build a new model from scratch
model = YOLO("yolov8m.yaml")
# del model
# load a(n official) pretrained model
# model.load("yolov8m.pt")

# train
model.train(data="./datas/test.yaml", name="yoloTest00",
            epochs=3, batch=3, imgsz=2988,
            lr0=0.01, lrf=0.01, optimizer='SGD',
            device=0, save_period=30)
# device = 0 는 글카 0번 혹은 device=cpu 하면 cpu 사용
# batch=-1은 AutoBatch
# patience = 50(default)는 학습 중 개선이 없으면 patience 수치만큼 기다리다 학습 조기종료
# workers = 8(default)는 데이터를 로드하는 스레드 수 설정
# save_period는 체크포인트를 저장하는 epoch간격

# val
# metrics= model.val()

# model export
# success = model.export()
# default로 pytorch.
# # 위의 model.load의 "yolov8n.pt"또한 pytorch 모델이기 때문에 pt.