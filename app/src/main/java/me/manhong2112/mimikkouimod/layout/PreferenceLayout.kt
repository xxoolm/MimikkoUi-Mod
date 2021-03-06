package me.manhong2112.mimikkouimod.layout

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.*
import me.manhong2112.mimikkouimod.R
import me.manhong2112.mimikkouimod.common.Config
import me.manhong2112.mimikkouimod.common.Const
import me.manhong2112.mimikkouimod.common.Utils
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.custom.ankoView
import me.manhong2112.mimikkouimod.common.TypedKey as K

@Suppress("UNCHECKED_CAST")
class PreferenceLayout {
   companion object {
      inline fun Activity.preferenceLayout(init: ViewGroup.() -> Unit = {}) = with(scrollView()) {
         Config.bindSharedPref(context.defaultSharedPreferences)
         isFillViewport = true
         id = View.generateViewId()
         verticalLayout {
            id = View.generateViewId()
            backgroundColorResource = R.color.backgroundNormal

            lparams(matchParent, matchParent)
            setOnTouchListener { _, _ -> true }
            init()
         }
      }

      inline fun ViewManager.preferenceLayout(init: ViewGroup.() -> Unit = {}) = with(scrollView()) {
         Config.bindSharedPref(context.defaultSharedPreferences)
         isFillViewport = true
         id = View.generateViewId()
         verticalLayout {
            id = View.generateViewId()
            backgroundColorResource = R.color.backgroundNormal

            lparams(matchParent, matchParent)
            setOnTouchListener { _, _ -> true }
            init()
         }
      }

      fun ViewGroup.preferencePage(page: SettingFragment, nameRes: Int, summaryRes: Int = 0, icon: Drawable? = null) {
         preferencePage(page, context.getString(nameRes), if (summaryRes == 0) null else context.getString(summaryRes), icon)
      }

      fun ViewGroup.preferencePage(page: SettingFragment, name: String, summary: String? = null, icon: Drawable? = null) {
         page.init(context as AppCompatActivity)
         basePreference(name, summary, icon) { ->
            setOnClickListener {
               page.open(this@preferencePage.context as AppCompatActivity)
            }
         }
      }

      fun ViewGroup.switchPreference(nameRes: Int, summaryRes: Int = 0, key: K<Boolean>, init: Switch.() -> Unit = {}) {
         switchPreference(context.getString(nameRes), if (summaryRes == 0) null else context.getString(summaryRes), key, init)
      }

      fun ViewManager.switchPreference(name: String, summary: String? = null, key: K<Boolean>, init: Switch.() -> Unit = {}) {
         basePreference(name, summary) { ->
            val s = ankoView({
               object : Switch(it) {
                  override fun isLaidOut(): Boolean {
                     return height > 0 && width > 0
                  }
               }
            }, 0) {
               this.init()
               context.runOnUiThread {
                  isChecked = Config[key]
               }
               isClickable = false
            }.lparams {
               height = dip(Const.prefItemHeight)
               centerInParent()
               alignParentRight()
            }
            setOnClickListener {
               s.toggle()
               Config[key] = s.isChecked
            }
         }
      }


      fun ViewGroup.seekBarPreference(nameRes: Int, numFormatRes: Int = 0,
                                      key: K<Int>,
                                      min: Int = 0, max: Int = 100,
                                      init: SeekBar.() -> Unit = {}) {
         return seekBarPreference(context.getString(nameRes), if (numFormatRes == 0) null else context.getString(numFormatRes), key, min, max, init)
      }

      fun ViewManager.seekBarPreference(name: String, numFormat: String? = null,
                                        key: K<Int>,
                                        min: Int = 0, max: Int = 100,
                                        init: SeekBar.() -> Unit = {}) {
         seekBarPreference(name, numFormat, key, min, max, Utils::id, Utils::id, init)
      }

      fun ViewManager.seekBarPreference(name: String, numFormat: String? = null,
                                        key: K<Float>,
                                        min: Float = 0f, max: Float = 100f,
                                        init: SeekBar.() -> Unit = {}) {
         val displayParse: (Float) -> Int = { (it * Const.prefFloatPrecise).toInt() }
         val valueParse: (Int) -> Float = { it / Const.prefFloatPrecise }
         seekBarPreference(name, numFormat, key, (min * Const.prefFloatPrecise).toInt(), (max * Const.prefFloatPrecise).toInt(), displayParse, valueParse, init)
      }

      fun ViewGroup.seekBarPreference(nameRes: Int, numFormatRes: Int = 0,
                                      key: K<Float>,
                                      min: Float = 0f, max: Float = 100f,
                                      init: SeekBar.() -> Unit = {}) {
         return seekBarPreference(context.getString(nameRes), if (numFormatRes == 0) null else context.getString(numFormatRes), key, min, max, init)
      }

      fun <T : Number> ViewGroup.seekBarPreference(nameRes: Int, numFormatRes: Int = 0,
                                                   key: K<T>,
                                                   min: Int = 0, max: Int = 100,
                                                   displayParse: (T) -> Int,
                                                   valueParse: (Int) -> T,
                                                   init: SeekBar.() -> Unit = {}) {
         return seekBarPreference(context.getString(nameRes), if (numFormatRes == 0) null else context.getString(numFormatRes), key, min, max, displayParse, valueParse, init)
      }

      fun <T : Number> ViewManager.seekBarPreference(name: String, numFormat: String? = null,
                                                     key: K<T>,
                                                     min: Int = 0, max: Int = 100,
                                                     displayParse: (T) -> Int,
                                                     valueParse: (Int) -> T,
                                                     init: SeekBar.() -> Unit = {}) {
         basePreference(name, "") { iconView, nameView, _ ->
            ankoView({ BubbleSeekBar(it) }, 0) {
               setPadding(25, 0, dip(8) + 25, 0)
               minimumHeight = dip(Const.prefItemHeight) / 2
               gravity = Gravity.CENTER
               this.min = min
               this.max = max

               val value = displayParse(Config[key])
               progress = value - min
               progressText = numFormat?.format(valueParse(value)) ?: valueParse(value).toString()
               setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                  override fun onProgressChanged(p0: SeekBar, value: Int, p2: Boolean) {
                     this@ankoView.progressText = numFormat?.format(valueParse(value)) ?: valueParse(value).toString()
                  }

                  override fun onStartTrackingTouch(p0: SeekBar) {
                  }

                  override fun onStopTrackingTouch(p0: SeekBar) {
                     Config[key] = valueParse(p0.progress)
                  }
               })
               init()
            }.lparams {
               leftMargin = -25
               rightMargin = -25
               width = matchParent
               height = dip(Const.prefItemHeight) / 2
               below(nameView)
               rightOf(iconView)
               alignParentBottom()
            }
         }
      }

      fun <T> ViewGroup.selectorPreference(nameRes: Int, summaryRes: Int = 0,
                                           items: List<Pair<String, T>>, key: K<T>,
                                           onClick: (DialogInterface, Int) -> Unit = { _, index ->
                                              Config[key] = items[index].second
                                           }) where T : Any {
         selectorPreference(nameRes, summaryRes, items.map { it.first }, onClick)
      }

      fun <T> ViewGroup.selectorPreference(name: String, summary: String? = null,
                                           items: List<Pair<String, T>>, key: K<T>,
                                           onClick: (DialogInterface, Int) -> Unit = { _, index ->
                                              Config[key] = items[index].second
                                           }) where T : Any {
         selectorPreference(name, summary, items.map { it.first }, onClick)
      }

      fun ViewGroup.selectorPreference(nameRes: Int, summaryRes: Int = 0, display: List<String>, onClick: (DialogInterface, Int) -> Unit) {
         val name = context.getString(nameRes)
         val summary = if (summaryRes == 0) null else context.getString(summaryRes)
         selectorPreference(name, summary, display, onClick)
      }

      fun ViewGroup.selectorPreference(name: String, summary: String? = null, display: List<String>, onClick: (DialogInterface, Int) -> Unit) {
         basePreference(name, summary) { ->
            setOnClickListener {
               context.selector(name, display, onClick)
            }
         }
      }

      fun <T> ViewGroup.editTextPreference(nameRes: Int, summaryRes: Int = 0, hintRes: Int = 0, key: K<T>, displayParser: (T) -> String = { it.toString() }, valueParser: (String) -> T = { it as T })
            where T : Any {
         editTextPreference(context.getString(nameRes),
               if (summaryRes == 0) null else context.getString(summaryRes),
               if (hintRes == 0) null else context.getString(hintRes),
               key,
               displayParser,
               valueParser)
      }

      fun <T> ViewGroup.editTextPreference(name: String, summary: String? = null, hint: String? = null, key: K<T>, displayParser: (T) -> String = { it.toString() }, valueParser: (String) -> T = { it as T })
            where T : Any {
         basePreference(name, summary) { _, _, _ ->
            setOnClickListener {
               context.editTextAlert(name, defaultText = displayParser(Config[key])) {
                  Config[key] = valueParser(it)
               }
            }
         }
      }

      fun ViewGroup.sortingPreference(nameRes: Int, key: K<List<String>>, displayList: MutableList<String>, valueList: MutableList<String>) {
         sortingPreference(name = context.getString(nameRes), key = key, displayList = displayList, valueList = valueList)
      }

      fun ViewManager.sortingPreference(name: String, key: K<List<String>>, displayList: MutableList<String>, valueList: MutableList<String>) {
         basePreference(name) { _, _, _ ->
            setOnClickListener {
               context.alert {
                  customView {
                     ankoView({ DragAndDropListView<String>(it) }, 0) {
                        targetList = displayList
                        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, displayList)
                        divider = null
                        onItemSwapListener = { i1, i2 ->
                           valueList.swap(i1, i2)
                        }
                     }
                  }

                  positiveButton("OK") {
                     Config[key] = valueList
                  }
               }.show()
            }
         }
      }

      fun ViewManager.preferenceHeader(title: String) {
         relativeLayout {
            id = View.generateViewId()
            padding = dip(8)
            val titleView = textView {
               id = android.R.id.title // why?
               text = title
            }.lparams {
               centerInParent()
            }
            view {
               backgroundColor = 0xaaaaaa.opaque
            }.lparams {
               centerVertically()
               height = dip(1.5f)
               width = wrapContent

               startOf(titleView)
               marginStart = dip(64)
               marginEnd = dip(16)
            }
            view {
               backgroundColor = 0xaaaaaa.opaque
            }.lparams {
               centerVertically()
               height = dip(1.5f)
               width = wrapContent

               endOf(titleView)
               marginEnd = dip(64)
               marginStart = dip(16)
            }
            lparams {
               width = matchParent
               height = wrapContent
            }
         }
      }

      fun ViewManager.preferenceGroup(init: _LinearLayout.() -> Unit = {}) {
         cardView {
            radius = dip(4).toFloat()
            cardElevation = 0f
            id = View.generateViewId()
            verticalLayout {
               init()
            }
            lparams {
               width = matchParent
               margin = dip(8)
            }
         }
      }

      fun ViewManager.basePreference(name: String, summary: String? = null, icon: Drawable? = null, init: _RelativeLayout.() -> Unit) =
            basePreference(name, summary, icon, { _, _, _ -> init() })

      fun ViewManager.basePreference(name: String, summary: String? = null, icon: Drawable? = null, init: _RelativeLayout.(ImageView, TextView, TextView?) -> Unit = { _, _, _ -> }) =
            relativeLayout {
               id = View.generateViewId()
               backgroundDrawable = getSelectedItemDrawable(context)
               isClickable = true
               setPadding(dip(8), 0, dip(8), 0)
               val iconView = imageView {
                  padding = dip(8)
                  id = View.generateViewId()
                  image = icon
               }.lparams {
                  centerInParent()
                  alignParentStart()
                  width = dip(Const.prefIconWidth)
                  height = dip(Const.prefIconHeight)
               }

               val nameView = textView {
                  id = View.generateViewId()
                  text = name
                  if (summary === null) {
                     gravity = Gravity.CENTER_VERTICAL
                  } else {
                     gravity = Gravity.BOTTOM
                  }
               }.lparams {
                  height = dip(Const.prefItemHeight / 2)
                  rightOf(iconView)
                  if (summary === null) {
                     centerVertically()
                  } else {
                     alignParentTop()
                  }
               }
               var summaryView: TextView? = null
               if (summary !== null) {
                  summaryView = textView {
                     text = summary
                     id = View.generateViewId()
                     gravity = Gravity.TOP
                     textColor = Color.DKGRAY
                  }.lparams {
                     height = dip(Const.prefItemHeight / 2)
                     rightOf(iconView)
                     below(nameView)
                  }
               }
               init(this@relativeLayout, iconView, nameView, summaryView)
               lparams {
                  height = dip(Const.prefItemHeight)
                  width = matchParent
               }
            }


      fun getSelectedItemDrawable(ctx: Context): Drawable {
         val ta = ctx.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
         val selectedItemDrawable = ta.getDrawable(0)
         ta.recycle()
         return selectedItemDrawable
      }
   }
}

fun <A, B> List<Pair<A, B>>.toPairList(): Pair<List<A>, List<B>> {
   val resultA = ArrayList<A>(this.size)
   val resultB = ArrayList<B>(this.size)
   this.forEachWithIndex { i, (a, b) ->
      resultA[i] = a
      resultB[i] = b
   }
   return resultA to resultB
}

fun <A, B> Pair<List<A>, List<B>>.toListPair(): List<Pair<A, B>> {
   require(this.first.size == this.second.size)
   return this.first.zip(this.second)
}

fun Context.editTextAlert(title: String, hint: String? = null, defaultText: String = "", callback: (String) -> Unit) {
   this.alert {
      customView {
         val text = editText {
            this.hint = hint
            this.setText(defaultText)
         }
         positiveButton("OK") {
            callback(text.text.toString())
         }
      }
   }.show()
}