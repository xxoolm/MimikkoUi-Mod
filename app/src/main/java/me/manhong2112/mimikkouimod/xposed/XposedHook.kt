package me.manhong2112.mimikkouimod.xposed


import android.app.Activity
import android.app.Application
import android.content.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.RelativeLayout
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.manhong2112.mimikkouimod.BuildConfig
import me.manhong2112.mimikkouimod.SettingsActivity
import me.manhong2112.mimikkouimod.common.Config
import me.manhong2112.mimikkouimod.common.Const
import me.manhong2112.mimikkouimod.common.Const.mimikkouiPackageName
import me.manhong2112.mimikkouimod.common.OnSwipeTouchListener
import me.manhong2112.mimikkouimod.common.Utils
import me.manhong2112.mimikkouimod.common.Utils.findMethod
import me.manhong2112.mimikkouimod.common.Utils.getField
import me.manhong2112.mimikkouimod.common.Utils.hook
import me.manhong2112.mimikkouimod.common.Utils.hookAllMethod
import me.manhong2112.mimikkouimod.common.Utils.invokeMethod
import me.manhong2112.mimikkouimod.common.Utils.log
import me.manhong2112.mimikkouimod.common.Utils.replace
import me.manhong2112.mimikkouimod.xposed.MimikkoID.appVariableName
import org.jetbrains.anko.contentView
import org.jetbrains.anko.find

class XposedHook : IXposedHookLoadPackage, IXposedHookInitPackageResources {
   lateinit var app: Application
   lateinit var launcherAct: Activity

   private var drawer: ViewGroup? = null
      set(value) {
         field = value
         initDrawer(launcherAct, value!!)
      }
   private var workspace: ViewGroup? = null
      set(value) {
         field = value
         initWorkspace(launcherAct, workspace!!)
      }

   private val configUpdateReceiver by lazy {
      object : BroadcastReceiver() {
         override fun onReceive(ctx: Context, intent: Intent) {
            val key = intent.getStringExtra("Key")
            log("receive config ${key}")
            val value = intent.getSerializableExtra("Value")
            Config.set(Config.Key.valueOf(key), value)
         }
      }
   }
   private val dock: RelativeLayout by lazy {
      val d = launcherAct.getField<RelativeLayout>("dock")
      initDock(launcherAct, d)
      d
   }

   private val root: RelativeLayout by lazy {
      val r = launcherAct.getField<RelativeLayout>("root")
      initRoot(launcherAct, r)
      r
   }

   override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
      if (lpparam.packageName != mimikkouiPackageName) return
      val stubClass = XposedHelpers.findClass("com.stub.StubApp", lpparam.classLoader)
      val m = stubClass.findMethod("attachBaseContext", Context::class.java)
      m.hook(after = { param ->
         val app = param.thisObject.getField(appVariableName) as Application
         realAppHook(app)
      })

   }

   @Throws(Throwable::class)
   override fun handleInitPackageResources(resparam: InitPackageResourcesParam) {
      if (resparam.packageName != mimikkouiPackageName) return
   }

   private fun realAppHook(app: Application) {
      // com.mimikko.mimikkoui.launcher.components.cell.CellView
      val launcherClass = findClass("com.mimikko.mimikkoui.launcher.activity.Launcher", app.classLoader)
      launcherClass.findMethod("onCreate", Bundle::class.java).hook(after = { param ->
         this.app = app
         app.registerReceiver(configUpdateReceiver, IntentFilter(Const.configUpdateAction))
         launcherAct = param.thisObject as Activity


         bindConfigUpdateListener()
         loadConfig(app)

         IconProvider.update(app)
         DrawerBackground.update(app)

         val mAddViewVILp = root.findMethod("addView", View::class.java, Integer.TYPE, ViewGroup.LayoutParams::class.java)
         mAddViewVILp.hook(after = {
            rootHook(launcherAct, root, it)
         })

      })

      val load = launcherClass.hookAllMethod("load") {
         // com.mimikko.mimikkoui.launcher.activity.Launcher -> load
         m, p ->
         // 鬼知道這數字甚麼意思, 我討厭殼 (好吧我承認是我渣
         if (p.args[0] as Int == 10) {
            dock
         }
      }

      val appItemEntityClass = findClass("com.mimikko.common.beans.models.AppItemEntity", app.classLoader)
      appItemEntityClass.findMethod("getIcon").replace(::iconHook)

      injectSetting(app)
   }

   private fun bindConfigUpdateListener() {
      val updateDrawerBackground = { k: Config.Key, v: Any ->
         DrawerBackground.update(app)
         if (drawer !== null) DrawerBackground.setDrawerBackground(drawer!!)
      }
      Config.setOnChangeListener(Config.Key.DrawerBlurBackground, updateDrawerBackground)
      Config.setOnChangeListener(Config.Key.DrawerBlurBackgroundBlurRadius, updateDrawerBackground)
      Config.setOnChangeListener(Config.Key.DrawerDarkBackground, updateDrawerBackground)

      Config.setOnChangeListener(Config.Key.GeneralIconPack, { key, value: String ->
         IconProvider.update(app)
      })

   }

   private fun loadConfig(ctx: Context) {
      log("loadConfig")
      val intent = Intent(Const.loadConfigAction)
      intent.setClassName(BuildConfig.APPLICATION_ID, SettingsActivity::class.java.name)
      ctx.startActivity(intent)
   }

   private fun iconHook(param: XC_MethodHook.MethodHookParam): Any {
      val name = param.thisObject.invokeMethod<ComponentName>("getId")
      return IconProvider.getIcon(name.toString())
            ?: throw Utils.CallOriginalMethod()
   }

   private fun rootHook(activity: Activity, root: RelativeLayout, param: XC_MethodHook.MethodHookParam) {
      if ((param.thisObject as View).id != root.id) return
      val innerLayout = param.args[0] as ViewGroup
      when (innerLayout) {
         is RelativeLayout -> {
            // it can be drawerLayout or Workspace
            // drawerLayout : com.mimikko.mimikkoui.launcher.components.drawer.DrawerLayout
            if (drawer === null && innerLayout.findViewById<ViewGroup?>(MimikkoID.drawer_layout) !== null) {
               drawer = innerLayout.findViewById(MimikkoID.drawer_layout) as ViewGroup
            }
            if (workspace === null && innerLayout.findViewById<ViewGroup?>(MimikkoID.workspace) !== null) {
               workspace = innerLayout.findViewById(MimikkoID.workspace) as ViewGroup
            }
         }
         else -> {
            log("rootAddView ${innerLayout::class.java.canonicalName}")
         }
      }
   }

   private fun injectSetting(app: Application) {
      val launcherSettingClass = findClass("com.mimikko.mimikkoui.launcher.activity.LauncherSettingActivity", app.classLoader)
      lateinit var launcherSettingAct: Activity
      launcherSettingClass.findMethod("onCreate", Bundle::class.java).hook { param ->
         launcherSettingAct = param.thisObject as Activity
         val contentView = launcherSettingAct.contentView!! as ViewGroup
         val setting = contentView.find<View>(MimikkoID.app_settings).parent!! as LinearLayout

         val modSettingView =
               findClass("com.mimikko.common.ui.settinglist.ListItem", app.classLoader)
                     .getDeclaredConstructor(Context::class.java)
                     .newInstance(launcherAct) as RelativeLayout
         modSettingView.invokeMethod<Unit>("setClickable", true)
         modSettingView.invokeMethod<Unit>("setLabel", "MimikkoUI-Mod")
         modSettingView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
         modSettingView.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.setClassName(BuildConfig.APPLICATION_ID, SettingsActivity::class.java.name)
            launcherSettingAct.startActivity(intent)
         }
         setting.addView(modSettingView)
      }
   }

   private fun initDock(act: Activity, dock: RelativeLayout) {
      val drawerBtn = dock.findViewById(MimikkoID.drawerButton) as View?
      if (drawerBtn !== null) {
         // btn.image = drawerButton.context.resources.getDrawable(android.R.drawable.btn_star)
         drawerBtn.setOnTouchListener(
               object : OnSwipeTouchListener(act) {
                  override fun onSwipeTop() {
                     if (Config[Config.Key.DockSwipeToDrawer]) drawerBtn.callOnClick()
                  }

                  override fun onClick() {
                     drawerBtn.callOnClick()
                  }
               }
         )
         dock.setOnTouchListener(
               object : OnSwipeTouchListener(act) {
                  override fun onSwipeTop() {
                     if (Config[Config.Key.DockSwipeToDrawer]) drawerBtn.callOnClick()
                  }
               }
         )
      }
   }

   private fun initWorkspace(activity: Activity, workspace: ViewGroup) {
      return
   }

   private fun initRoot(activity: Activity, root: RelativeLayout): RelativeLayout {
      return root
   }

   companion object Drawer {
      private fun initDrawer(activity: Activity, drawer: ViewGroup) {
         DrawerBackground.setDrawerBackground(drawer)
      }
   }
}