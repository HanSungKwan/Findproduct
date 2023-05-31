package com.capstone.camerausage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MainActivity: AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    private val RQ_SPEECH_REC = 102

    private val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something!")//stt때 화면에 보여줄 text
    }

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val result: String? =
                    data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                if (result != null) {
                    Log.d("박인선", result) //내 텍스트 확인하는 코드

                    val jsonObject = JSONObject() // json 인스턴스 생성
                    jsonObject.put("text", result)//text란 태그로 stt값을 post로 보내주겠다.

                    postRequest(jsonObject.toString())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CameraUsage) //splash 나오는 화면 (수정)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this@MainActivity::onInit)


        val camera_button = findViewById<Button>(R.id.camera_btn)
        camera_button.setOnClickListener {
            val intent = Intent(this@MainActivity, CameraActivity::class.java)
            startActivity(intent)
        }
        //camera_button.performClick() // 버튼 자동 실행



//        val stt_button = findViewById<Button>(R.id.stt_btn)
//        stt_button.setOnClickListener {
//            speechRecognizer.startListening(speechRecognizerIntent)
//            startForResult.launch(speechRecognizerIntent)
//        }
//
//        val tts_button = findViewById<Button>(R.id.tts_btn)
//        tts_button.setOnClickListener{
//            //initializeTextToSpeech()
//        }

    }



    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            tts.setPitch(1.0f);
            tts.setSpeechRate(1.0f);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 언어 데이터가 없거나 지원되지 않는 경우 처리하는 부분을 추가하세요.
            } else {
                val utteranceId = "utteranceId"
                tts.speak(
                    "안녕하세요 findproduct입니다. 화면을 터치해서 사진을 찍으면 음료에 대한 정보를 알려드립니다.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId
                )
            }
        } else {
            // TTS 초기화 실패 시 처리하는 부분을 추가하세요.
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

//    private fun postRequest(json: String) {
//        val client = OkHttpClient()
//
//        val body = RequestBody.create("application/json".toMediaType(), json)
//        val request = Request.Builder()
//            .url("http://114.70.92.44:11000/tJsn/") //도커 post주
//            .post(body)
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("POST", "Request failed: ${e.message}")
//                // 요청 실패 시 에러 메시지를 출력합니다.
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                Log.i("POST", "Request successful")
//                // 요청 성공 시 로그를 출력합니다.
//            }
//        })
//    }
//}

    private fun postRequest(json: String) {
        val client = OkHttpClient()

        val jsonObject = JSONObject(json)
        val requestBody = FormBody.Builder()
            .add("text", jsonObject.getString("text"))
            .build()

        val request = Request.Builder()
            .url("http://114.70.92.44:11000/tForm/")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("POST", "Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i("POST", "Request successful")
            }
        })
    }

}