package com.example.text2Img

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso

class FullscreenImageActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)
        
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        val fullscreenImageView = findViewById<ImageView>(R.id.fullscreenImageView)
        
        // 设置全屏模式
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        
        // 加载图片
        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(imageUrl)
                .into(fullscreenImageView)
        }
        
        // 点击图片返回
        fullscreenImageView.setOnClickListener {
            finish()
        }
    }
} 