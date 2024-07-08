package peter.skydev.warhammerrpfhelper

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import peter.skydev.warhammerrpfhelper.activities.Custom
import peter.skydev.warhammerrpfhelper.activities.*
import peter.skydev.warhammerrpfhelper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnUserEarnedRewardListener {
    private lateinit var binding: ActivityMainBinding
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private val sharedPrefFile = "warhammerrpf"
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportActionBar?.hide()
        sharedPreferences = this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)

        val time = System.currentTimeMillis()

        if (time > 1722474000000) {
            MobileAds.initialize(this) { initializationStatus ->
                val timeSaved = sharedPreferences.getLong("ad_time", 0L)
                val actualTime = System.currentTimeMillis()
                if (actualTime - timeSaved > 12 * 60 * 60 * 1000) {
                    loadAd()
                }
            }
        }

        binding.buttonFight.setOnClickListener {
            val intent = Intent(this, Fight::class.java)
            startActivity(intent)
        }

        binding.buttonBestiary.setOnClickListener {
            val intent = Intent(this, Bestiary::class.java)
            startActivity(intent)
        }

        binding.buttonCharacter.setOnClickListener {
            val intent = Intent(this, Character::class.java)
            startActivity(intent)
        }

        binding.buttonCustom.setOnClickListener {
            val intent = Intent(this, Custom::class.java)
            startActivity(intent)
        }

        binding.buttonAbilitiesAndTalents.setOnClickListener {
            val intent = Intent(this, AbilitiesAndTalents::class.java)
            startActivity(intent)
        }
    }

    private fun loadAd() {
        RewardedInterstitialAd.load(this, "ca-app-pub-4855450974262250/1226717847", // Real: ca-app-pub-4855450974262250/1226717847 -- Test: ca-app-pub-3940256099942544/5354046379
            AdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    showMessage()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedInterstitialAd = null
                }
            })
    }

    private fun showMessage() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.ad_video_message_title))
        builder.setMessage(resources.getString(R.string.ad_video_message_message))
        builder.setCancelable(true)
        builder.setPositiveButton(resources.getString(R.string.ad_video_message_button_ok)) { _, _ ->
            rewardedInterstitialAd?.show(this, this)
        }
        builder.setNegativeButton(resources.getString(R.string.ad_video_message_button_cancel)) { _, _ ->
        }
        builder.setNeutralButton(resources.getString(R.string.ad_video_message_button_no_more)) { _, _ ->
            val time = System.currentTimeMillis()
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putLong("ad_time", time)
            editor.apply()
            editor.commit()
        }
        builder.show()
    }

    override fun onUserEarnedReward(p0: RewardItem) {
        Toast.makeText(this, resources.getString(R.string.ad_video_message_thank_you), Toast.LENGTH_LONG).show()
        val time = System.currentTimeMillis()
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putLong("ad_time", time)
        editor.apply()
        editor.commit()
    }
}