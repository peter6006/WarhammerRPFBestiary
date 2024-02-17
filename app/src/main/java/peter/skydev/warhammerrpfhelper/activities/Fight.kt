package peter.skydev.warhammerrpfhelper.activities

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import peter.skydev.warhammerrpfhelper.R
import peter.skydev.warhammerrpfhelper.dataClass.*
import peter.skydev.warhammerrpfhelper.databinding.ActivityFightBinding
import java.io.*
import java.util.*

class Fight : AppCompatActivity() {
    private lateinit var binding: ActivityFightBinding
    private lateinit var bestiaryList: MutableList<BestiaryDataClass>
    private lateinit var traitsList: MutableList<TraitsDataClass>
    private lateinit var skillCardArray: MutableList<SkillDataClass>
    private lateinit var talentCardArray: MutableList<TalentDataClass>
    private lateinit var characterList: MutableList<CharacterDataClass>
    private var characterArray: MutableList<String> = mutableListOf()
    lateinit var selectedCharacters: BooleanArray

    private var enemyInFightList: MutableList<BestiaryDataClass> = mutableListOf()
    private var characterInFightList: MutableList<CharacterDataClass> = mutableListOf()
    private var enemyInFightQuantity = mutableMapOf<BestiaryDataClass, Int>()

    private val fileName = "characters.json"
    private val fileNameBeasts = "beasts.json"
    private val fileNameTraits = "traits.json"
    private val fileNameSkills = "skills.json"
    private val fileNameTalents = "talents.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFightBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportActionBar?.hide()

        MobileAds.initialize(this)
        val mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        val language = Locale.getDefault().language
        if (language == "es" || language == "en") {
            getJsonBestiaryDataFromAsset(this, "bestiary_$language.json")
            getJsonTraitsDataFromAsset(this, "traits_$language.json")
            getJsonSkillDataFromAsset(this, "abilities_$language.json")
            getJsonTalentDataFromAsset(this, "talents_$language.json")
        } else {
            getJsonBestiaryDataFromAsset(this, "bestiary_en.json")
            getJsonTraitsDataFromAsset(this, "traits_en.json")
            getJsonSkillDataFromAsset(this, "abilities_en.json")
            getJsonTalentDataFromAsset(this, "talents_en.json")
        }

        loadCharacters()
        loadBeasts()
        loadTraits()
        loadSkills()
        loadTalents()

        listeners()
        populateListView()
    }

    private fun getJsonBestiaryDataFromAsset(context: Context, fileName: String) {
        var jsonString = ""
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
        val gson = Gson()
        val list = object : TypeToken<List<BestiaryDataClass>>() {}.type
        bestiaryList = gson.fromJson(jsonString, list)
        for (card in bestiaryList){
            enemyInFightQuantity[card] = 0
        }
    }

    private fun getJsonTraitsDataFromAsset(context: Context, fileName: String) {
        var jsonString = ""
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
        val gson = Gson()
        val list = object : TypeToken<List<TraitsDataClass>>() {}.type
        traitsList = gson.fromJson(jsonString, list)
    }

    private fun getJsonSkillDataFromAsset(context: Context, fileName: String) {
        var jsonString = ""
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
        val gson = Gson()
        val list = object : TypeToken<List<SkillDataClass>>() {}.type
        skillCardArray = gson.fromJson(jsonString, list)
    }

    private fun getJsonTalentDataFromAsset(context: Context, fileName: String) {
        var jsonString = ""
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
        val gson = Gson()
        val list = object : TypeToken<List<TalentDataClass>>() {}.type
        talentCardArray = gson.fromJson(jsonString, list)
    }

    private fun loadCharacters() {
        var ret = "[]"
        try {
            val inputStream: InputStream = this.openFileInput(fileName)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                ret = stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: " + e.toString())
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: " + e.toString())
        }
        val gson = Gson()
        val list = object : TypeToken<List<CharacterDataClass>>() {}.type
        characterList = gson.fromJson(ret, list)
        for (character in characterList) {
            characterArray.add(character.name)
        }
        selectedCharacters = BooleanArray(characterArray.size) { false }
    }

    private fun loadBeasts() {
        var ret = "[]"
        try {
            val inputStream: InputStream = this.openFileInput(fileNameBeasts)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                ret = stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: " + e.toString())
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: " + e.toString())
        }
        val gson = Gson()
        val list = object : TypeToken<List<BestiaryDataClass>>() {}.type
        val aux: MutableList<BestiaryDataClass> = gson.fromJson(ret, list)
        for (b in aux) {
            if (!bestiaryList.contains(b)) {
                bestiaryList.add(b)
                enemyInFightQuantity[b] = 0
            }
        }
        bestiaryList.sortBy { it.name }
    }

    private fun loadTraits() {
        var ret = "[]"
        try {
            val inputStream: InputStream = this.openFileInput(fileNameTraits)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                ret = stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: " + e.toString())
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: " + e.toString())
        }
        val gson = Gson()
        val list = object : TypeToken<List<TraitsDataClass>>() {}.type
        val aux: MutableList<TraitsDataClass> = gson.fromJson(ret, list)
        for (t in aux) {
            if (!traitsList.contains(t)) {
                traitsList.add(t)
            }
        }
        traitsList.sortBy { it.name }
    }

    private fun loadSkills() {
        var ret = "[]"
        try {
            val inputStream: InputStream = this.openFileInput(fileNameSkills)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                ret = stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: " + e.toString())
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: " + e.toString())
        }
        val gson = Gson()
        val list = object : TypeToken<List<SkillDataClass>>() {}.type
        val aux: MutableList<SkillDataClass> = gson.fromJson(ret, list)
        for (t in aux) {
            if (!skillCardArray.contains(t)) {
                skillCardArray.add(t)
            }
        }
        skillCardArray.sortBy { it.name }
    }

    private fun loadTalents() {
        var ret = "[]"
        try {
            val inputStream: InputStream = this.openFileInput(fileNameTalents)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                ret = stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: " + e.toString())
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: " + e.toString())
        }
        val gson = Gson()
        val list = object : TypeToken<List<TalentDataClass>>() {}.type
        val aux: MutableList<TalentDataClass> = gson.fromJson(ret, list)
        for (t in aux) {
            if (!talentCardArray.contains(t)) {
                talentCardArray.add(t)
            }
        }
        talentCardArray.sortBy { it.name }
    }

    private fun listeners() {
        binding.addEnemyButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_enemies))
            builder.setCancelable(false)
            var ln = LinearLayout(this)
            ln.orientation = LinearLayout.VERTICAL

            val sv = ScrollView(this)
            for (card in bestiaryList){
                val view = layoutInflater.inflate(R.layout.beast_selection_item, null)


                val beastCB: CheckBox = view.findViewById(R.id.beast_cb)
                val beastTV: TextView = view.findViewById(R.id.beast_tv)
                val beastRestQuantity: FloatingActionButton = view.findViewById(R.id.rest_beast_quantity_fi)
                val beastQuantity: TextView = view.findViewById(R.id.beast_quantity_tv)
                val beastAddQuantity: FloatingActionButton = view.findViewById(R.id.add_beast_quantity_fi)

                val initQ = enemyInFightQuantity[card]!!

                if(enemyInFightQuantity[card]!! > 0){
                    beastCB.isChecked = true
                }

                beastCB.setOnCheckedChangeListener { _, b ->
                    if(b){
                        enemyInFightQuantity[card] = 1
                        beastQuantity.text = enemyInFightQuantity[card]!!.toString()
                    } else {
                        beastQuantity.text = "0"
                        enemyInFightQuantity[card] = 0
                    }
                }

                beastTV.text = card.name
                beastTV.setOnClickListener {
                    beastCB.isChecked = true
                }

                beastRestQuantity.setOnClickListener {
                    if (enemyInFightQuantity[card] != 0){
                        val i = enemyInFightQuantity[card]
                        enemyInFightQuantity[card] = i!! - 1
                        beastQuantity.text =  enemyInFightQuantity[card]!!.toString()

                        if (i - 1 == 0){
                            beastCB.isChecked = false
                        }
                    }
                }
                beastAddQuantity.setOnClickListener {
                    if (enemyInFightQuantity[card] == 0){
                        beastCB.isChecked = true
                    } else {
                        enemyInFightQuantity[card] = enemyInFightQuantity[card]!! + 1
                        beastQuantity.text = enemyInFightQuantity[card]!!.toString()
                    }
                }

                enemyInFightQuantity[card] = initQ
                beastQuantity.text = enemyInFightQuantity[card]!!.toString()

                ln.addView(view)
            }

            sv.addView(ln)

            builder.setView(sv)

            builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                populateListView()
            }

            builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            builder.show()
        }

        binding.addAllyButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_characters))
            builder.setCancelable(false)
            val aux: Array<String> = characterArray.toTypedArray()
            builder.setMultiChoiceItems(aux, selectedCharacters) { _, i, b ->
                if (b) {
                    characterInFightList.add(characterList[i])
                } else {
                    for(card in characterInFightList){
                        if (card.name == aux[i]){
                            characterInFightList.remove(card)
                        }
                    }
                }
            }

            builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                populateListView()
            }

            builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            builder.show()
        }
    }

    private fun populateListView() {
        binding.enemyListScroll.removeAllViews()
        binding.allyListScroll.removeAllViews()

        for (card in enemyInFightQuantity.keys) {
            if (enemyInFightQuantity[card] == 0){
                continue
            } else if(enemyInFightQuantity[card] == 1){
                binding.enemyListScroll.addView(returnBeastView(card))
            } else {
                for (i in 1..enemyInFightQuantity[card]!!){
                    binding.enemyListScroll.addView(returnBeastView(card, " ($i)"))
                }
            }
        }

        for (card in characterInFightList) {
            val view = layoutInflater.inflate(R.layout.character_in_fight_card_item, null)

            //val rlItem: RelativeLayout = view.findViewById(R.id.character_list_activity_card_text_container)
            val tvTitle: TextView = view.findViewById(R.id.character_list_activity_card_title)
            //rlItem.visibility = View.VISIBLE

            val imCharacterImage: ImageView = view.findViewById(R.id.character_image)

            val tvM: TextView = view.findViewById(R.id.movement)
            val tvWS: TextView = view.findViewById(R.id.ws)
            val tvBS: TextView = view.findViewById(R.id.bs)
            val tvS: TextView = view.findViewById(R.id.s)
            val tvT: TextView = view.findViewById(R.id.t)
            val tvI: TextView = view.findViewById(R.id.i)
            val tvAg: TextView = view.findViewById(R.id.ag)
            val tvDex: TextView = view.findViewById(R.id.dex)
            val tvInt: TextView = view.findViewById(R.id.intelligence)
            val tvWP: TextView = view.findViewById(R.id.wp)
            val tvFel: TextView = view.findViewById(R.id.fel)
            val tvW: TextView = view.findViewById(R.id.w)

            tvM.text = card.M.toString()
            tvWS.text = card.WS.toString()
            tvBS.text = card.BS.toString()
            tvS.text = card.S.toString()
            tvT.text = card.T.toString()
            tvI.text = card.I.toString()
            tvAg.text = card.Ag.toString()
            tvDex.text = card.Dex.toString()
            tvInt.text = card.Int.toString()
            tvWP.text = card.WP.toString()
            tvFel.text = card.Fel.toString()
            tvW.text = card.W.toString()

            val tvMAdd: TextInputEditText = view.findViewById(R.id.movement_add)
            tvMAdd.setText("0")
            val tvWSAdd: TextInputEditText = view.findViewById(R.id.ws_add)
            tvWSAdd.setText("0")
            val tvBSAdd: TextInputEditText = view.findViewById(R.id.bs_add)
            tvBSAdd.setText("0")
            val tvSAdd: TextInputEditText = view.findViewById(R.id.s_add)
            tvSAdd.setText("0")
            val tvTAdd: TextInputEditText = view.findViewById(R.id.t_add)
            tvTAdd.setText("0")
            val tvIAdd: TextInputEditText = view.findViewById(R.id.i_add)
            tvIAdd.setText("0")
            val tvAgAdd: TextInputEditText = view.findViewById(R.id.ag_add)
            tvAgAdd.setText("0")
            val tvDexAdd: TextInputEditText = view.findViewById(R.id.dex_add)
            tvDexAdd.setText("0")
            val tvIntAdd: TextInputEditText = view.findViewById(R.id.intelligence_add)
            tvIntAdd.setText("0")
            val tvWPAdd: TextInputEditText = view.findViewById(R.id.wp_add)
            tvWPAdd.setText("0")
            val tvFelAdd: TextInputEditText = view.findViewById(R.id.fel_add)
            tvFelAdd.setText("0")
            val tvWRest: TextInputEditText = view.findViewById(R.id.w_rest)
            tvWRest.setText("0")

            val tvMTotal: TextView = view.findViewById(R.id.movement_total)
            tvMTotal.text = tvM.text.toString()
            tvMAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvMTotal.text = (Integer.parseInt(tvM.text.toString()) + 0).toString()
                    } else {
                        tvMTotal.text = (Integer.parseInt(tvM.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvWSTotal: TextView = view.findViewById(R.id.ws_total)
            tvWSTotal.text = tvWS.text.toString()
            tvWSAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvWSTotal.text = (Integer.parseInt(tvWS.text.toString()) + 0).toString()
                    } else {
                        tvWSTotal.text = (Integer.parseInt(tvWS.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvBSTotal: TextView = view.findViewById(R.id.bs_total)
            tvBSTotal.text = tvBS.text.toString()
            tvBSAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvBSTotal.text = (Integer.parseInt(tvBS.text.toString()) + 0).toString()
                    } else {
                        tvBSTotal.text = (Integer.parseInt(tvBS.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvSTotal: TextView = view.findViewById(R.id.s_total)
            tvSTotal.text = tvS.text.toString()
            tvSAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvSTotal.text = (Integer.parseInt(tvS.text.toString()) + 0).toString()
                    } else {
                        tvSTotal.text = (Integer.parseInt(tvS.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvTTotal: TextView = view.findViewById(R.id.t_total)
            tvTTotal.text = tvT.text.toString()
            tvTAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvTTotal.text = (Integer.parseInt(tvT.text.toString()) + 0).toString()
                    } else {
                        tvTTotal.text = (Integer.parseInt(tvT.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvITotal: TextView = view.findViewById(R.id.i_total)
            tvITotal.text = tvI.text.toString()
            tvIAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvITotal.text = (Integer.parseInt(tvI.text.toString()) + 0).toString()
                    } else {
                        tvITotal.text = (Integer.parseInt(tvI.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvAgTotal: TextView = view.findViewById(R.id.ag_total)
            tvAgTotal.text = tvAg.text.toString()
            tvAgAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvAgTotal.text = (Integer.parseInt(tvAg.text.toString()) + 0).toString()
                    } else {
                        tvAgTotal.text = (Integer.parseInt(tvAg.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvDexTotal: TextView = view.findViewById(R.id.dex_total)
            tvDexTotal.text = tvDex.text.toString()
            tvDexAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvDexTotal.text = (Integer.parseInt(tvDex.text.toString()) + 0).toString()
                    } else {
                        tvDexTotal.text = (Integer.parseInt(tvDex.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvIntTotal: TextView = view.findViewById(R.id.intelligence_total)
            tvIntTotal.text = tvInt.text.toString()
            tvIntAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvIntTotal.text = (Integer.parseInt(tvInt.text.toString()) + 0).toString()
                    } else {
                        tvIntTotal.text = (Integer.parseInt(tvInt.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvWPTotal: TextView = view.findViewById(R.id.wp_total)
            tvWPTotal.text = tvWP.text.toString()
            tvWPAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvWPTotal.text = (Integer.parseInt(tvWP.text.toString()) + 0).toString()
                    } else {
                        tvWPTotal.text = (Integer.parseInt(tvWP.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvFelTotal: TextView = view.findViewById(R.id.fel_total)
            tvFelTotal.text = tvFel.text.toString()
            tvFelAdd.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvFelTotal.text = (Integer.parseInt(tvFel.text.toString()) + 0).toString()
                    } else {
                        tvFelTotal.text = (Integer.parseInt(tvFel.text.toString()) + Integer.parseInt(s.toString())).toString()
                    }
                }
            })
            val tvWTotal: TextView = view.findViewById(R.id.w_total)
            tvWTotal.text = tvW.text.toString()
            tvWRest.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        tvWTotal.text = (Integer.parseInt(tvW.text.toString()) - 0).toString()
                    } else {
                        tvWTotal.text = (Integer.parseInt(tvW.text.toString()) - Integer.parseInt(s.toString())).toString()
                    }
                }
            })

            tvTitle.text = card.name

            if (card.image != null) {
                try {
                    val file = File("/storage/" + card.image.split("/storage/")[1])
                    val uri: Uri = Uri.fromFile(file.absoluteFile)
                    imCharacterImage.setImageURI(uri)
                } catch (e: Exception) {
                    imCharacterImage.setImageResource(R.drawable.ic_baseline_image_not_supported_24)
                }
            }

            val lnSkillsList1: LinearLayout = view.findViewById(R.id.skills_ln1)
            val lnSkillsList2: LinearLayout = view.findViewById(R.id.skills_ln2)
            var i = 0
            for (skill in card.skills) {
                val tv = TextView(this)
                tv.setTextColor(Color.WHITE)
                tv.text = skill
                tv.setOnClickListener {
                    val builder = AlertDialog.Builder(this)
                    var stringToSearch = if (tv.text.toString().contains("(")){
                        tv.text.toString().split(" (")[0]
                    }else {
                        tv.text.toString()
                    }
                    for (skillRaw in skillCardArray) {
                        if (skillRaw.name.split(" (")[0] == stringToSearch){

                            builder.setTitle(resources.getString(R.string.skill_name_completion) + " ${skillRaw.name} (${skillRaw.att})")
                            builder.setMessage(skillRaw.text)
                            builder.setCancelable(true)
                            builder.setPositiveButton(resources.getString(R.string.ok_button)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                    }
                }
                if (i%2 == 0){
                    lnSkillsList1.addView(tv)
                } else {
                    lnSkillsList2.addView(tv)
                }
                i++
            }


            val lnTalentsList1: LinearLayout = view.findViewById(R.id.talents_ln1)
            val lnTalentsList2: LinearLayout = view.findViewById(R.id.talents_ln2)
            for (talent in card.talents) {
                val tv = TextView(this)
                tv.setTextColor(Color.WHITE)
                tv.text = talent
                tv.setOnClickListener {
                    val builder = AlertDialog.Builder(this)
                    var stringToSearch = if (tv.text.toString().contains("(")){
                        tv.text.toString().split(" (")[0]
                    }else {
                        tv.text.toString()
                    }
                    for (talentRaw in talentCardArray) {
                        if (talentRaw.name.split(" (")[0] == stringToSearch){
                            builder.setTitle(resources.getString(R.string.talent_name_completion) + " ${talentRaw.name} (${talentRaw.att})")
                            builder.setMessage("Max: ${talentRaw.max_val}\nTests: ${talentRaw.tests}\n\n${talentRaw.description}")
                            builder.setCancelable(true)
                            builder.setPositiveButton(resources.getString(R.string.ok_button)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                    }
                }
                if (i%2 == 0){
                    lnTalentsList1.addView(tv)
                } else {
                    lnTalentsList2.addView(tv)
                }
                i++
            }


            binding.allyListScroll.addView(view)
        }
    }

    private fun returnBeastView(card: BestiaryDataClass, nameAddition: String = ""): View{
        val view = layoutInflater.inflate(R.layout.beast_in_fight_card_item, null)

        val tvTitle: TextView = view.findViewById(R.id.beast_list_activity_card_title)
        tvTitle.text = card.name + nameAddition

        val imBeastImage: ImageView = view.findViewById(R.id.beast_image)
        if (card.image != null) {
            try {
                val file = File("/storage/" + card.image.split("/storage/")[1])
                val uri: Uri = Uri.fromFile(file.absoluteFile)
                imBeastImage.setImageURI(uri)
            } catch (e: Exception) {
                imBeastImage.setImageResource(R.drawable.ic_baseline_image_not_supported_24)
            }
        }

        val tvM: TextView = view.findViewById(R.id.movement)
        val tvWS: TextView = view.findViewById(R.id.ws)
        val tvBS: TextView = view.findViewById(R.id.bs)
        val tvS: TextView = view.findViewById(R.id.s)
        val tvT: TextView = view.findViewById(R.id.t)
        val tvI: TextView = view.findViewById(R.id.i)
        val tvAg: TextView = view.findViewById(R.id.ag)
        val tvDex: TextView = view.findViewById(R.id.dex)
        val tvInt: TextView = view.findViewById(R.id.intelligence)
        val tvWP: TextView = view.findViewById(R.id.wp)
        val tvFel: TextView = view.findViewById(R.id.fel)
        val tvW: TextView = view.findViewById(R.id.w)

        tvM.text = card.M.toString()
        tvWS.text = card.WS.toString()
        tvBS.text = card.BS.toString()
        tvS.text = card.S.toString()
        tvT.text = card.T.toString()
        tvI.text = card.I.toString()
        tvAg.text = card.Ag.toString()
        tvDex.text = card.Dex.toString()
        tvInt.text = card.Int.toString()
        tvWP.text = card.WP.toString()
        tvFel.text = card.Fel.toString()
        tvW.text = card.W.toString()

        val tvMAdd: TextInputEditText = view.findViewById(R.id.movement_add)
        tvMAdd.setText("0")
        val tvWSAdd: TextInputEditText = view.findViewById(R.id.ws_add)
        tvWSAdd.setText("0")
        val tvBSAdd: TextInputEditText = view.findViewById(R.id.bs_add)
        tvBSAdd.setText("0")
        val tvSAdd: TextInputEditText = view.findViewById(R.id.s_add)
        tvSAdd.setText("0")
        val tvTAdd: TextInputEditText = view.findViewById(R.id.t_add)
        tvTAdd.setText("0")
        val tvIAdd: TextInputEditText = view.findViewById(R.id.i_add)
        tvIAdd.setText("0")
        val tvAgAdd: TextInputEditText = view.findViewById(R.id.ag_add)
        tvAgAdd.setText("0")
        val tvDexAdd: TextInputEditText = view.findViewById(R.id.dex_add)
        tvDexAdd.setText("0")
        val tvIntAdd: TextInputEditText = view.findViewById(R.id.intelligence_add)
        tvIntAdd.setText("0")
        val tvWPAdd: TextInputEditText = view.findViewById(R.id.wp_add)
        tvWPAdd.setText("0")
        val tvFelAdd: TextInputEditText = view.findViewById(R.id.fel_add)
        tvFelAdd.setText("0")
        val tvWRest: TextInputEditText = view.findViewById(R.id.w_rest)
        tvWRest.setText("0")

        val tvMTotal: TextView = view.findViewById(R.id.movement_total)
        tvMTotal.text = tvM.text.toString()
        tvMAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvMTotal.text = (Integer.parseInt(tvM.text.toString()) + 0).toString()
                } else {
                    tvMTotal.text = (Integer.parseInt(tvM.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvWSTotal: TextView = view.findViewById(R.id.ws_total)
        tvWSTotal.text = tvWS.text.toString()
        tvWSAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvWSTotal.text = (Integer.parseInt(tvWS.text.toString()) + 0).toString()
                } else {
                    tvWSTotal.text = (Integer.parseInt(tvWS.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvBSTotal: TextView = view.findViewById(R.id.bs_total)
        tvBSTotal.text = tvBS.text.toString()
        tvBSAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvBSTotal.text = (Integer.parseInt(tvBS.text.toString()) + 0).toString()
                } else {
                    tvBSTotal.text = (Integer.parseInt(tvBS.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvSTotal: TextView = view.findViewById(R.id.s_total)
        tvSTotal.text = tvS.text.toString()
        tvSAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvSTotal.text = (Integer.parseInt(tvS.text.toString()) + 0).toString()
                } else {
                    tvSTotal.text = (Integer.parseInt(tvS.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvTTotal: TextView = view.findViewById(R.id.t_total)
        tvTTotal.text = tvT.text.toString()
        tvTAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvTTotal.text = (Integer.parseInt(tvT.text.toString()) + 0).toString()
                } else {
                    tvTTotal.text = (Integer.parseInt(tvT.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvITotal: TextView = view.findViewById(R.id.i_total)
        tvITotal.text = tvI.text.toString()
        tvIAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvITotal.text = (Integer.parseInt(tvI.text.toString()) + 0).toString()
                } else {
                    tvITotal.text = (Integer.parseInt(tvI.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvAgTotal: TextView = view.findViewById(R.id.ag_total)
        tvAgTotal.text = tvAg.text.toString()
        tvAgAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvAgTotal.text = (Integer.parseInt(tvAg.text.toString()) + 0).toString()
                } else {
                    tvAgTotal.text = (Integer.parseInt(tvAg.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvDexTotal: TextView = view.findViewById(R.id.dex_total)
        tvDexTotal.text = tvDex.text.toString()
        tvDexAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvDexTotal.text = (Integer.parseInt(tvDex.text.toString()) + 0).toString()
                } else {
                    tvDexTotal.text = (Integer.parseInt(tvDex.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvIntTotal: TextView = view.findViewById(R.id.intelligence_total)
        tvIntTotal.text = tvInt.text.toString()
        tvIntAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvIntTotal.text = (Integer.parseInt(tvInt.text.toString()) + 0).toString()
                } else {
                    tvIntTotal.text = (Integer.parseInt(tvInt.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvWPTotal: TextView = view.findViewById(R.id.wp_total)
        tvWPTotal.text = tvWP.text.toString()
        tvWPAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvWPTotal.text = (Integer.parseInt(tvWP.text.toString()) + 0).toString()
                } else {
                    tvWPTotal.text = (Integer.parseInt(tvWP.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvFelTotal: TextView = view.findViewById(R.id.fel_total)
        tvFelTotal.text = tvFel.text.toString()
        tvFelAdd.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvFelTotal.text = (Integer.parseInt(tvFel.text.toString()) + 0).toString()
                } else {
                    tvFelTotal.text = (Integer.parseInt(tvFel.text.toString()) + Integer.parseInt(s.toString())).toString()
                }
            }
        })
        val tvWTotal: TextView = view.findViewById(R.id.w_total)
        tvWTotal.text = tvW.text.toString()
        tvWRest.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    tvWTotal.text = (Integer.parseInt(tvW.text.toString()) - 0).toString()
                } else {
                    tvWTotal.text = (Integer.parseInt(tvW.text.toString()) - Integer.parseInt(s.toString())).toString()
                }
            }
        })

        val lnMTraitsList1: LinearLayout = view.findViewById(R.id.m_traits_ln1)
        val lnMTraitsList2: LinearLayout = view.findViewById(R.id.m_traits_ln2)
        var i = 0
        for (mTrait in card.mandatory_traits) {
            val tv = TextView(this)
            tv.setTextColor(Color.WHITE)
            tv.text = mTrait
            tv.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                val stringToSearch = if (tv.text.toString().contains("(")){
                    tv.text.toString().split(" (")[0]
                }else {
                    tv.text.toString()
                }
                for (traitRaw in traitsList) {
                    if (traitRaw.name.split(" (")[0] == stringToSearch){
                        builder.setTitle(resources.getString(R.string.trait_name_completion) + " ${traitRaw.name}")
                        builder.setMessage(traitRaw.description)
                        builder.setCancelable(true)
                        builder.setPositiveButton(resources.getString(R.string.ok_button)) { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                        builder.show()
                    }
                }
            }
            if (i%2 == 0){
                lnMTraitsList1.addView(tv)
            } else {
                lnMTraitsList2.addView(tv)
            }
            i++
        }

        val lnOTraitsList1: LinearLayout = view.findViewById(R.id.o_traits_ln1)
        val lnOTraitsList2: LinearLayout = view.findViewById(R.id.o_traits_ln2)
        for (mTrait in card.optional_traits) {
            val tv = TextView(this)
            tv.setTextColor(Color.WHITE)
            tv.text = mTrait
            tv.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                val stringToSearch = if (tv.text.toString().contains("(")){
                    tv.text.toString().split(" (")[0]
                }else {
                    tv.text.toString()
                }
                for (traitRaw in traitsList) {
                    if (traitRaw.name.split(" (")[0] == stringToSearch){
                        builder.setTitle(resources.getString(R.string.trait_name_completion) + " ${traitRaw.name}")
                        builder.setMessage(traitRaw.description)
                        builder.setCancelable(true)
                        builder.setPositiveButton(resources.getString(R.string.ok_button)) { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                        builder.show()
                    }
                }
            }
            if (i%2 == 0){
                lnOTraitsList1.addView(tv)
            } else {
                lnOTraitsList2.addView(tv)
            }
            i++
        }

        return view
    }
}