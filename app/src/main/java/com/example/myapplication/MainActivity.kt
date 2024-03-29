package com.example.myapplication

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam
import com.permissionx.guolindev.callback.ForwardToSettingsCallback
import com.permissionx.guolindev.request.ExplainScope
import com.permissionx.guolindev.request.ForwardScope
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var takePictures: Button
    private lateinit var photos: Button
    private lateinit var isCrop: CheckBox
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var allAlbum: Button

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
        allAlbum = findViewById(R.id.allAlbum)

        PermissionX.init(this)
            .permissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            )
            .onExplainRequestReason { scope, deniedList, beforeRequest ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "应用需要以下权限才能继续",
                    "允许",
                    "拒绝"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "请在设置中允许以下权限",
                    "允许",
                    "拒绝"
                )
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    toast(this, "获取所有权限")
                    Log.e("TAG", "All permissions are granted")
                } else {
                    toast(this, "权限获取失败: $deniedList")
                    Log.e("TAG", "These permissions are denied: $deniedList")
                }
            }

        takePictures.setOnClickListener {
            doCamera()
        }

        photos.setOnClickListener {
            doPhotos()
        }

        allAlbum.setOnClickListener {
            seeAllAlbum()
        }
    }

    fun open(v: View) {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivityForResult(intent, 333)
    }

    /**
     * 打开系统相册
     */
    private fun doPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(intent, REQ_PHOTOS)
    }

    lateinit var cameraFile: File

    /**
     * 打开系统相机
     */
    private fun doCamera() {
        val intent = Intent().apply {
            action = MediaStore.ACTION_IMAGE_CAPTURE
        }
        val directoryName = "IMG"
        val directory =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + File.separator + directoryName)
        FileUtils.createDirs(directory)
        cameraFile = File(directory, DateTimeUtils.now() + ".jpg")
        outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 通过 FileProvider 创建一个 Content 类型的 Uri 文件
            FileProvider.getUriForFile(
                this,
                AppConfig.getPackageName() + ".provider",
                cameraFile
            )
        } else {
            Uri.fromFile(cameraFile)
        }
        // 对目标应用临时授权该 Uri 所代表的文件
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        // 将拍取的照片保存到指定 Uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        startActivityForResult(intent, REQ_CAMERA)
    }

    var outputUri: Uri? = null

    /**
     * 调用系统剪裁工具
     */
    private fun doCrop(uri: Uri) {
        val path: String? = getImagePathByUri(uri)
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
        intent.putExtra("crop", "true") //是否剪裁
        intent.putExtra("aspectX", 2) //剪裁宽高比例
        intent.putExtra("aspectY", 2)
//        intent.putExtra("outputX", 100) //剪裁大小
//        intent.putExtra("outputY", 100)
        intent.putExtra("scale", true) //是否保持比例不变
        intent.putExtra("scaleUpIfNeeded", true) //裁剪区域小于输出大小时，是否放大图像
        intent.putExtra("return-data", false)

        //设置剪裁后保存的文件路径
        outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //适配Android10分区存储特性
            val values = ContentValues().apply {
                // 设置显示的文件名
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                // 设置输出的路径信息
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + dirName
                )
            }
            // 生成一个新的 uri 路径
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            val dirFile =
//                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + File.separator + dirName)
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        .toString() + File.separator + dirName
                )
            FileUtils.createDirs(dirFile)
            Uri.fromFile(File(dirFile, fileName))
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)

        // 设置裁剪后保存的文件格式
        intent.putExtra("outputFormat", FileUtils.getImageFormat(sourceFile))
        startActivityForResult(intent, REQ_CROP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e("TAG", "requestCode:$requestCode resultCode:$resultCode")
        when (requestCode) {
            REQ_PHOTOS -> {
                data?.data?.let {
                    if (isCrop.isChecked) {
                        //剪裁
                        doCrop(it)
                    } else {
                        imageView1.setImageURI(it)
                        Glide.with(this).load(getImagePathByUri(it)).into(imageView2)
                    }
                }
            }
            REQ_CAMERA -> {
                //刷新系统相册
                cameraFile.let {
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        arrayOf(it.path),
                        null,
                        null
                    )
                }

                if (isCrop.isChecked) {
                    //剪裁
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
     * 通过Uri获取图片path路径
     */
    private fun getImagePathByUri(uri: Uri?): String? {
        var path: String? = null
        if (uri == null) return path

        val scheme = uri.scheme
        if (scheme == null) {
            path = uri.path
        } else if (scheme == ContentResolver.SCHEME_FILE) {
            path = uri.path
        } else if (scheme == ContentResolver.SCHEME_CONTENT) {
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
        }

        return path
    }

    /**
     * 查看所有相册
     */
    private fun seeAllAlbum() {

    }
}