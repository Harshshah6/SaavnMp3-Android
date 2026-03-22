package com.harsh.shah.saavnmp3.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.BuildConfig
import com.harsh.shah.saavnmp3.databinding.ActivityAboutBinding
import com.harsh.shah.saavnmp3.model.aboutus.Contributors
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.network.utility.RequestNetworkController
import com.harsh.shah.saavnmp3.utils.customview.BottomSheetItemView

class AboutActivity : AppCompatActivity() {
    var binding: ActivityAboutBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        setSupportActionBar(binding!!.toolbar)
        binding!!.toolbar.setNavigationOnClickListener { _: android.view.View? -> finish() }

        binding!!.versionTxt.titleTextView?.text = BuildConfig.VERSION_NAME

        binding!!.email.setOnClickListener {
            openUrl(
                "mailto:harshsandeep23@gmail.com"
            )
        }
        binding!!.sourceCode.setOnClickListener {
            openUrl(
                "https://github.com/harshshah6/SaavnMp3-Android"
            )
        }
        binding!!.discord.setOnClickListener {
            Toast.makeText(
                this@AboutActivity,
                "Oops, No Discord Server found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding!!.instagram.setOnClickListener {
            openUrl(
                "https://www.instagram.com/harsh_.s._shah/"
            )
        }
        binding!!.telegram.setOnClickListener {
            openUrl(
                "https://t.me/legendary_streamer_official"
            )
        }
        binding!!.rate.setOnClickListener {
            openUrl(
                "https://github.com/harshshah6/SaavnMp3-Android"
            )
        }

        RequestNetwork(this).startRequestNetwork(
            RequestNetworkController.GET,
            "https://androsketchui.vercel.app/api/github/harshshah6/saavnmp3-android/contributors",
            "",
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: java.util.HashMap<String?, Any?>?
                ) {
//                final BottomSheetItemView bottomSheetItemView = new BottomSheetItemView(AboutActivity.this, "Harsh Shah", "https://avatars.githubusercontent.com/u/69447184?v=4", "");
//                bottomSheetItemView.setOnClickListener(view -> openUrl("https://github.com/harshshah6"));
//                binding.layoutContributors.addView(bottomSheetItemView);
                    val contributors: Contributors =
                        Gson().fromJson(response, Contributors::class.java)
                    android.util.Log.i("AboutActivity", "contributors: $contributors")
                    val list = contributors.contributors
                    if (list != null) {
                        for (contributor in list) {
                            if (contributor == null) continue
                            val item = BottomSheetItemView(
                                this@AboutActivity,
                                contributor.login ?: "",
                                contributor.avatar_url ?: "",
                                ""
                            )
                            item.setOnClickListener { openUrl(contributor.html_url) }
                            binding!!.layoutContributors.addView(item)
                        }
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                }
            })
    }

    private fun openUrl(url: String?) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_VIEW
        sendIntent.data = url?.toUri()
        startActivity(sendIntent)
    }
}
