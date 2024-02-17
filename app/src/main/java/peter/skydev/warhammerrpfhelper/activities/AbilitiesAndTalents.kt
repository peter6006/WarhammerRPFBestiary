package peter.skydev.warhammerrpfhelper.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import peter.skydev.warhammerrpfhelper.R
import peter.skydev.warhammerrpfhelper.dataClass.SkillDataClass
import peter.skydev.warhammerrpfhelper.dataClass.TalentDataClass
import peter.skydev.warhammerrpfhelper.databinding.ActivityAbilitiesAndTalentsBinding
import java.io.*
import java.text.Normalizer
import java.util.*


class AbilitiesAndTalents : AppCompatActivity() {
    private lateinit var binding: ActivityAbilitiesAndTalentsBinding
    private val fileNameSkills = "skills.json"
    private val fileNameTalents = "talents.json"
    private lateinit var skillList: MutableList<Any>
    private lateinit var auxSkillList: MutableList<SkillDataClass>
    private lateinit var talentList: MutableList<TalentDataClass>
    private lateinit var auxTalentList: MutableList<TalentDataClass>
    private var expandedView: View? = null
    private var opionSelected = 0

    private lateinit var skillListNoEditable: MutableList<SkillDataClass>
    private lateinit var talentListNoEditable: MutableList<TalentDataClass>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbilitiesAndTalentsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportActionBar?.hide()

        MobileAds.initialize(this)
        val mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        val language = Locale.getDefault().language
        if (language == "es" || language == "en") {
            getJsonSkillDataFromAsset(this, "abilities_$language.json")
            getJsonTalentDataFromAsset(this, "talents_$language.json")
        } else {
            getJsonSkillDataFromAsset(this, "abilities_en.json")
            getJsonTalentDataFromAsset(this, "talents_en.json")
        }

        loadSkills()
        loadTalents()

        listeners()
        populateListView()
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
        skillList = gson.fromJson(jsonString, list)
        auxSkillList = gson.fromJson(jsonString, list)
        skillListNoEditable = gson.fromJson(jsonString, list)
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
        talentList = gson.fromJson(jsonString, list)
        auxTalentList = gson.fromJson(jsonString, list)
        talentListNoEditable = gson.fromJson(jsonString, list)
    }

    private fun populateListView() {
        binding.progressBar.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            binding.skillListLayout.removeAllViews()
            if (opionSelected == 0) {
                val auxList = skillList as MutableList<SkillDataClass>
                auxList.sortBy { it.name }
                for (card: SkillDataClass in auxList) {
                    val view = layoutInflater.inflate(R.layout.skill_card_item, null)

                    val skillListActivityRelativeLayout: RelativeLayout =
                        view.findViewById(R.id.skill_list_activity_card_text_container)
                    val tvTitle: TextView = view.findViewById(R.id.skill_list_activity_card_title)
                    val tvText: TextView = view.findViewById(R.id.skill_list_activity_card_text)
                    val imArrow: ImageView = view.findViewById(R.id.skill_list_activity_card_arrow)
                    val imDelete: ImageView = view.findViewById(R.id.skill_list_activity_card_delete)
                    val imEdit: ImageView = view.findViewById(R.id.skill_list_activity_card_edit)

                    if (card in skillListNoEditable) {
                        imDelete.visibility = View.INVISIBLE
                        imEdit.visibility = View.INVISIBLE
                    } else {
                        imDelete.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(resources.getString(R.string.delete_a_skill))
                            builder.setMessage(resources.getString(R.string.delete_a_skill_msg))
                            builder.setCancelable(false)
                            builder.setPositiveButton(resources.getString(R.string.delete_a_skill_ok)) { dialogInterface, _ ->
                                deleteSkill(card)
                                dialogInterface.dismiss()
                                populateListView()
                            }
                            builder.setNegativeButton(resources.getString(R.string.delete_a_skill_cancel)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                        imEdit.setOnClickListener {
                            editSkill(card)
                        }
                    }

                    tvTitle.text = card.name
                    tvText.text = card.text

                    view.setOnClickListener {
                        if (skillListActivityRelativeLayout.isVisible) {
                            collapse(skillListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_less)
                            expandedView = null
                        } else {
                            if (expandedView != null) {
                                expandedView!!.performClick()
                            }
                            expand(skillListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_more)
                            expandedView = view
                        }
                    }

                    binding.createFi.setOnClickListener {
                        createSkill()
                    }

                    binding.skillListLayout.addView(view)
                    binding.progressBar.visibility = View.GONE
                }
            } else {
                val auxList = talentList
                auxList.sortBy { it.name }
                for (card: TalentDataClass in auxList) {
                    val view = layoutInflater.inflate(R.layout.talent_card_item, null)

                    val skillListActivityRelativeLayout: RelativeLayout =
                        view.findViewById(R.id.talent_list_activity_card_text_container)
                    val tvTitle: TextView = view.findViewById(R.id.talent_list_activity_card_title)
                    val lnTests: LinearLayout = view.findViewById(R.id.talent_list_activity_card_tests_layout)
                    val tvMaxValue: TextView =
                        view.findViewById(R.id.talent_list_activity_card_max_value)
                    val tvTests: TextView = view.findViewById(R.id.talent_list_activity_card_tests)
                    val tvText: TextView = view.findViewById(R.id.talent_list_activity_card_text)
                    val imArrow: ImageView = view.findViewById(R.id.talent_list_activity_card_arrow)

                    val imDelete: ImageView = view.findViewById(R.id.talent_list_activity_card_delete)
                    val imEdit: ImageView = view.findViewById(R.id.talent_list_activity_card_edit)

                    if (card in talentListNoEditable) {
                        imDelete.visibility = View.INVISIBLE
                        imEdit.visibility = View.INVISIBLE
                    } else {
                        imDelete.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(resources.getString(R.string.delete_a_talent))
                            builder.setMessage(resources.getString(R.string.delete_a_talent_msg))
                            builder.setCancelable(false)
                            builder.setPositiveButton(resources.getString(R.string.delete_a_talent_ok)) { dialogInterface, _ ->
                                deleteTalent(card)
                                dialogInterface.dismiss()
                                populateListView()
                            }
                            builder.setNegativeButton(resources.getString(R.string.delete_a_talent_cancel)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                        imEdit.setOnClickListener {
                            editTalent(card)
                        }
                    }

                    tvTitle.text = card.name
                    tvMaxValue.text = card.max_val
                    tvTests.text = card.tests
                    tvText.text = card.description

                    if (card.tests == "") {
                        lnTests.visibility = View.GONE
                    }

                    view.setOnClickListener {
                        expandedView = if (skillListActivityRelativeLayout.isVisible) {
                            collapse(skillListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_less)
                            null
                        } else {
                            if (expandedView != null) {
                                expandedView!!.performClick()
                            }
                            expand(skillListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_more)
                            view
                        }
                    }

                    binding.createFi.setOnClickListener {
                        createTalent()
                    }
                    binding.skillListLayout.addView(view)
                    binding.progressBar.visibility = View.GONE
                }
            }
        }, 100)
    }

    private fun listeners() {
        binding.buttonSkills.backgroundTintList =
            resources.getColorStateList(R.color.button_pressed_tint)

        binding.buttonSkills.setOnClickListener {
            binding.buttonSkills.backgroundTintList =
                resources.getColorStateList(R.color.button_pressed_tint)
            binding.buttonTalents.backgroundTintList =
                resources.getColorStateList(R.color.button_no_pressed_tint)

            opionSelected = 0
            skillList = auxSkillList as MutableList<Any>
            binding.textInputEditText.setText("")
            hideKeyboard(currentFocus ?: View(this))
            populateListView()
        }

        binding.buttonTalents.setOnClickListener {
            binding.buttonSkills.backgroundTintList =
                resources.getColorStateList(R.color.button_no_pressed_tint)
            binding.buttonTalents.backgroundTintList =
                resources.getColorStateList(R.color.button_pressed_tint)

            opionSelected = 1
            talentList = auxTalentList
            binding.textInputEditText.setText("")
            hideKeyboard(currentFocus ?: View(this))
            populateListView()
        }

        binding.textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val aux: MutableList<Any> = mutableListOf()
                if (opionSelected == 0) {
                    for (card in auxSkillList) {
                        if (card.name.removeNonSpacingMarks().contains(s.toString().removeNonSpacingMarks(), ignoreCase = true)) {
                            aux.add(card)
                        }
                    }
                    skillList = aux
                } else {
                    for (card in auxTalentList) {
                        if (card.name.removeNonSpacingMarks().contains(s.toString().removeNonSpacingMarks(), ignoreCase = true)) {
                            aux.add(card)
                        }
                    }
                    talentList = aux as MutableList<TalentDataClass>
                }
                if(before != 0 || count != 0){
                    populateListView()
                }
            }
        })

        binding.abilitiesAndTalentsLy.setOnClickListener {
            hideKeyboard(currentFocus ?: View(this))
        }

        binding.buttonWs.setOnClickListener {
            if (binding.buttonWs.isChecked) {
                colorAllChipsByDefault()

                binding.buttonWs.isChecked = true
                filterByCategory(resources.getString(R.string.weapon_skill_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonBs.setOnClickListener {
            if (binding.buttonBs.isChecked) {
                colorAllChipsByDefault()

                binding.buttonBs.isChecked = true
                filterByCategory(resources.getString(R.string.ballistic_skill_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonS.setOnClickListener {
            if (binding.buttonS.isChecked) {
                colorAllChipsByDefault()

                binding.buttonS.isChecked = true
                filterByCategory(resources.getString(R.string.strength_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonT.setOnClickListener {
            if (binding.buttonT.isChecked) {
                colorAllChipsByDefault()

                binding.buttonT.isChecked = true
                filterByCategory(resources.getString(R.string.toughness_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonI.setOnClickListener {
            if (binding.buttonI.isChecked) {
                colorAllChipsByDefault()

                binding.buttonI.isChecked = true
                filterByCategory(resources.getString(R.string.initiative_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonAg.setOnClickListener {
            if (binding.buttonAg.isChecked) {
                colorAllChipsByDefault()

                binding.buttonAg.isChecked = true
                filterByCategory(resources.getString(R.string.agility_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonDex.setOnClickListener {
            if (binding.buttonDex.isChecked) {
                colorAllChipsByDefault()

                binding.buttonDex.isChecked = true
                filterByCategory(resources.getString(R.string.dexterity_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonInt.setOnClickListener {
            if (binding.buttonInt.isChecked) {
                colorAllChipsByDefault()

                binding.buttonInt.isChecked = true
                filterByCategory(resources.getString(R.string.intelligence_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonWp.setOnClickListener {
            if (binding.buttonWp.isChecked) {
                colorAllChipsByDefault()

                binding.buttonWp.isChecked = true
                filterByCategory(resources.getString(R.string.willpower_short_string))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }

        binding.buttonFel.setOnClickListener {
            if (binding.buttonFel.isChecked) {
                colorAllChipsByDefault()

                binding.buttonFel.isChecked = true
                filterByCategory(resources.getString(R.string.fellowship))
            } else {
                skillList = if (opionSelected == 0) {
                    auxSkillList as MutableList<Any>
                } else {
                    auxTalentList as MutableList<Any>
                }
                populateListView()
            }
        }
    }

    private fun colorAllChipsByDefault() {
        binding.buttonWs.isChecked = false
        binding.buttonBs.isChecked = false
        binding.buttonS.isChecked = false
        binding.buttonT.isChecked = false
        binding.buttonI.isChecked = false
        binding.buttonAg.isChecked = false
        binding.buttonDex.isChecked = false
        binding.buttonInt.isChecked = false
        binding.buttonWp.isChecked = false
        binding.buttonFel.isChecked = false
    }

    private fun filterByCategory(attToFilter: String) {
        binding.textInputEditText.setText("")
        val aux: MutableList<Any> = mutableListOf()

        if (opionSelected == 0) {
            for (card in auxSkillList) {
                if (card.att.equals(attToFilter, ignoreCase = true)) {
                    aux.add(card)
                }
            }
        } else {
            for (card in auxTalentList) {
                if (card.att.equals(attToFilter, ignoreCase = true)) {
                    aux.add(card)
                }
            }
        }

        if (opionSelected == 0){
            skillList = aux
        } else {
            talentList = aux as MutableList<TalentDataClass>
        }
        populateListView()
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
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun createSkill() {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.skill_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_skill)

        val tvName: TextInputEditText = view.findViewById(R.id.text_input_edit_text_skill_name)
        val tvDescription: TextInputEditText = view.findViewById(R.id.text_input_edit_text_skill_description)

        val spinner: Spinner = view.findViewById(R.id.attribute_spinner)
        var selectedAtt = ""
        ArrayAdapter.createFromResource(this, R.array.attributes, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinner.setSelection(0)
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAtt = parent!!.getItemAtPosition(position).toString()
                (parent.getChildAt(0) as TextView).setTextColor(Color.WHITE)
            }

        }

        val advancedCB = view.findViewById<CheckBox>(R.id.advanced_cb)
        val groupedCB = view.findViewById<CheckBox>(R.id.grouped_cb)

        builder.setView(view)

        buttonCancel.setOnClickListener {
            builder.dismiss()
        }

        buttonSave.setOnClickListener {
            var nameIsOK = true
            for (skill in auxSkillList) {
                if (skill.name.equals(tvName.text.toString(), ignoreCase = true)) {
                    nameIsOK = false
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(resources.getString(R.string.duplicate_skill))
                    builder.setMessage(resources.getString(R.string.duplicate_skill_msg))
                    builder.setCancelable(false)
                    builder.setPositiveButton(resources.getString(R.string.duplicate_skill_ok)) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    builder.show()
                }
            }
            if (nameIsOK) {
                val skill = SkillDataClass(
                    tvName.text.toString(),
                    selectedAtt.split("(")[1].split(")")[0],
                    if (!advancedCB.isChecked) "basic" else "advanced",
                    groupedCB.isChecked,
                    tvDescription.text.toString()
                )
                saveSkill(skill)
                loadSkills()
                populateListView()
                builder.dismiss()
            }
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun saveSkill(skillToAdd: SkillDataClass) {
        auxSkillList.add(skillToAdd)
        val list = ArrayList<SkillDataClass?>()
        for (skill in auxSkillList) {
            list.add(skill)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileNameSkills, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
    }

    private fun deleteSkill(card: SkillDataClass) {
        auxSkillList.remove(card)
        val list = ArrayList<SkillDataClass?>()
        for (skill in auxSkillList) {
            list.add(skill)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileNameSkills, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
        skillList = auxSkillList as MutableList<Any>
    }

    private fun editSkill(card: SkillDataClass) {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.skill_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_skill)

        val tvName: TextInputEditText = view.findViewById(R.id.text_input_edit_text_skill_name)
        tvName.setText(card.name)
        tvName.isEnabled = false
        val tvDescription: TextInputEditText = view.findViewById(R.id.text_input_edit_text_skill_description)
        tvDescription.setText(card.text)

        val spinner: Spinner = view.findViewById(R.id.attribute_spinner)
        var selectedAtt = ""
        ArrayAdapter.createFromResource(this, R.array.attributes, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        val attList = resources.getStringArray(R.array.attributes)
        spinner.setSelection(0)
        for ((index, att) in attList.withIndex()){
            if (att.contains(card.att)){
                spinner.setSelection(index)
            }
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinner.setSelection(0)
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAtt = parent!!.getItemAtPosition(position).toString()
                (parent.getChildAt(0) as TextView).setTextColor(Color.WHITE)
            }

        }

        val advancedCB = view.findViewById<CheckBox>(R.id.advanced_cb)
        advancedCB.isChecked = !card.group.contains("sic")
        val groupedCB = view.findViewById<CheckBox>(R.id.grouped_cb)
        groupedCB.isChecked = card.grouped

        builder.setView(view)

        buttonCancel.setOnClickListener {
            builder.dismiss()
        }

        buttonSave.setOnClickListener {
            auxSkillList.remove(card)
                val skill = SkillDataClass(
                    tvName.text.toString(),
                    selectedAtt.split("(")[1].split(")")[0],
                    if (!advancedCB.isChecked) "basic" else "advanced",
                    groupedCB.isChecked,
                    tvDescription.text.toString()
                )
                saveSkill(skill)
                loadSkills()
                populateListView()
                builder.dismiss()

        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun createTalent() {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.talent_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_talent)

        val tvName: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_name)
        val tvDescription: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_description)

        val tvMaxValue: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_max_value)
        val tvTest: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_test)

        val spinner: Spinner = view.findViewById(R.id.attribute_spinner)
        var selectedAtt = ""
        ArrayAdapter.createFromResource(this, R.array.attributes, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinner.setSelection(0)
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAtt = parent!!.getItemAtPosition(position).toString()
                (parent.getChildAt(0) as TextView).setTextColor(Color.WHITE)
            }

        }

        builder.setView(view)

        buttonCancel.setOnClickListener {
            builder.dismiss()
        }

        buttonSave.setOnClickListener {
            var nameIsOK = true
            for (talent in auxTalentList) {
                if (talent.name.equals(tvName.text.toString(), ignoreCase = true)) {
                    nameIsOK = false
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(resources.getString(R.string.willpower_short_string))
                    builder.setMessage(resources.getString(R.string.duplicate_talent_msg))
                    builder.setCancelable(false)
                    builder.setPositiveButton(resources.getString(R.string.duplicate_talent_ok)) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    builder.show()
                }
            }
            if (nameIsOK) {
                val talent = TalentDataClass(tvName.text.toString(), tvMaxValue.text.toString(), tvTest.text.toString(), selectedAtt.split("(")[1].split(")")[0], tvDescription.text.toString())
                saveTalent(talent)
                loadTalents()
                populateListView()
                builder.dismiss()
            }
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun saveTalent(talentToAdd: TalentDataClass) {
        auxTalentList.add(talentToAdd)
        val list = ArrayList<TalentDataClass?>()
        for (talent in auxTalentList) {
            list.add(talent)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileNameTalents, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
    }

    private fun deleteTalent(card: TalentDataClass) {
        auxTalentList.remove(card)
        val list = ArrayList<TalentDataClass?>()
        for (talent in auxTalentList) {
            list.add(talent)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileNameTalents, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
        talentList = auxTalentList
    }

    private fun editTalent(card: TalentDataClass) {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.talent_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_talent)

        val tvName: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_name)
        tvName.setText(card.name)
        tvName.isEnabled = false
        val tvDescription: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_description)
        tvDescription.setText(card.description)

        val tvMaxValue: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_max_value)
        tvMaxValue.setText(card.max_val)
        val tvTest: TextInputEditText = view.findViewById(R.id.text_input_edit_text_talent_test)
        tvTest.setText(card.tests)

        val spinner: Spinner = view.findViewById(R.id.attribute_spinner)
        var selectedAtt = ""
        ArrayAdapter.createFromResource(this, R.array.attributes, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        val attList = resources.getStringArray(R.array.attributes)
        spinner.setSelection(0)
        for ((index, att) in attList.withIndex()){
            if (att.contains(card.att)){
                spinner.setSelection(index)
            }
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinner.setSelection(0)
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAtt = parent!!.getItemAtPosition(position).toString()
                (parent.getChildAt(0) as TextView).setTextColor(Color.WHITE)
            }

        }

        builder.setView(view)

        buttonCancel.setOnClickListener {
            builder.dismiss()
        }

        buttonSave.setOnClickListener {
            auxTalentList.remove(card)
            val talent = TalentDataClass(tvName.text.toString(), tvMaxValue.text.toString(), tvTest.text.toString(), selectedAtt.split("(")[1].split(")")[0], tvDescription.text.toString())
            saveTalent(talent)
            loadTalents()
            populateListView()
            builder.dismiss()
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
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
            if (!auxTalentList.contains(t)) {
                auxTalentList.add(t)
            }
        }
        talentList = auxTalentList
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
            if (!auxSkillList.contains(t)) {
                auxSkillList.add(t)
            }
        }
        skillList = auxSkillList as MutableList<Any>
    }
}