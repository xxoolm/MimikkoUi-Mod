package me.manhong2112.mimikkouimod.xposed

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import me.manhong2112.mimikkouimod.R
import me.manhong2112.mimikkouimod.common.Config
import me.manhong2112.mimikkouimod.common.Utils
import me.manhong2112.mimikkouimod.common.Utils.drawableToBitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.lang.ref.WeakReference


object IconProvider {
   private lateinit var ctx: WeakReference<Context>
   private lateinit var iconPack: IconPack

   fun update(ctx: Context) {
      Utils.log("IconProvider update")
      this.ctx = WeakReference(ctx)
      val value = Config.get<String>(Config.Key.GeneralIconPack)
      if (value == "default") {
         Utils.log("IconProvider update default")
         IconProvider.iconPack = object : IconPack(WeakReference(ctx), "default") {
            override fun getIcon(componentName: ComponentName): Bitmap? {
               return null // IconHook will rollback to original method if icon is null
            }

            override fun hasIcon(componentName: ComponentName): Boolean {
               return true
            }
         }
      } else {
         Utils.log("IconProvider update $value")
         IconProvider.iconPack = IconPack(WeakReference(ctx), value)
      }
   }

   fun getIcon(componentName: ComponentName): Bitmap? {
      return iconPack.getIcon(componentName)
   }

   fun hasIcon(componentName: ComponentName): Boolean {
      return iconPack.hasIcon(componentName)
   }

   fun getAllIconPack(ctx: Context): List<Pair<String, String>> {
      val pm = ctx.packageManager

      val list: MutableList<ResolveInfo> = pm.queryIntentActivities(Intent("com.novalauncher.THEME"), 0) // nova
      list.addAll(pm.queryIntentActivities(Intent("android.intent.action.MAIN").addCategory("com.teslacoilsw.launcher.THEME"), 0)) // nova
      list.addAll(pm.queryIntentActivities(Intent("org.adw.launcher.icons.ACTION_PICK_ICON"), 0)) // adw
      list.addAll(pm.queryIntentActivities(Intent("com.dlto.atom.launcher.THEME"), 0)) // atom
      list.addAll(pm.queryIntentActivities(Intent("android.intent.action.MAIN").addCategory("com.anddoes.launcher.THEME"), 0)) // Apex
      return (list.map {
         val stringId = it.activityInfo.applicationInfo.labelRes
         if (stringId == 0) {
            it.activityInfo.applicationInfo.nonLocalizedLabel.toString()
         } else {
            ctx.packageManager.getResourcesForApplication(it.activityInfo.applicationInfo).getString(stringId)
         } to it.activityInfo.packageName
      }.distinct() + (ctx.getString(R.string.pref_general_none_icon_pack) to "default")).sortedBy { it.first }
   }


   open class IconPack(private val ctx: WeakReference<Context>, private val packageName: String) {

      private val icons = HashMap<String, Bitmap>()
      private val appFilter by lazy {
         loadAppFilter()
      }
      private val res by lazy {
         ctx.get()?.run {
            packageManager.getResourcesForApplication(packageName)
         }
      }

      open fun getIcon(componentName: ComponentName): Bitmap? {
         res ?: return null
         val componentInfo = componentName.toString()
         if (componentInfo !in appFilter) {
            return null
         }
         val drawableName = appFilter[componentInfo]!!
         if (drawableName !in icons) {
            val id = res!!.getIdentifier(drawableName, "drawable", packageName)
            if (id == 0) {
               return null
            }
            icons[drawableName] = drawableToBitmap(res!!.getDrawable(id))
         }
         return icons[drawableName]!!
      }

      open fun hasIcon(componentName: ComponentName): Boolean {
         return res?.run {
            getIdentifier(componentName.toString(), "packageName", packageName) != 0
         } ?: false
      }

      private fun loadAppFilter(): HashMap<String, String> {
         val hashMap = hashMapOf<String, String>()
         res ?: return hashMap
         val id = res!!.getIdentifier("appfilter", "xml", packageName)
         val parser = if (id == 0) {
            ctx.get() ?: return hashMap
            val otherContext = ctx.get()!!.createPackageContext(packageName, 0)
            val am = otherContext.assets
            val f = XmlPullParserFactory.newInstance()
            f.isNamespaceAware = true
            f.newPullParser().also {
               it.setInput(am.open("appfilter.xml"), "utf-8")
            }
         } else {
            res!!.getXml(id)
         }

         loop@ while (true) {
            val eventType = parser.next()
            when (eventType) {
               XmlPullParser.START_TAG -> {
                  if (parser.name == "item") {
                     val key = parser.getAttributeValue(null, "component") ?: continue@loop
                     val value: String = parser.getAttributeValue(null, "drawable") ?: continue@loop
                     hashMap[key] = value
                  }
               }
               XmlPullParser.END_DOCUMENT -> break@loop
               else -> {
               }
            }
         }
         return hashMap
      }
   }

}