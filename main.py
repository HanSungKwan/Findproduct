# testing
from typing import Annotated

from fastapi import FastAPI, File, Form, UploadFile, HTTPException
import pydantic as pyd
import aiofiles as aio

app = FastAPI()

@app.get("/tTxt/{txt}")
async def test_text(txt:str):
    print(txt)
    return {"received_D" : txt}


@app.get("/tQry/")
async def test_query(qry: str):
    return {"received_D" : qry}


class tDTO(pyd.BaseModel):
    text : str

@app.post("/tJsn/")
async def test_json(jsn : tDTO):
    print(f"received data: {jsn.dict()}[format=dict] | {jsn}[format=Basemodel] | {jsn.text}[format=str]")
    return {"received" : "server_received",
            **jsn.dict() }

@app.post("/tForm/")
async def test_form(text: Annotated[str, Form()]):
    print(f"server received data : {text}")
    return {"server received data" : text}

@app.post("/tFile/")
async def create_upload_file(inFile: UploadFile = File()):
    # 오류: input file != image
    if 'image' not in inFile.content_type:
        return {"filename" : inFile.filename,
                "message" : "Wrong Content type"}
    # 정상: input file == image
    else:
        async with aio.open(f"./rsc/images/{inFile.filename}", mode='wb') as pxd_f:
            content = await inFile.read()
            await pxd_f.write(content)
        # 경로 분석 넣기
        return {"filename" : inFile.filename,
            "message":"Hello World"}
    
