package peter.skydev.warhammerrpfhelper.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import peter.skydev.warhammerrpfhelper.R
import peter.skydev.warhammerrpfhelper.dataClass.CharacterDataClass
import peter.skydev.warhammerrpfhelper.dataClass.SkillDataClass
import peter.skydev.warhammerrpfhelper.dataClass.TalentDataClass
import peter.skydev.warhammerrpfhelper.databinding.ActivityCharacterBinding
import java.io.*
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList


class Character : AppCompatActivity() {
    private lateinit var binding: ActivityCharacterBinding
    private val fileName = "characters.json"
    private val fileNameSkills = "skills.json"
    private val fileNameTalents = "talents.json"
    private lateinit var characterList: MutableList<CharacterDataClass>
    private var expandedView: View? = null
    private var skillArray: MutableList<String> = mutableListOf()
    private var skillCardArray: MutableList<SkillDataClass> = mutableListOf()
    private var skillCardArrayNoEdit: MutableList<SkillDataClass> = mutableListOf()
    lateinit var selectedSkill: BooleanArray
    var skillList: ArrayList<Int> = ArrayList()

    private var pathToTheImage = ""
    private lateinit var characterImageAux: ImageView

    private var talentArray: MutableList<String> = mutableListOf()
    private var talentArrayNoEdit: MutableList<TalentDataClass> = mutableListOf()
    lateinit var selectedTalent: BooleanArray
    var talentList: ArrayList<Int> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportActionBar?.hide()

        MobileAds.initialize(this)
        val mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        listeners()
        populateListView()

        val language = Locale.getDefault().language
        if (language == "es" || language == "en") {
            getJsonSkillDataFromAsset(this, "abilities_$language.json")
            getJsonTalentDataFromAsset(this, "talents_$language.json")
        } else {
            getJsonSkillDataFromAsset(this, "abilities_en.json")
            getJsonTalentDataFromAsset(this, "talents_en.json")
        }

        loadCharacters()
        loadSkills()
        loadTalents()
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
        skillCardArrayNoEdit = gson.fromJson(jsonString, list)
        for (skill in skillCardArray) {
            if (skill.name !in skillArray){
                skillArray.add(skill.name)
            }
        }
        selectedSkill = BooleanArray(skillArray.size) { false }
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
        val aux: MutableList<TalentDataClass> = gson.fromJson(jsonString, list)
        talentArrayNoEdit = gson.fromJson(jsonString, list)
        for (talent in aux) {
            if (talent.name !in talentArray) {
                talentArray.add(talent.name)
            }
        }
        selectedTalent = BooleanArray(talentArray.size) { false }
    }

    private fun populateListView() {
        binding.listLayout.removeAllViews()
        binding.progressBar.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            val auxList = characterList
            for (card: CharacterDataClass in auxList) {
                val view = layoutInflater.inflate(R.layout.character_card_item, null)

                val characterListActivityRelativeLayout: RelativeLayout =
                    view.findViewById(R.id.character_list_activity_card_text_container)
                val tvTitle: TextView = view.findViewById(R.id.character_list_activity_card_title)
                val tvDescription: TextView = view.findViewById(R.id.character_history_tv)
                val imArrow: ImageView = view.findViewById(R.id.character_list_activity_card_arrow)
                val imDelete: ImageView = view.findViewById(R.id.character_list_activity_card_delete)
                val imEdit: ImageView = view.findViewById(R.id.character_list_activity_card_edit)

                imDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(resources.getString(R.string.delete_a_character))
                    builder.setMessage(resources.getString(R.string.delete_a_character_msg))
                    builder.setCancelable(false)
                    builder.setPositiveButton(resources.getString(R.string.delete_a_character_ok)) { dialogInterface, _ ->
                        deleteCharacter(card)
                        dialogInterface.dismiss()
                        populateListView()
                    }
                    builder.setNegativeButton(resources.getString(R.string.delete_a_character_cancel)) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    builder.show()
                }
                imEdit.setOnClickListener {
                    editCharacter(card)
                }

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

                val tvSkills: TextView = view.findViewById(R.id.skills_list_activity_card_list)
                val tvTalents: TextView = view.findViewById(R.id.talents_list_activity_card_list)

                tvTitle.text = card.name
                tvDescription.text = card.description

                if (card.image != null) {
                    try {
                        val file = File("/storage/" + card.image.split("/storage/")[1])
                        val uri: Uri = Uri.fromFile(file.absoluteFile)
                        imCharacterImage.setImageURI(uri)
                    } catch (e: Exception) {
                        imCharacterImage.setImageResource(R.drawable.ic_baseline_image_not_supported_24)
                    }
                }

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

                tvSkills.text = ""
                for (skill in card.skills) {
                    tvSkills.text = tvSkills.text.toString() + skill + ", "
                }

                tvSkills.text = tvSkills.text.dropLast(2)

                tvTalents.text = ""
                for (talent in card.talents) {
                    tvTalents.text = tvTalents.text.toString() + talent + ", "
                }

                tvTalents.text = tvTalents.text.dropLast(2)

                view.setOnClickListener {
                    if (characterListActivityRelativeLayout.isVisible) {
                        collapse(characterListActivityRelativeLayout)
                        imArrow.setImageResource(R.drawable.ic_baseline_expand_less)
                        expandedView = null
                    } else {
                        if (expandedView != null) {
                            expandedView!!.performClick()
                        }
                        expand(characterListActivityRelativeLayout)
                        imArrow.setImageResource(R.drawable.ic_baseline_expand_more)
                        expandedView = view
                    }
                }

                binding.listLayout.addView(view)
            }
            binding.progressBar.visibility = View.GONE
        }, 100)
    }

    private fun listeners() {
        binding.createCharacterFi.setOnClickListener {
            val builder = AlertDialog.Builder(this).create()
            val view = layoutInflater.inflate(R.layout.character_card_item_create, null)
            val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
            val buttonSave = view.findViewById<Button>(R.id.button_save_character)

            val tvName: TextInputEditText = view.findViewById(R.id.textInputEditTextCharacterName)
            val tvDescription: TextInputEditText =
                view.findViewById(R.id.textInputEditTextCharacterDescription)

            val tvM: TextInputEditText = view.findViewById(R.id.movement)
            val tvWS: TextInputEditText = view.findViewById(R.id.ws)
            val tvBS: TextInputEditText = view.findViewById(R.id.bs)
            val tvS: TextInputEditText = view.findViewById(R.id.s)
            val tvT: TextInputEditText = view.findViewById(R.id.t)
            val tvI: TextInputEditText = view.findViewById(R.id.i)
            val tvAg: TextInputEditText = view.findViewById(R.id.ag)
            val tvDex: TextInputEditText = view.findViewById(R.id.dex)
            val tvInt: TextInputEditText = view.findViewById(R.id.intelligence)
            val tvWP: TextInputEditText = view.findViewById(R.id.wp)
            val tvFel: TextInputEditText = view.findViewById(R.id.fel)
            val tvW: TextInputEditText = view.findViewById(R.id.w)

            val tvSkills: TextView = view.findViewById(R.id.skills_selector)
            val tvTalents: TextView = view.findViewById(R.id.talent_selector)

            val buttonLoadImage: AppCompatButton = view.findViewById(R.id.load_character_image)
            characterImageAux = view.findViewById(R.id.character_image_aux)

            tvSkills.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(resources.getString(R.string.select_skills))
                builder.setCancelable(false)

                val aux: Array<String> = skillArray.toTypedArray()
                skillList = ArrayList()
                selectedSkill = BooleanArray(skillArray.size) { false }

                builder.setMultiChoiceItems(aux, selectedSkill) { cb, i, b ->
                    if (b) {
                        if (skillCardArray[i].grouped) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(resources.getString(R.string.skill_data))
                            builder.setCancelable(false)
                            val specialisationsED = EditText(this)
                            specialisationsED.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                            builder.setView(specialisationsED)
                            builder.setPositiveButton(resources.getString(R.string.skill_data_ok)) { _, _ ->
                                aux[i] =
                                    skillCardArray[i].name.split(" (")[0] + " (" + specialisationsED.text.toString() + ")"
                                skillArray[i] = aux[i]
                            }
                            builder.setNegativeButton(resources.getString(R.string.skill_data_cancel)) { dialogInterface, _ ->
                                selectedSkill[i] = false
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                        skillList.add(i)
                        skillList.sort()
                    } else {
                        skillList.remove(Integer.valueOf(i))
                    }
                }

                builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                    val stringBuilder = java.lang.StringBuilder()
                    for (j in 0 until skillList.size) {
                        stringBuilder.append(skillArray[skillList[j]])
                        if (j != skillList.size - 1) {
                            stringBuilder.append(", ")
                        }
                    }
                    tvSkills.text = stringBuilder.toString()
                }

                builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }

                builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                    for (j in selectedSkill.indices) {
                        selectedSkill[j] = false
                        skillList.clear()
                        tvSkills.text = resources.getText(R.string.button_skills)
                    }
                }
                builder.show()
            }

            tvTalents.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(resources.getString(R.string.select_talents))
                builder.setCancelable(false)

                val aux: Array<String> = talentArray.toTypedArray()
                talentList = ArrayList()
                selectedTalent = BooleanArray(talentArray.size) { false }

                builder.setMultiChoiceItems(aux, selectedTalent) { _, i, b ->
                    if (b) {
                        if (talentArrayNoEdit[i].name.contains("(")) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(resources.getString(R.string.talent_data))
                            builder.setCancelable(false)
                            val specialisationsED = EditText(this)
                            specialisationsED.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                            specialisationsED.hint = talentArrayNoEdit[i].name.split("(")[1].split(")")[0]
                            builder.setView(specialisationsED)
                            builder.setPositiveButton(resources.getString(R.string.talent_data_ok)) { _, _ ->
                                aux[i] =
                                    talentArray[i].split(" (")[0] + " (" + specialisationsED.text.toString() + ")"
                                talentArray[i] = aux[i]
                            }
                            builder.setNegativeButton(resources.getString(R.string.talent_data_cancel)) { dialogInterface, _ ->
                                selectedSkill[i] = false
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                        talentList.add(i)
                        talentList.sort()
                    } else {
                        talentList.remove(Integer.valueOf(i))
                    }
                }

                builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                    val stringBuilder = java.lang.StringBuilder()
                    for (j in 0 until talentList.size) {
                        stringBuilder.append(talentArray[talentList[j]])
                        if (j != talentList.size - 1) {
                            stringBuilder.append(", ")
                        }
                    }
                    tvTalents.text = stringBuilder.toString()
                }

                builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }

                builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                    for (j in selectedTalent.indices) {
                        selectedTalent[j] = false
                        talentList.clear()
                        tvTalents.text = resources.getText(R.string.button_talents)
                    }
                }
                builder.show()
            }

            buttonLoadImage.setOnClickListener {
                askPermissionAndBrowseFile()
            }

            builder.setView(view)

            buttonCancel.setOnClickListener {
                builder.dismiss()
            }

            buttonSave.setOnClickListener {
                var nameIsOK = true
                for (chars in characterList) {
                    if (chars.name.equals(tvName.text.toString(), ignoreCase = true)) {
                        nameIsOK = false
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(resources.getString(R.string.duplicate_character))
                        builder.setMessage(resources.getString(R.string.duplicate_character_msg))
                        builder.setCancelable(false)
                        builder.setPositiveButton(resources.getString(R.string.duplicate_character_ok)) { dialogInterface, _ ->
                            dialogInterface.dismiss()
                        }
                        builder.show()
                    }
                }

                if (nameIsOK) {
                    var aux1 = mutableListOf("")
                    var aux2 = mutableListOf("")

                    if (tvSkills.text.toString() != resources.getString(R.string.button_skills)) {
                        aux1 = tvSkills.text.toString().split(", ") as MutableList<String>
                    }
                    if (tvTalents.text.toString() != resources.getString(R.string.button_talents)) {
                        aux2 = tvTalents.text.toString().split(", ") as MutableList<String>
                    }

                    val character = CharacterDataClass(
                        tvName.text.toString(),
                        tvDescription.text.toString(),
                        pathToTheImage,
                        if (tvM.text.toString() != "") tvM.text.toString().toInt() else 0,
                        if (tvWS.text.toString() != "") tvWS.text.toString().toInt() else 0,
                        if (tvBS.text.toString() != "") tvBS.text.toString().toInt() else 0,
                        if (tvS.text.toString() != "") tvS.text.toString().toInt() else 0,
                        if (tvT.text.toString() != "") tvT.text.toString().toInt() else 0,
                        if (tvI.text.toString() != "") tvI.text.toString().toInt() else 0,
                        if (tvAg.text.toString() != "") tvAg.text.toString().toInt() else 0,
                        if (tvDex.text.toString() != "") tvDex.text.toString().toInt() else 0,
                        if (tvInt.text.toString() != "") tvInt.text.toString().toInt() else 0,
                        if (tvWP.text.toString() != "") tvWP.text.toString().toInt() else 0,
                        if (tvFel.text.toString() != "") tvFel.text.toString().toInt() else 0,
                        if (tvW.text.toString() != "") tvW.text.toString().toInt() else 0,
                        aux1,
                        aux2
                    )
                    saveNewCharacter(character)
                    loadCharacters()
                    populateListView()
                    builder.dismiss()
                }
            }

            builder.setCanceledOnTouchOutside(false)
            builder.show()
        }

        binding.charactersLy.setOnClickListener {
            hideKeyboard(currentFocus ?: View(this))
        }

        binding.textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val aux: MutableList<CharacterDataClass> = mutableListOf()
                for (card in characterList) {
                    if (card.name.removeNonSpacingMarks()
                            .contains(s.toString().removeNonSpacingMarks(), ignoreCase = true)
                    ) {
                        aux.add(card)
                    }
                }

                characterList = aux
                populateListView()
            }
        })
    }

    private fun editCharacter(card: CharacterDataClass) {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.character_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_character)

        val tvName: TextInputEditText = view.findViewById(R.id.textInputEditTextCharacterName)
        tvName.setText(card.name)
        tvName.isEnabled = false

        val tvDescription: TextInputEditText =
            view.findViewById(R.id.textInputEditTextCharacterDescription)
        tvDescription.setText(card.description)

        val tvM: TextInputEditText = view.findViewById(R.id.movement)
        tvM.setText(card.M.toString())
        val tvWS: TextInputEditText = view.findViewById(R.id.ws)
        tvWS.setText(card.WS.toString())
        val tvBS: TextInputEditText = view.findViewById(R.id.bs)
        tvBS.setText(card.BS.toString())
        val tvS: TextInputEditText = view.findViewById(R.id.s)
        tvS.setText(card.S.toString())
        val tvT: TextInputEditText = view.findViewById(R.id.t)
        tvT.setText(card.T.toString())
        val tvI: TextInputEditText = view.findViewById(R.id.i)
        tvI.setText(card.I.toString())
        val tvAg: TextInputEditText = view.findViewById(R.id.ag)
        tvAg.setText(card.Ag.toString())
        val tvDex: TextInputEditText = view.findViewById(R.id.dex)
        tvDex.setText(card.Dex.toString())
        val tvInt: TextInputEditText = view.findViewById(R.id.intelligence)
        tvInt.setText(card.Int.toString())
        val tvWP: TextInputEditText = view.findViewById(R.id.wp)
        tvWP.setText(card.WP.toString())
        val tvFel: TextInputEditText = view.findViewById(R.id.fel)
        tvFel.setText(card.Fel.toString())
        val tvW: TextInputEditText = view.findViewById(R.id.w)
        tvW.setText(card.W.toString())

        val tvSkills: TextView = view.findViewById(R.id.skills_selector)
        val tvTalents: TextView = view.findViewById(R.id.talent_selector)

        val buttonLoadImage: AppCompatButton = view.findViewById(R.id.load_character_image)
        characterImageAux = view.findViewById(R.id.character_image_aux)
        if (card.image != null) {
            try {
                val file = File("/storage/" + card.image.split("/storage/")[1])
                val uri: Uri = Uri.fromFile(file.absoluteFile)
                characterImageAux.setImageURI(uri)
                pathToTheImage = uri.path!!
            } catch (e: Exception) {
                characterImageAux.setImageResource(R.drawable.ic_baseline_image_not_supported_24)
            }
        }

        tvSkills.text = ""
        for ((index, trait) in card.skills.withIndex()) {
            tvSkills.text = tvSkills.text.toString() + trait
            if (card.skills.size - 1 != index) {
                tvSkills.text = tvSkills.text.toString() + ", "
            }
        }

        tvTalents.text = ""
        for ((index, trait) in card.talents.withIndex()) {
            tvTalents.text = tvTalents.text.toString() + trait
            if (card.talents.size - 1 != index) {
                tvTalents.text = tvTalents.text.toString() + ", "
            }
        }

        tvSkills.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_skills))
            builder.setCancelable(false)

            val aux: Array<String> = skillArray.toTypedArray()
            skillList = ArrayList()
            selectedSkill = BooleanArray(skillArray.size) { false }

            for (skill in card.skills) {
                for (s in skillArray) {
                    if (s.split(" (")[0] == skill.split(" (")[0]) {
                        if (skill !in aux){
                            aux[skillArray.indexOf(s)] = skill
                        }
                        if (skillArray.indexOf(s) !in skillList){
                            skillList.add(skillArray.indexOf(s))
                        }
                        skillArray[skillArray.indexOf(s)] = skill
                        selectedSkill[skillArray.indexOf(s)] = true
                    }
                }
            }

            builder.setMultiChoiceItems(aux, selectedSkill) { _, i, b ->
                if (b) {
                    if (skillCardArray[i].grouped) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(resources.getString(R.string.skill_data))
                        builder.setCancelable(false)
                        val specialisationsED = EditText(this)
                        specialisationsED.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        specialisationsED.hint = skillCardArray[i].name.split("(")[1].split(")")[0]
                        builder.setView(specialisationsED)
                        builder.setPositiveButton(resources.getString(R.string.skill_data_ok)) { _, _ ->
                            aux[i] =
                                skillCardArray[i].name.split(" (")[0] + " (" + specialisationsED.text.toString() + ")"
                            skillArray[i] = aux[i]
                        }
                        builder.setNegativeButton(resources.getString(R.string.skill_data_cancel)) { dialogInterface, _ ->
                            selectedSkill[i] = false
                            dialogInterface.dismiss()
                        }
                        builder.show()
                    }
                    skillList.add(i)
                    skillList.sort()
                } else {
                    skillList.remove(Integer.valueOf(i))
                    card.skills.remove(skillArray[i])
                    skillArray[i] = skillCardArrayNoEdit[i].name
                }
            }

            builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                val stringBuilder = java.lang.StringBuilder()
                for (j in 0 until skillList.size) {
                    stringBuilder.append(skillArray[skillList[j]])
                    if (skillArray[skillList[j]] !in card.skills) {
                        card.skills.add(skillArray[skillList[j]])
                    }
                    if (j != skillList.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                card.skills.sort()
                tvSkills.text = stringBuilder.toString()
            }

            builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                for (j in selectedSkill.indices) {
                    selectedSkill[j] = false
                    skillList.clear()
                    tvSkills.text = resources.getText(R.string.button_skills)
                }
            }
            builder.show()
        }

        tvTalents.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_talents))
            builder.setCancelable(false)

            val aux: Array<String> = talentArray.toTypedArray()
            talentList = ArrayList()
            selectedTalent = BooleanArray(talentArray.size) { false }

            for (talent in card.talents) {
                for (t in talentArray) {
                    if (t.split(" (")[0] == talent.split(" (")[0]) {
                        selectedTalent[talentArray.indexOf(t)] = true
                        talentList.add(talentArray.indexOf(t))
                    }
                }
            }

            builder.setMultiChoiceItems(aux, selectedTalent) { _, i, b ->
                if (b) {
                    if (talentArrayNoEdit[i].name.contains("(")) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(resources.getString(R.string.talent_data))
                        builder.setCancelable(false)
                        val specialisationsED = EditText(this)
                        specialisationsED.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        specialisationsED.hint = talentArrayNoEdit[i].name.split("(")[1].split(")")[0]
                        builder.setView(specialisationsED)
                        builder.setPositiveButton(resources.getString(R.string.talent_data_ok)) { _, _ ->
                            aux[i] =
                                talentArray[i].split(" (")[0] + " (" + specialisationsED.text.toString() + ")"
                            talentArray[i] = aux[i]
                        }
                        builder.setNegativeButton(resources.getString(R.string.talent_data_cancel)) { dialogInterface, _ ->
                            selectedSkill[i] = false
                            dialogInterface.dismiss()
                        }
                        builder.show()
                    }
                    talentList.add(i)
                    talentList.sort()
                } else {
                    talentList.remove(Integer.valueOf(i))
                    card.talents.remove(talentArray[i])
                    talentArray[i] = talentArrayNoEdit[i].name
                }
            }

            builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                val stringBuilder = java.lang.StringBuilder()
                for (j in 0 until talentList.size) {
                    stringBuilder.append(talentArray[talentList[j]])
                    if (talentArray[talentList[j]] !in card.talents) {
                        card.talents.add(talentArray[talentList[j]])
                    }
                    if (j != talentList.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                card.talents.sort()
                tvTalents.text = stringBuilder.toString()
            }

            builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                for (j in selectedTalent.indices) {
                    selectedTalent[j] = false
                    talentList.clear()
                    tvTalents.text = resources.getText(R.string.button_talents)
                }
            }
            builder.show()
        }

        buttonLoadImage.setOnClickListener {
            askPermissionAndBrowseFile()
        }

        builder.setView(view)

        buttonCancel.setOnClickListener {
            builder.dismiss()
        }

        buttonSave.setOnClickListener {
            var aux1 = mutableListOf("")
            var aux2 = mutableListOf("")

            if (tvSkills.text.toString() != resources.getString(R.string.button_skills)) {
                aux1 = tvSkills.text.toString().split(", ") as MutableList<String>
            }
            if (tvTalents.text.toString() != resources.getString(R.string.button_talents)) {
                aux2 = tvTalents.text.toString().split(", ") as MutableList<String>
            }

            characterList.remove(card)
            val character = CharacterDataClass(
                tvName.text.toString(),
                tvDescription.text.toString(),
                pathToTheImage,
                if (tvM.text.toString() != "") tvM.text.toString().toInt() else 0,
                if (tvWS.text.toString() != "") tvWS.text.toString().toInt() else 0,
                if (tvBS.text.toString() != "") tvBS.text.toString().toInt() else 0,
                if (tvS.text.toString() != "") tvS.text.toString().toInt() else 0,
                if (tvT.text.toString() != "") tvT.text.toString().toInt() else 0,
                if (tvI.text.toString() != "") tvI.text.toString().toInt() else 0,
                if (tvAg.text.toString() != "") tvAg.text.toString().toInt() else 0,
                if (tvDex.text.toString() != "") tvDex.text.toString().toInt() else 0,
                if (tvInt.text.toString() != "") tvInt.text.toString().toInt() else 0,
                if (tvWP.text.toString() != "") tvWP.text.toString().toInt() else 0,
                if (tvFel.text.toString() != "") tvFel.text.toString().toInt() else 0,
                if (tvW.text.toString() != "") tvW.text.toString().toInt() else 0,
                aux1,
                aux2
            )
            saveNewCharacter(character)
            loadCharacters()
            populateListView()
            builder.dismiss()
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun askPermissionAndBrowseFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 123)
        } else {
            this.selectImageInExplorer()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            123 -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("AS", "Permission has been denied by user")
                } else {
                    this.selectImageInExplorer()
                }
            }
        }
    }

    private fun selectImageInExplorer() {
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        )

        startActivityForResult(Intent.createChooser(intent, resources.getString(R.string.select_an_image)), 777)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 777 && data != null) {
            val uri = data.data
            if (uri!!.authority!!.contains("com.google.android.apps.photos.contentprovider")) {
                if (!getFileName(uri)!!.contains(".jpg")) {
                    showWrongImageDialog()
                } else {
                    pathToTheImage = getFilePath(uri)!!
                    val file = File("/storage/" + pathToTheImage.split("/storage/")[1])
                    val uri2: Uri = Uri.fromFile(file.absoluteFile)
                    runOnUiThread {
                        characterImageAux.setImageURI(uri2)
                    }
                }
            } else {
                if (!getFileName(uri)!!.contains(".jpg")) {
                    showWrongImageDialog()
                } else {
                    pathToTheImage = data.data?.path!!
                    val file = File("/storage/" + pathToTheImage.split("/storage/")[1])
                    val uri2: Uri = Uri.fromFile(file.absoluteFile)
                    runOnUiThread {
                        characterImageAux.setImageURI(uri2)
                    }
                }
            }
        }
    }

    private fun showWrongImageDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.wrong_image_selected))
        builder.setMessage(resources.getString(R.string.wrong_image_selected_msg))
        builder.setCancelable(false)
        builder.setPositiveButton(resources.getString(R.string.wrong_image_selected_ok)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    @SuppressLint("Range")
    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result
    }

    @SuppressLint("Range")
    fun getFilePath(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(4)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result
    }

    private fun expand(v: View) {
        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) RelativeLayout.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Expansion speed of 1dp/ms
        a.duration = ((targetHeight / v.context.resources.displayMetrics.density).toInt()).toLong()
        v.startAnimation(a)
    }

    private fun collapse(v: View) {
        val initialHeight = v.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Collapse speed of 1dp/ms
        a.duration = ((initialHeight / v.context.resources.displayMetrics.density).toInt()).toLong()
        v.startAnimation(a)
    }

    fun String.removeNonSpacingMarks() =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun saveNewCharacter(character: CharacterDataClass) {
        characterList.add(character)
        val list = ArrayList<CharacterDataClass?>()
        for (char in characterList) {
            list.add(char)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileName, Context.MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
    }

    private fun deleteCharacter(character: CharacterDataClass) {
        characterList.remove(character)
        val list = ArrayList<CharacterDataClass?>()
        for (char in characterList) {
            list.add(char)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileName, Context.MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
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
        for (s in aux) {
            if (!skillCardArray.contains(s)) {
                skillCardArray.add(s)
            }
            if (!skillCardArrayNoEdit.contains(s)) {
                skillCardArrayNoEdit.add(s)
            }
            if (!skillArray.contains(s.name)) {
                skillArray.add(s.name)
            }
        }
        skillCardArray.sortBy { it.name }
        skillCardArrayNoEdit.sortBy { it.name }
        skillArray.sort()
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
            if (!talentArray.contains(t.name)) {
                talentArray.add(t.name)
            }
            if (!talentArrayNoEdit.contains(t)) {
                talentArrayNoEdit.add(t)
            }
        }
        talentArray.sort()
        talentArrayNoEdit.sortBy { it.name }
    }
}
