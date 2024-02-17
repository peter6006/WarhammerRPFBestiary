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
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import peter.skydev.warhammerrpfhelper.R
import peter.skydev.warhammerrpfhelper.dataClass.BestiaryDataClass
import peter.skydev.warhammerrpfhelper.dataClass.TraitsDataClass
import peter.skydev.warhammerrpfhelper.databinding.ActivityBestiaryBinding
import java.io.*
import java.text.Normalizer
import java.util.*


class Bestiary : AppCompatActivity() {
    private lateinit var binding: ActivityBestiaryBinding
    private val fileName = "beasts.json"
    private val fileNameTraits = "traits.json"
    private lateinit var bestiaryList: MutableList<Any>
    private lateinit var auxBestiaryList: MutableList<BestiaryDataClass>
    private lateinit var traitsList: MutableList<TraitsDataClass>
    private lateinit var auxTraitList: MutableList<TraitsDataClass>
    private var expandedView: View? = null
    private var opionSelected = 0

    private var pathToTheImage = ""
    private lateinit var beastImageAux: ImageView
    private var traitsArray: MutableList<String> = mutableListOf()
    lateinit var selectedMTraits: BooleanArray
    var traitsListAux: ArrayList<TraitsDataClass> = ArrayList()

    private var optionalTraitsArray: MutableList<String> = mutableListOf()
    lateinit var selectedOptionalTraits: BooleanArray
    var optionalTraitsListAux: ArrayList<TraitsDataClass> = ArrayList()

    private lateinit var beastListNoEditable: MutableList<BestiaryDataClass>
    private lateinit var traitListNoEditable: MutableList<TraitsDataClass>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBestiaryBinding.inflate(layoutInflater)
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
        } else {
            getJsonBestiaryDataFromAsset(this, "bestiary_en.json")
            getJsonTraitsDataFromAsset(this, "traits_en.json")
        }

        loadBeasts()
        loadTraits()

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
        auxBestiaryList = gson.fromJson(jsonString, list)
        beastListNoEditable = gson.fromJson(jsonString, list)
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
        auxTraitList = gson.fromJson(jsonString, list)
        traitListNoEditable = gson.fromJson(jsonString, list)
        for (skill in traitsList) {
            traitsArray.add(skill.name)
            optionalTraitsArray.add(skill.name)
        }
        selectedMTraits = BooleanArray(traitsArray.size) { false }
        selectedOptionalTraits = BooleanArray(traitsArray.size) { false }
    }

    private fun restoreTraits() {
        val language = Locale.getDefault().language
        if (language == "es" || language == "en") {
            getJsonTraitsDataFromAsset(this, "traits_$language.json")
        } else {
            getJsonTraitsDataFromAsset(this, "traits_en.json")
        }
        loadTraits()
    }

    private fun populateListView() {
        binding.progressBar.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.listLayout.removeAllViews()
            if (opionSelected == 0) {
                val auxList = bestiaryList as MutableList<BestiaryDataClass>
                auxList.sortBy { it.name }
                for (card: BestiaryDataClass in auxList) {
                    val view = layoutInflater.inflate(R.layout.beast_card_item, null)

                    val beastListActivityRelativeLayout: RelativeLayout =
                        view.findViewById(R.id.character_list_activity_card_text_container)
                    val tvTitle: TextView = view.findViewById(R.id.character_list_activity_card_title)
                    val imArrow: ImageView = view.findViewById(R.id.beast_list_activity_card_arrow)
                    val imDelete: ImageView = view.findViewById(R.id.beast_list_activity_card_delete)
                    val imEdit: ImageView = view.findViewById(R.id.beast_list_activity_card_edit)

                    if (card in beastListNoEditable) {
                        imDelete.visibility = View.INVISIBLE
                        imEdit.visibility = View.INVISIBLE
                    } else {
                        imDelete.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(resources.getString(R.string.delete_a_beast))
                            builder.setMessage(resources.getString(R.string.delete_a_beast_msg))
                            builder.setCancelable(false)
                            builder.setPositiveButton(resources.getString(R.string.delete_a_beast_ok)) { dialogInterface, _ ->
                                deleteBeast(card)
                                dialogInterface.dismiss()
                                populateListView()
                            }
                            builder.setNegativeButton(resources.getString(R.string.delete_a_beast_cancel)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                        imEdit.setOnClickListener {
                            editBeast(card)
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

                    val imBeastImage: ImageView = view.findViewById(R.id.beast_image)

                    val tvTraits: TextView = view.findViewById(R.id.skills_list_activity_card_list)
                    val tvOptionalTraits: TextView = view.findViewById(R.id.talents_list_activity_card_list)

                    tvTitle.text = card.name
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

                    if (card.image != null) {
                        imBeastImage.visibility = View.VISIBLE
                        try {
                            val file = File("/storage/" + card.image.split("/storage/")[1])
                            val uri: Uri = Uri.fromFile(file.absoluteFile)
                            imBeastImage.setImageURI(uri)
                        } catch (e: Exception) {
                            imBeastImage.setImageResource(R.drawable.ic_baseline_image_not_supported_24)
                        }
                    }

                    tvTraits.text = ""
                    for (trait in card.mandatory_traits) {
                        tvTraits.text = tvTraits.text.toString() + trait + ", "
                    }

                    tvTraits.text = tvTraits.text.dropLast(2)

                    tvOptionalTraits.text = ""
                    for (trait in card.optional_traits) {
                        tvOptionalTraits.text = tvOptionalTraits.text.toString() + trait + ", "
                    }

                    tvOptionalTraits.text = tvOptionalTraits.text.dropLast(2)

                    view.setOnClickListener {
                        expandedView = if (beastListActivityRelativeLayout.isVisible) {
                            collapse(beastListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_less)
                            null
                        } else {
                            if (expandedView != null) {
                                expandedView!!.performClick()
                            }
                            expand(beastListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_more)
                            view
                        }
                    }

                    binding.createFi.setOnClickListener {
                        selectedMTraits = BooleanArray(traitsArray.size) { false }
                        selectedOptionalTraits = BooleanArray(traitsArray.size) { false }
                        createBeast()
                    }

                    binding.listLayout.addView(view)
                }
            } else {
                val auxList = traitsList
                auxList.sortBy { it.name }
                for (card: TraitsDataClass in auxList) {
                    val view = layoutInflater.inflate(R.layout.trait_card_item, null)

                    val traitListActivityRelativeLayout: RelativeLayout =
                        view.findViewById(R.id.trait_list_activity_card_text_container)
                    val tvTitle: TextView = view.findViewById(R.id.trait_list_activity_card_title)
                    val tvText: TextView = view.findViewById(R.id.trait_list_activity_card_text)
                    val imArrow: ImageView = view.findViewById(R.id.trait_list_activity_card_arrow)
                    val imDelete: ImageView = view.findViewById(R.id.trait_list_activity_card_delete)
                    val imEdit: ImageView = view.findViewById(R.id.trait_list_activity_card_edit)

                    if (card in traitListNoEditable) {
                        imDelete.visibility = View.INVISIBLE
                        imEdit.visibility = View.INVISIBLE
                    } else {
                        imDelete.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(resources.getString(R.string.delete_a_trait))
                            builder.setMessage(resources.getString(R.string.delete_a_trait_msg))
                            builder.setCancelable(false)
                            builder.setPositiveButton(resources.getString(R.string.delete_a_trait_ok)) { dialogInterface, _ ->
                                deleteTrait(card)
                                dialogInterface.dismiss()
                                populateListView()
                            }
                            builder.setNegativeButton(resources.getString(R.string.delete_a_trait_cancel)) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            builder.show()
                        }
                        imEdit.setOnClickListener {
                            editTrait(card)
                        }
                    }

                    tvTitle.text = card.name
                    tvText.text = card.description

                    view.setOnClickListener {
                        expandedView = if (traitListActivityRelativeLayout.isVisible) {
                            collapse(traitListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_less)
                            null
                        } else {
                            if (expandedView != null) {
                                expandedView!!.performClick()
                            }
                            expand(traitListActivityRelativeLayout)
                            imArrow.setImageResource(R.drawable.ic_baseline_expand_more)
                            view
                        }
                    }

                    binding.createFi.setOnClickListener {
                        createTrait()
                    }

                    binding.listLayout.addView(view)
                }
            }

            binding.progressBar.visibility = View.GONE
        }, 100)
    }

    private fun listeners() {
        binding.buttonBestiary.backgroundTintList =
            resources.getColorStateList(R.color.button_pressed_tint)

        binding.buttonBestiary.setOnClickListener {
            binding.buttonBestiary.backgroundTintList =
                resources.getColorStateList(R.color.button_pressed_tint)
            binding.buttonTraits.backgroundTintList =
                resources.getColorStateList(R.color.button_no_pressed_tint)

            opionSelected = 0
            bestiaryList = auxBestiaryList as MutableList<Any>
            binding.textInputEditText.setText("")
            hideKeyboard(currentFocus ?: View(this))
            populateListView()
        }

        binding.buttonTraits.setOnClickListener {
            binding.buttonBestiary.backgroundTintList =
                resources.getColorStateList(R.color.button_no_pressed_tint)
            binding.buttonTraits.backgroundTintList =
                resources.getColorStateList(R.color.button_pressed_tint)

            opionSelected = 1
            bestiaryList = auxTraitList as MutableList<Any>
            binding.textInputEditText.setText("")
            hideKeyboard(currentFocus ?: View(this))
            populateListView()
        }

        binding.bestiaryLy.setOnClickListener {
            hideKeyboard(currentFocus ?: View(this))
        }

        binding.textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val aux: MutableList<Any> = mutableListOf()
                if (opionSelected == 0) {
                    for (card in auxBestiaryList) {
                        if (card.name.removeNonSpacingMarks().contains(s.toString().removeNonSpacingMarks(), ignoreCase = true)) {
                            aux.add(card)
                        }
                    }
                } else {
                    for (card in auxTraitList) {
                        if (card.name.removeNonSpacingMarks().contains(s.toString().removeNonSpacingMarks(), ignoreCase = true)) {
                            aux.add(card)
                        }
                    }
                }
                bestiaryList = aux
                populateListView()
            }
        })
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

    @SuppressLint("ClickableViewAccessibility")
    private fun createBeast() {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.beast_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_beast)

        val tvName: TextInputEditText = view.findViewById(R.id.textInputEditTextBeastName)

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

        val tvMandatoryTraits: TextView = view.findViewById(R.id.mandatory_traits_selector)
        val tvOptionalTraits: TextView = view.findViewById(R.id.optional_traits_selector)

        val buttonLoadImage: AppCompatButton = view.findViewById(R.id.load_beast_image)
        beastImageAux = view.findViewById(R.id.beast_image_aux)

        restoreTraits()
        traitsListAux = ArrayList()
        optionalTraitsListAux = ArrayList()
        pathToTheImage = ""

        tvMandatoryTraits.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_mandatory_traits))
            builder.setCancelable(false)

            val sc = ScrollView(this)
            val ln = LinearLayout(this)
            ln.orientation = LinearLayout.VERTICAL
            for (trait in traitsList) {
                val cb = CheckBox(this)
                cb.text = trait.name
                cb.textSize = 20F
                if (trait in optionalTraitsListAux) {
                    cb.isActivated = false
                    cb.isEnabled = false
                }
                if (trait in traitsListAux) {
                    cb.isChecked = true
                }

                cb.setOnCheckedChangeListener { _, b ->
                    run {
                        if (b) {
                            if (trait.name.contains("(")) {
                                val valuesToAdd = trait.name.split(" (")
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle(resources.getString(R.string.mandatory_trait_data))
                                builder.setCancelable(false)
                                val ln = LinearLayout(this)
                                ln.orientation = LinearLayout.VERTICAL

                                for (value in valuesToAdd) {
                                    if (value.contains(")")) {
                                        val valueED = EditText(this)
                                        valueED.hint = value.split(")")[0]
                                        valueED.inputType =
                                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                                        val params = LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        params.leftMargin = 30
                                        params.rightMargin = 30
                                        ln.addView(valueED, params)
                                    }
                                }
                                builder.setView(ln)
                                builder.setPositiveButton(resources.getString(R.string.mandatory_trait_data_ok)) { _, _ ->
                                    var valueAux = trait.name.split(" (")[0]
                                    for (child in ln.children) {
                                        val c = child as EditText
                                        valueAux = valueAux + " (" + c.text.toString() + ")"
                                    }
                                    cb.text = valueAux
                                    traitsList[traitsList.indexOf(trait)].name = valueAux
                                }
                                builder.setNegativeButton(resources.getString(R.string.mandatory_trait_data_cancel)) { dialogInterface, _ ->
                                    cb.isChecked = false
                                    dialogInterface.dismiss()
                                }
                                builder.show()
                            }
                            traitsListAux.add(trait)
                            traitsListAux.sortBy { it.name }
                        } else {
                            traitsListAux.remove(trait)
                        }
                    }
                }
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.topMargin = 30
                val scale = this.resources.displayMetrics.density
                cb.setPadding(
                    cb.paddingLeft + (10.0f * scale + 0.6f).toInt(),
                    cb.paddingTop,
                    cb.paddingRight,
                    cb.paddingBottom
                )
                ln.addView(cb, params)
            }

            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = 40
            sc.addView(ln, params)

            builder.setView(sc)

            builder.setPositiveButton(resources.getString(R.string.mandatory_trait_data_ok)) { _, _ ->
                val stringBuilder = java.lang.StringBuilder()
                for (j in traitsListAux) {
                    stringBuilder.append(j.name)
                    if (traitsListAux.indexOf(j) != traitsListAux.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                if (traitsListAux.size == 0) {
                    tvMandatoryTraits.text = resources.getString(R.string.mandatory_traits2)
                } else {
                    tvMandatoryTraits.text = stringBuilder.toString()
                }
            }

            builder.setNegativeButton(resources.getString(R.string.mandatory_trait_data_cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                for (j in selectedMTraits.indices) {
                    selectedMTraits[j] = false
                    traitsListAux.clear()
                    tvMandatoryTraits.text = resources.getText(R.string.mandatory_traits2)
                }
            }
            builder.show()
        }

        tvOptionalTraits.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_optional_traits))
            builder.setCancelable(false)

            val sc = ScrollView(this)
            val ln = LinearLayout(this)
            ln.orientation = LinearLayout.VERTICAL
            for (trait in traitsList) {
                val cb = CheckBox(this)
                cb.text = trait.name
                cb.textSize = 20F
                if (trait in traitsListAux) {
                    cb.isActivated = false
                    cb.isEnabled = false
                }
                if (trait in optionalTraitsListAux) {
                    cb.isChecked = true
                }

                cb.setOnCheckedChangeListener { _, b ->
                    run {
                        if (b) {
                            if (trait.name.contains("(")) {
                                val valuesToAdd = trait.name.split(" (")
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle(resources.getString(R.string.optional_trait_data))
                                builder.setCancelable(false)
                                val ln = LinearLayout(this)
                                ln.orientation = LinearLayout.VERTICAL

                                for (value in valuesToAdd) {
                                    if (value.contains(")")) {
                                        val valueED = EditText(this)
                                        valueED.hint = value.split(")")[0]
                                        valueED.inputType =
                                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                                        val params = LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        params.leftMargin = 30
                                        params.rightMargin = 30
                                        ln.addView(valueED, params)
                                    }
                                }
                                builder.setView(ln)
                                builder.setPositiveButton(resources.getString(R.string.optional_trait_data_ok)) { _, _ ->
                                    var valueAux = trait.name.split(" (")[0]
                                    for (child in ln.children) {
                                        val c = child as EditText
                                        valueAux = valueAux + " (" + c.text.toString() + ")"
                                    }
                                    cb.text = valueAux
                                    traitsList[traitsList.indexOf(trait)].name = valueAux
                                }
                                builder.setNegativeButton(resources.getString(R.string.optional_trait_data_cancel)) { dialogInterface, _ ->
                                    cb.isChecked = false
                                    dialogInterface.dismiss()
                                }
                                builder.show()
                            }
                            optionalTraitsListAux.add(trait)
                            optionalTraitsListAux.sortBy { it.name }
                        } else {
                            optionalTraitsListAux.remove(trait)
                        }
                    }
                }
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.topMargin = 30
                val scale = this.resources.displayMetrics.density
                cb.setPadding(
                    cb.paddingLeft + (10.0f * scale + 0.6f).toInt(),
                    cb.paddingTop,
                    cb.paddingRight,
                    cb.paddingBottom
                )
                ln.addView(cb, params)
            }

            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = 40
            sc.addView(ln, params)

            builder.setView(sc)

            builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                val stringBuilder = java.lang.StringBuilder()
                for (j in optionalTraitsListAux) {
                    stringBuilder.append(j.name)
                    if (optionalTraitsListAux.indexOf(j) != optionalTraitsListAux.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                if (traitsListAux.size == 0) {
                    tvOptionalTraits.text = resources.getString(R.string.optional_traits2)
                } else {
                    tvOptionalTraits.text = stringBuilder.toString()
                }
            }

            builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                for (j in selectedOptionalTraits.indices) {
                    selectedOptionalTraits[j] = false
                    optionalTraitsListAux.clear()
                    tvOptionalTraits.text = resources.getText(R.string.optional_traits2)
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
            for (beast in auxBestiaryList) {
                if (beast.name.equals(tvName.text.toString(), ignoreCase = true)) {
                    nameIsOK = false
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(resources.getString(R.string.duplicate_beast))
                    builder.setMessage(resources.getString(R.string.duplicate_beast_msg))
                    builder.setCancelable(false)
                    builder.setPositiveButton(resources.getString(R.string.duplicate_beast_ok)) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    builder.show()
                }
            }

            if (nameIsOK) {
                var aux1 = mutableListOf("")
                var aux2 = mutableListOf("")

                if (tvMandatoryTraits.text.toString() != resources.getString(R.string.mandatory_traits2)) {
                    aux1 = tvMandatoryTraits.text.toString().split(", ") as MutableList<String>
                }
                if (tvOptionalTraits.text.toString() != resources.getString(R.string.optional_traits2)) {
                    aux2 = tvOptionalTraits.text.toString().split(", ") as MutableList<String>
                }

                val beast = BestiaryDataClass(
                    tvName.text.toString(),
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
                saveNewBeast(beast)
                loadBeasts()
                populateListView()
                builder.dismiss()
            }
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
                        beastImageAux.setImageURI(uri2)
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
                        beastImageAux.setImageURI(uri2)
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

    private fun saveNewBeast(beastToAdd: BestiaryDataClass) {
        auxBestiaryList.add(beastToAdd)
        val list = ArrayList<BestiaryDataClass?>()
        for (beast in auxBestiaryList) {
            list.add(beast)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileName, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
    }

    private fun deleteBeast(beastToDelete: BestiaryDataClass) {
        auxBestiaryList.remove(beastToDelete)
        val list = ArrayList<BestiaryDataClass?>()
        for (beast in auxBestiaryList) {
            list.add(beast)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileName, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
        bestiaryList = auxBestiaryList as MutableList<Any>
    }

    private fun editBeast(card: BestiaryDataClass) {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.beast_card_item_create, null)
        val buttonCancel = view.findViewById<AppCompatButton>(R.id.button_cancel)
        val buttonSave = view.findViewById<AppCompatButton>(R.id.button_save_beast)

        val tvName: TextInputEditText = view.findViewById(R.id.textInputEditTextBeastName)
        tvName.setText(card.name)
        tvName.isEnabled = false

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

        val tvMandatoryTraits: TextView = view.findViewById(R.id.mandatory_traits_selector)
        val tvOptionalTraits: TextView = view.findViewById(R.id.optional_traits_selector)

        val buttonLoadImage: AppCompatButton = view.findViewById(R.id.load_beast_image)
        beastImageAux = view.findViewById(R.id.beast_image_aux)
        if (card.image != null) {
            try {
                val file = File("/storage/" + card.image.split("/storage/")[1])
                val uri: Uri = Uri.fromFile(file.absoluteFile)
                beastImageAux.setImageURI(uri)
                pathToTheImage = uri.path!!
            } catch (e: Exception) {
                beastImageAux.setImageResource(R.drawable.ic_baseline_image_not_supported_24)
            }
        }

        tvMandatoryTraits.text = ""
        for ((index, trait) in card.mandatory_traits.withIndex()) {
            tvMandatoryTraits.text = tvMandatoryTraits.text.toString() + trait
            if (card.mandatory_traits.size - 1 != index) {
                tvMandatoryTraits.text = tvMandatoryTraits.text.toString() + ", "
            }
        }

        tvOptionalTraits.text = ""
        for ((index, trait) in card.optional_traits.withIndex()) {
            tvOptionalTraits.text = tvOptionalTraits.text.toString() + trait
            if (card.optional_traits.size - 1 != index) {
                tvOptionalTraits.text = tvOptionalTraits.text.toString() + ", "
            }
        }

        tvMandatoryTraits.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_mandatory_traits))
            builder.setCancelable(false)

            val sc = ScrollView(this)
            val ln = LinearLayout(this)
            ln.orientation = LinearLayout.VERTICAL

            traitsListAux = ArrayList()

            for (t1 in card.mandatory_traits) {
                for (t2 in traitListNoEditable) {
                    if (t2.name.split(" (")[0] == t1.split(" (")[0]) {
                        t2.name = t1
                        traitsListAux.add(t2)
                    }
                }
            }

            for (trait in traitsList) {
                val cb = CheckBox(this)
                for (opTrait in card.optional_traits) {
                    if (opTrait.split(" (")[0].equals(trait.name.split(" (")[0])) {
                        cb.isActivated = false
                        cb.isEnabled = false

                        trait.name = opTrait
                    }
                }
                for (manTrait in card.mandatory_traits) {
                    if (manTrait.split(" (")[0].equals(trait.name.split(" (")[0])) {
                        cb.isChecked = true

                        trait.name = manTrait
                    }
                }
                for (t in traitsListAux) {
                    if (t.name.split(" (")[0].equals(trait.name.split(" (")[0])) {
                        cb.isChecked = true

                        trait.name = t.name
                    }
                }

                cb.text = trait.name
                cb.textSize = 20F

                cb.setOnCheckedChangeListener { _, b ->
                    run {
                        if (b) {
                            if (trait.name.contains("(")) {
                                val valuesToAdd = trait.name.split(" (")
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle(resources.getString(R.string.mandatory_trait_data))
                                builder.setCancelable(false)
                                val ln = LinearLayout(this)
                                ln.orientation = LinearLayout.VERTICAL

                                for (value in valuesToAdd) {
                                    if (value.contains(")")) {
                                        val valueED = EditText(this)
                                        valueED.hint = value.split(")")[0]
                                        valueED.inputType =
                                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                                        val params = LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        params.leftMargin = 30
                                        params.rightMargin = 30
                                        ln.addView(valueED, params)
                                    }
                                }
                                builder.setView(ln)
                                builder.setPositiveButton(resources.getString(R.string.mandatory_trait_data_ok)) { _, _ ->
                                    var valueAux = trait.name.split(" (")[0]
                                    for (child in ln.children) {
                                        val c = child as EditText
                                        valueAux = valueAux + " (" + c.text.toString() + ")"
                                    }
                                    cb.text = valueAux
                                }
                                builder.setNegativeButton(resources.getString(R.string.mandatory_trait_data_cancel)) { dialogInterface, _ ->
                                    cb.isChecked = false
                                    dialogInterface.dismiss()
                                }
                                builder.show()
                            }
                            traitsListAux.add(trait)
                            traitsListAux.sortBy { it.name }
                        } else {
                            traitsListAux.remove(trait)
                        }
                    }
                }
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.topMargin = 30
                val scale = this.resources.displayMetrics.density
                cb.setPadding(
                    cb.paddingLeft + (10.0f * scale + 0.6f).toInt(),
                    cb.paddingTop,
                    cb.paddingRight,
                    cb.paddingBottom
                )
                ln.addView(cb, params)
            }

            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = 40
            sc.addView(ln, params)

            builder.setView(sc)

            builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                val stringBuilder = java.lang.StringBuilder()
                for (j in traitsListAux) {
                    stringBuilder.append(j.name)
                    if (traitsListAux.indexOf(j) != traitsListAux.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                if (traitsListAux.size + card.mandatory_traits.size == 0) {
                    tvMandatoryTraits.text = resources.getString(R.string.mandatory_traits2)
                } else {
                    if (stringBuilder.toString() != "") {
                        tvMandatoryTraits.text = stringBuilder.toString()
                    }
                }
            }

            builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                traitsListAux.removeAll(traitsListAux)
                dialogInterface.dismiss()
            }

            builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                for (j in selectedMTraits.indices) {
                    selectedMTraits[j] = false
                    traitsListAux.clear()
                    tvMandatoryTraits.text = resources.getText(R.string.mandatory_traits2)
                }
            }
            builder.show()
        }

        tvOptionalTraits.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.select_optional_traits))
            builder.setCancelable(false)

            val sc = ScrollView(this)
            val ln = LinearLayout(this)
            ln.orientation = LinearLayout.VERTICAL

            optionalTraitsListAux = ArrayList()

            for (t1 in card.optional_traits) {
                for (t2 in traitListNoEditable) {
                    if (t2.name.split(" (")[0] == t1.split(" (")[0]) {
                        t2.name = t1
                        optionalTraitsListAux.add(t2)
                    }
                }
            }

            for (trait in traitsList) {
                val cb = CheckBox(this)
                for (opTrait in card.mandatory_traits) {
                    if (opTrait.split(" (")[0].equals(trait.name.split(" (")[0])) {
                        cb.isActivated = false
                        cb.isEnabled = false

                        trait.name = opTrait
                    }
                }
                for (manTrait in card.optional_traits) {
                    if (manTrait.split(" (")[0].equals(trait.name.split(" (")[0])) {
                        cb.isChecked = true

                        trait.name = manTrait
                    }
                }
                for (t in optionalTraitsListAux) {
                    if (t.name.split(" (")[0].equals(trait.name.split(" (")[0])) {
                        cb.isChecked = true

                        trait.name = t.name
                    }
                }

                cb.text = trait.name
                cb.textSize = 20F

                cb.setOnCheckedChangeListener { _, b ->
                    run {
                        if (b) {
                            if (trait.name.contains("(")) {
                                val valuesToAdd = trait.name.split(" (")
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle(resources.getString(R.string.optional_trait_data))
                                builder.setCancelable(false)
                                val ln = LinearLayout(this)
                                ln.orientation = LinearLayout.VERTICAL

                                for (value in valuesToAdd) {
                                    if (value.contains(")")) {
                                        val valueED = EditText(this)
                                        valueED.hint = value.split(")")[0]
                                        valueED.inputType =
                                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                                        val params = LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        params.leftMargin = 30
                                        params.rightMargin = 30
                                        ln.addView(valueED, params)
                                    }
                                }
                                builder.setView(ln)
                                builder.setPositiveButton(resources.getString(R.string.optional_trait_data_ok)) { _, _ ->
                                    var valueAux = trait.name.split(" (")[0]
                                    for (child in ln.children) {
                                        val c = child as EditText
                                        valueAux = valueAux + " (" + c.text.toString() + ")"
                                    }
                                    cb.text = valueAux
                                }
                                builder.setNegativeButton(resources.getString(R.string.optional_trait_data_cancel)) { dialogInterface, _ ->
                                    cb.isChecked = false
                                    dialogInterface.dismiss()
                                }
                                builder.show()
                            }
                            optionalTraitsListAux.add(trait)
                            optionalTraitsListAux.sortBy { it.name }
                        } else {
                            optionalTraitsListAux.remove(trait)
                        }
                    }
                }
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.topMargin = 30
                val scale = this.resources.displayMetrics.density
                cb.setPadding(
                    cb.paddingLeft + (10.0f * scale + 0.6f).toInt(),
                    cb.paddingTop,
                    cb.paddingRight,
                    cb.paddingBottom
                )
                ln.addView(cb, params)
            }

            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = 40
            sc.addView(ln, params)

            builder.setView(sc)

            builder.setPositiveButton(resources.getString(R.string.ok_button)) { _, _ ->
                val stringBuilder = java.lang.StringBuilder()
                for (j in optionalTraitsListAux) {
                    stringBuilder.append(j.name)
                    if (optionalTraitsListAux.indexOf(j) != optionalTraitsListAux.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                if (optionalTraitsListAux.size + card.optional_traits.size == 0) {
                    tvOptionalTraits.text = resources.getString(R.string.optional_traits2)
                } else {
                    if (stringBuilder.toString() != "") {
                        tvOptionalTraits.text = stringBuilder.toString()
                    }
                }
            }

            builder.setNegativeButton(resources.getString(R.string.cancel_button)) { dialogInterface, _ ->
                optionalTraitsListAux.removeAll(optionalTraitsListAux)
                dialogInterface.dismiss()
            }

            builder.setNeutralButton(resources.getString(R.string.clear_all_button)) { _, _ ->
                for (j in selectedOptionalTraits.indices) {
                    selectedOptionalTraits[j] = false
                    optionalTraitsListAux.clear()
                    tvOptionalTraits.text = resources.getText(R.string.optional_traits2)
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

            if (tvMandatoryTraits.text.toString() != resources.getString(R.string.mandatory_traits2)) {
                aux1 = tvMandatoryTraits.text.toString().split(", ") as MutableList<String>
            }
            if (tvOptionalTraits.text.toString() != resources.getString(R.string.optional_traits2)) {
                aux2 = tvOptionalTraits.text.toString().split(", ") as MutableList<String>
            }

            auxBestiaryList.remove(card)
            val beast = BestiaryDataClass(
                tvName.text.toString(),
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
            saveNewBeast(beast)
            loadBeasts()
            populateListView()
            builder.dismiss()
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun loadBeasts() {
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
        val list = object : TypeToken<List<BestiaryDataClass>>() {}.type
        val aux: MutableList<BestiaryDataClass> = gson.fromJson(ret, list)
        for (b in aux) {
            if (!auxBestiaryList.contains(b)) {
                auxBestiaryList.add(b)
            }
        }
        bestiaryList = auxBestiaryList as MutableList<Any>
    }

    private fun createTrait() {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.trait_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_trait)

        val tvName: TextInputEditText = view.findViewById(R.id.text_input_edit_text_trait_name)
        val tvDescription: TextInputEditText = view.findViewById(R.id.text_input_edit_text_trait_description)

        builder.setView(view)

        buttonCancel.setOnClickListener {
            builder.dismiss()
        }

        buttonSave.setOnClickListener {
            val trait = TraitsDataClass(tvName.text.toString(), tvDescription.text.toString())
            saveNewTrait(trait)
            loadTraits()
            populateListView()
            builder.dismiss()
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun saveNewTrait(traitToAdd: TraitsDataClass) {
        auxTraitList.add(traitToAdd)
        val list = ArrayList<TraitsDataClass?>()
        for (trait in auxTraitList) {
            list.add(trait)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileNameTraits, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
    }

    private fun deleteTrait(traitToDelete: TraitsDataClass) {
        auxTraitList.remove(traitToDelete)
        val list = ArrayList<TraitsDataClass?>()
        for (trait in auxTraitList) {
            list.add(trait)
        }
        val jsonElements = Gson().toJsonTree(list)
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput(fileNameTraits, MODE_PRIVATE))
            outputStreamWriter.write(jsonElements.toString())
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
        bestiaryList = auxTraitList as MutableList<Any>
    }

    private fun editTrait(card: TraitsDataClass) {
        val builder = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.trait_card_item_create, null)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonSave = view.findViewById<Button>(R.id.button_save_trait)

        val tvName: TextInputEditText = view.findViewById(R.id.text_input_edit_text_trait_name)
        val tvDescription: TextInputEditText = view.findViewById(R.id.text_input_edit_text_trait_description)

        tvName.setText(card.name)
        tvName.isEnabled = false
        tvDescription.setText(card.description)

        builder.setView(view)

        buttonCancel.setOnClickListener {
            builder.dismiss()
        }

        buttonSave.setOnClickListener {
            auxTraitList.remove(card)
            val trait = TraitsDataClass(tvName.text.toString(), tvDescription.text.toString())
            saveNewTrait(trait)
            loadTraits()
            populateListView()
            builder.dismiss()
        }

        builder.setCanceledOnTouchOutside(false)
        builder.show()
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
            if (!auxTraitList.contains(t)) {
                auxTraitList.add(t)
            }
        }
        traitsList = auxTraitList
        traitsList.sortBy { it.name }
    }
}