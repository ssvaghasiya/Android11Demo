package com.example.demoa11

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.URL
import kotlin.concurrent.thread


const val PICK_FILE = 1
const val RESULT_LOAD_IMG = 10
const val ALL_FILES_ACCESS_PERMISSION = 4

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        pickFile.setOnClickListener {
            pickFileAndCopyUriToExternalFilesDir()
        }
        pickImage.setOnClickListener {
            pickImage()
        }
        manageExternalStorage.setOnClickListener {
            requestAllFilesAccessPermission()
        }
        downloadFromUrl.setOnClickListener {
            val url =
                "https://st.depositphotos.com/1428083/2946/i/950/depositphotos_29460297-stock-photo-bird-cage.jpg"
            var fileName = getFileNameFromURL(url)
            downloadFile1(url, fileName)
        }
    }

    private fun checkPermission(): Boolean {
        val permissionsToRequire = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequire.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequire.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionsToRequire.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequire.toTypedArray(), 0)
            return true
        }
        return false
    }

    private fun pickFileAndCopyUriToExternalFilesDir() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, PICK_FILE)
    }

    private fun pickImage() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG)
    }

    private fun requestAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
            Toast.makeText(
                this,
                "We can access all files on external storage now",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val builder = AlertDialog.Builder(this)
                .setTitle("Tip")
                .setMessage("We need permission to access all files on external storage")
                .setPositiveButton("OK") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, ALL_FILES_ACCESS_PERMISSION)
                }
                .setNegativeButton("Cancel", null)
            builder.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "You must allow all the permissions.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_FILE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        val fileName = getFileNameByUri(uri)
                        copyUriToExternalFilesDir1(uri, fileName)
                    }
                }
            }
            RESULT_LOAD_IMG -> {
                try {
                    if (data != null) {
                        val imageUri: Uri = data.data!!
                        val imageStream: InputStream? = contentResolver.openInputStream(imageUri)
                        val selectedImage = BitmapFactory.decodeStream(imageStream)
                        img.setImageBitmap(selectedImage)
                        val fileName = getFileNameByUri(imageUri)
                        createDirectoryAndSaveFile(selectedImage, fileName)
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
                }
            }
            ALL_FILES_ACCESS_PERMISSION -> {
                requestAllFilesAccessPermission()
            }
        }
    }

    private fun getFileNameByUri(uri: Uri): String {
        var fileName = System.currentTimeMillis().toString()
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            fileName =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            cursor.close()
        }
        return fileName
    }

    private fun copyUriToExternalFilesDir(uri: Uri, fileName: String) {
        thread {
            val inputStream = contentResolver.openInputStream(uri)
            val tempDir = getExternalFilesDir("temp")
            if (inputStream != null && tempDir != null) {
                val file = File("$tempDir/$fileName")
                val fos = FileOutputStream(file)
                val bis = BufferedInputStream(inputStream)
                val bos = BufferedOutputStream(fos)
                val byteArray = ByteArray(1024)
                var bytes = bis.read(byteArray)
                while (bytes > 0) {
                    bos.write(byteArray, 0, bytes)
                    bos.flush()
                    bytes = bis.read(byteArray)
                }
                bos.close()
                fos.close()
                runOnUiThread {
                    Toast.makeText(this, "Copy file into $tempDir succeeded.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun createDirectoryAndSaveFile(
        imageToSave: Bitmap,
        fileName: String
    ) {
        val direct =
            File(Environment.getExternalStorageDirectory().toString() + "/DirName1")
        if (!direct.exists()) {
            val wallpaperDirectory = File("/sdcard/DirName1/")
            wallpaperDirectory.mkdirs()
        }
        val file = File("/sdcard/DirName1/", fileName)
        if (file.exists()) {
            file.delete()
        }
        try {
            val out = FileOutputStream(file)
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyUriToExternalFilesDir1(uri: Uri, fileName: String) {
        thread {
            val inputStream = contentResolver.openInputStream(uri)
            val direct =
                File(Environment.getExternalStorageDirectory().toString() + "/DirName1")
            if (!direct.exists()) {
                val wallpaperDirectory = File("/sdcard/DirName1/")
                wallpaperDirectory.mkdirs()
            }
            val file = File("/sdcard/DirName1/", fileName)
            if (file.exists()) {
                file.delete()
            }
            try {
                val fos = FileOutputStream(file)
                val bis = BufferedInputStream(inputStream)
                val bos = BufferedOutputStream(fos)
                val byteArray = ByteArray(1024)
                var bytes = bis.read(byteArray)
                while (bytes > 0) {
                    bos.write(byteArray, 0, bytes)
                    bos.flush()
                    bytes = bis.read(byteArray)
                }
                bos.close()
                fos.close()
                runOnUiThread {
                    Toast.makeText(this, "Copy file into $direct succeeded.", Toast.LENGTH_LONG)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun downloadFile1(url: String?, fileName: String) {
        val direct =
            File(Environment.getExternalStorageDirectory().toString() + "/DirName1")
        if (!direct.exists()) {
            val wallpaperDirectory = File("/sdcard/DirName1/")
            wallpaperDirectory.mkdirs()
        }
        val file = File("/sdcard/DirName1/", fileName)
        if (file.exists()) {
            file.delete()
        }

        val thread = Thread(Runnable {
            try {
                try {
                    BufferedInputStream(
                        URL(url).openStream()
                    ).use { inputStream ->
                        FileOutputStream(file).use { fileOS ->
                            val data = ByteArray(1024)
                            var byteContent: Int
                            while (inputStream.read(data, 0, 1024)
                                    .also { byteContent = it } != -1
                            ) {
                                fileOS.write(data, 0, byteContent)
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        })
        thread.start();

    }

    fun getFileNameFromURL(downloadUrl: String): String {
        val url =
            URL(downloadUrl)
        val fileName = url.file
        return fileName.substring(fileName.lastIndexOf('/') + 1)
    }
}