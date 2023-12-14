//package com.be.aiprac.image_generation
//
//import android.content.Intent
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import com.be.aiprac.databinding.ActivityImgGenBinding
//import com.be.aiprac.image_generation.diffusion.DiffusionActivity
//import com.be.aiprac.image_generation.loraweights.LoRAWeightActivity
//import com.be.aiprac.image_generation.plugins.PluginActivity
//
//class ImgGenActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityImgGenBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityImgGenBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.btnDiffusion.setOnClickListener {
//            startActivity(Intent(this, DiffusionActivity::class.java))
//        }
//
//        binding.btnPlugins.setOnClickListener {
//            startActivity(Intent(this, PluginActivity::class.java))
//        }
//
//        binding.btnLoRA.setOnClickListener {
//            startActivity(Intent(this, LoRAWeightActivity::class.java))
//        }
//    }
//
//}