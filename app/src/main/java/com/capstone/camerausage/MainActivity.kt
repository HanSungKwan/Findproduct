package com.capstone.camerausage

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capstone.camerausage.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import androidx.camera.core.ImageCaptureException
import androidx.core.net.toFile
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Vars for Photos
    /// Var for Camera vision preview
    private lateinit var viewBinding: ActivityMainBinding

    /// Var for image capture instance
    private var imageCapture: ImageCapture? = null
//    private var videoCapture: VideoCapture<Recorder>? = null
//    private var recording: Recording? = null

    /// Var for assign executor to image capture
    private lateinit var cameraExecutor: ExecutorService
    //
    // Vars for Queries
    /// Var for query instance
//    private var resultQuery: Cursor? = null
    /// Var for query to projection(WHERE col, col, col, ...)
    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN
    )
    /// Var for query to sort(ORDER BY threshold desc)
    private val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    /// Var for query result
    private var id: Long? = null
    private var dateTaken: Date? = null
    private var displayName: String? = null
    private var contentUri: Uri? = null
    private var resultFile: File? = null
    //
    // Vars for Http Communication
    private val targetUri:String = "http://114.70.92.44:11000/ttPost/"
    private val client:OkHttpClient by lazy { OkHttpClient() }
    private val requestBody: MultipartBody.Builder by lazy { MultipartBody.Builder() }
    private val request: Request.Builder by lazy { Request.Builder() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //카메라 권한 여부 검사
        if(allPermissionsGranted()) {
            // 모든 권한이 있을 때
            startCamera()
        } else {
            // 권한이 없을 때
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 카메라 기능을 위한 리스너들 ui와 연동
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
//        viewBinding.imageCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

//                // Bind use cases to camera
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview)
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector)
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed, 바인딩 실패", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FindProduct")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
        QueryAndPost(
            projection, null, null, sortOrder,
            client, null, requestBody, request
        )
    }
//    private fun captureVideo() {}

    // Function: Query and Post a picture to server
    private fun QueryAndPost(projection: Array<String>,
                             selection: String?,
                             selectionArgs: Array<String>?,
                             sortOrder: String?,
//                             targetResults: Array<String>,
                             client: OkHttpClient,
                             requestBody: RequestBody?,
                             multiPartBody:MultipartBody.Builder?,
                             request:Request.Builder
    ) {

        val resultQuery = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        resultQuery?.use {cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Images.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Images.Media.DATE_TAKEN)
            val displayNameColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Images.Media.DISPLAY_NAME)

            cursor.moveToFirst()
            id = cursor.getLong(idColumn)
            dateTaken = Date(cursor.getLong(dateTakenColumn))
            displayName = cursor.getString(displayNameColumn)
            contentUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toString()
            )
            Toast.makeText(baseContext, "qry sux${ displayName.toString() }", Toast.LENGTH_SHORT).show()
        }
        // FileProvider 또는 Files에contentResolver.openInputStream(
        ////            MediaStore.setRequireOriginal(contentUri))?.use {
        ////        } 사용하기
//        multiPartBody.let{
//            it.setType(MultipartBody.FORM)
//            it.addFormDataPart("inFile", displayName)
//        }
//        val imageByteArr =
//            contentUri?.let { MediaStore.setRequireOriginal(it) }?.let {
//                contentResolver.openTypedAssetFileDescriptor(
//                    it,
//                    "image/jpeg",
//                    null
//                ).use { assFDescriptor ->
//                    assFDescriptor?.createInputStream().readAllBytes() ?: null
//                }
//            }
        val target = contentUri?.let { uri ->
            contentResolver.openTypedAssetFileDescriptor(
                uri,
                "image/jpeg",
                null
            )
        }?.let {
            it.use{descriptor ->
                descriptor.createInputStream().readBytes()
            }
        }

        val mltPartBody = multiPartBody?.let { bBuilder->
            target?.let{ fileBytesArr ->
                displayName?.let { filename->
//                    val tFile: File = File.createTempFile(filename, "")
                    bBuilder.setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "tFile",
                            filename,
//                            tFile.asRequestBody("image/jpeg".toMediaType())
                            fileBytesArr.toRequestBody("image/jpeg".toMediaType())
                    )
                    .build()
                }
            }
        }

        val reQuest = request?.let {
            mltPartBody?.let { mltPBody ->
                it.url(targetUri)
                .post(mltPBody)
                .build()
            }
        }

        reQuest?.let {
            displayName?.let{
            client.newCall(reQuest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
//                    Toast.makeText(
//                        baseContext,
//                        "Failed! $displayName",
//                        Toast.LENGTH_SHORT
//                    )
//                        .show()
                    Log.e("POST", "Request failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
//                    Toast.makeText(
//                        baseContext,
//                        "Success! $displayName",
//                        Toast.LENGTH_SHORT
//                    )
//                        .show()
                    Log.i("POST", "Request successful $displayName")
                }
            })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraUsage"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}