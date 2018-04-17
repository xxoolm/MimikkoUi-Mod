package me.manhong2112.mimikkouimod.xposed

import android.content.ComponentName
import android.content.Context

object LabelProvider {
   private val labelCache by lazy {
      hashMapOf<String, String>()
   }

   fun getLabel(ctx: Context, name: ComponentName): String {
      val s = name.toString()
      if (s !in labelCache) {
         labelCache[s] = getLabelFromRes(ctx, name)
      }
      return labelCache[s]!!
   }

   fun getLabelFromRes(ctx: Context, name: ComponentName): String {
      val packageManager = ctx.packageManager
      val info = packageManager.getActivityInfo(name, 0)
      return if (info.labelRes == 0) {
         info.nonLocalizedLabel?.toString() ?: getApplicationLabel(ctx, name)
      } else {
         packageManager.getResourcesForApplication(name.packageName).getString(info.labelRes)
      }
   }

   fun getApplicationLabel(ctx: Context, name: ComponentName): String {
      val packageManager = ctx.packageManager
      val info = packageManager.getApplicationInfo(name.packageName, 0)
      return if (info.labelRes == 0) {
         info.nonLocalizedLabel.toString()
      } else {
         packageManager.getResourcesForApplication(name.packageName).getString(info.labelRes)
      }
   }
}