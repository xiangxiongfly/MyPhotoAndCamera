package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.Toast


fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

fun log(msg: String) = Log.e("TAG", msg)