package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.permissionx.guolindev.PermissionX
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var takePictures: Button
    private lateinit var photos: Button
    private lateinit var isCrop: CheckBox
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView

    companion object {
        const val REQ_PHOTOS = 1
        const val REQ_CAMERA = 2
        const val REQ_CROP = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        takePictures = findViewById(R.id.takePictures)
        photos = findViewById(R.id.photos)
        isCrop = findViewById(R.id.isCrop)
        imageView1 = findViewById(R.id.imageView1)
        imageView2 = findViewById(R.id.imageView2)

        PermissionX.init(this)
            .permissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    toast(this, "All permissions are granted")
                } else {
                    toast(this, "These permissions are denied: $deniedList")
                }
            }

        takePictures.setOnClickListener {
            doCamera()
        }

        photos.setOnClickListener {
            doPhotos()
        }
    }

    /**
     * ??????????????????
     */
    private fun doPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(intent, REQ_PHOTOS)
    }

    lateinit var cameraFile: File

    /**
     * ??????????????????
     */
    private fun doCamera() {
        val intent = Intent().apply {
            action = MediaStore.ACTION_IMAGE_CAPTURE
        }
        val dirName = "IMG"
        val dirFile =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + File.separator + dirName)
        FileUtils.createDirs(dirFile)
        cameraFile = File(dirFile.toString(), DateTimeUtils.now() + ".jpg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // ?????? FileProvider ???????????? Content ????????? Uri ??????
            outputUri =
                FileProvider.getUriForFile(
                    this,
                    AppConfig.getPackageName() + ".provider",
                    cameraFile
                )
        } else {
            outputUri = Uri.fromFile(cameraFile)
        }
        // ?????????????????????????????? Uri ??????????????????
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        // ????????????????????????????????? Uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        startActivityForResult(intent, REQ_CAMERA)
    }

    var outputUri: Uri? = null

    /**
     * ????????????????????????
     */
    private fun doCrop(uri: Uri) {
        val path = getImagePathByUri(uri)
        path?.let {
            val file = File(path)
            doCrop(file)
        }
    }

    private fun doCrop(sourceFile: File) {
        val intent = Intent("com.android.camera.action.CROP")
        val sourceUri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sourceUri = FileProvider.getUriForFile(
                this,
                AppConfig.getPackageName() + ".provider",
                sourceFile
            )
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } else {
            sourceUri = Uri.fromFile(sourceFile)
        }

        val fileName = "CROP_${DateTimeUtils.now()}.${FileUtils.getImageFormat(sourceFile)}"
        val dirName = "CropImage"

        intent.setDataAndType(sourceUri, "image/*")
        intent.putExtra("crop", true.toString()) //????????????
        intent.putExtra("aspectX", 2) //??????????????????
        intent.putExtra("aspectY", 2)
//        intent.putExtra("outputX", 100) //????????????
//        intent.putExtra("outputY", 100)
        intent.putExtra("scale", true) //????????????????????????
        intent.putExtra("scaleUpIfNeeded", true) //??????????????????????????????????????????????????????
        intent.putExtra("return-data", false)

        //????????????????????????????????????
        outputUri = null
        outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //??????Android10??????????????????
            val values = ContentValues()
            // ????????????????????????
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            // ???????????????????????????
            values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + dirName
            )
            // ?????????????????? uri ??????
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        } else {
            val dirFile =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + File.separator + dirName)
            FileUtils.createDirs(dirFile)
            Uri.fromFile(File(dirFile, fileName))
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)

        // ????????????????????????????????????
        intent.putExtra("outputFormat", FileUtils.getImageFormat(sourceFile))
        startActivityForResult(intent, REQ_CROP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_PHOTOS -> {
                data?.data?.let {
                    if (isCrop.isChecked) {
                        //??????
                        doCrop(it)
                    } else {
                        imageView1.setImageURI(it)
                        Glide.with(this).load(getImagePathByUri(it)).into(imageView2)
                    }
                }
            }
            REQ_CAMERA -> {
                //??????????????????
                cameraFile?.let {
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        arrayOf(it.path),
                        null,
                        null
                    )
                }

                if (isCrop.isChecked) {
                    //??????
                    cameraFile?.let {
                        doCrop(it)
                    }
                } else {
                    imageView1.setImageURI(Uri.fromFile(cameraFile))
                    Glide.with(this).load(cameraFile).into(imageView2)
                }
            }
            REQ_CROP -> {
                if (resultCode == RESULT_OK) {
                    outputUri?.let {
                        imageView1.setImageURI(it)
                        Glide.with(this).load(getImagePathByUri(it)).into(imageView2)
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        outputUri?.let {
                            contentResolver.delete(it, null, null)
                        }
                    }
                }
            }
        }
    }

    /**
     * ??????Uri????????????path??????
     */
    private fun getImagePathByUri(uri: Uri): String? {
        var path: String? = null
        val cursor: Cursor? = contentResolver.query(
            uri,
            arrayOf<String>(MediaStore.Images.ImageColumns.DATA),
            null,
            null,
            null
        )
        cursor?.let {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                path = it.getString(index)
            }
            it.close()
        }
        return path
    }
}