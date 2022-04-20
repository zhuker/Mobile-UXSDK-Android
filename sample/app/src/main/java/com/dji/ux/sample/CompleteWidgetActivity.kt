package com.dji.ux.sample

import android.app.Activity
import android.graphics.Point
import dji.ux.widget.MapWidget
import android.view.ViewGroup
import dji.ux.widget.FPVWidget
import dji.ux.widget.FPVOverlayWidget
import android.widget.RelativeLayout
import android.widget.FrameLayout
import dji.ux.panel.CameraSettingExposurePanel
import dji.ux.panel.CameraSettingAdvancedPanel
import dji.ux.widget.config.CameraConfigISOAndEIWidget
import dji.ux.widget.config.CameraConfigShutterWidget
import dji.ux.widget.config.CameraConfigApertureWidget
import dji.ux.widget.config.CameraConfigEVWidget
import dji.ux.widget.config.CameraConfigWBWidget
import dji.ux.widget.config.CameraConfigStorageWidget
import dji.ux.widget.config.CameraConfigSSDWidget
import dji.ux.widget.controls.CameraControlsWidget
import dji.ux.widget.controls.LensControlWidget
import dji.ux.widget.ThermalPaletteWidget
import android.os.Bundle
import android.util.Log
import android.view.View
import video.zhuker.sancho.R
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Button
import com.dji.mapkit.core.maps.DJIMap
import com.dji.mapkit.core.models.DJILatLng
import dji.ux.utils.DJIProductUtil
import dji.keysdk.CameraKey
import dji.keysdk.KeyManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.ux.widget.FPVOverlayWidget.GridOverlayType.PARALLEL_DIAGONAL

/**
 * Activity that shows all the UI elements together
 */
class CompleteWidgetActivity : Activity() {

    private var mapWidget: MapWidget? = null
    private var parentView: ViewGroup? = null
    private var fpvWidget: FPVWidget? = null
    private var secondaryFPVWidget: FPVWidget? = null
    private var fpvOverlayWidget: FPVOverlayWidget? = null
    private var primaryVideoView: RelativeLayout? = null
    private var secondaryVideoView: FrameLayout? = null
    private var isMapMini = true
    private var cameraSettingExposurePanel: CameraSettingExposurePanel? = null
    private var cameraSettingAdvancedPanel: CameraSettingAdvancedPanel? = null
    private var cameraConfigISOAndEIWidget: CameraConfigISOAndEIWidget? = null
    private var cameraConfigShutterWidget: CameraConfigShutterWidget? = null
    private var cameraConfigApertureWidget: CameraConfigApertureWidget? = null
    private var cameraConfigEVWidget: CameraConfigEVWidget? = null
    private var cameraConfigWBWidget: CameraConfigWBWidget? = null
    private var cameraConfigStorageWidget: CameraConfigStorageWidget? = null
    private var cameraConfigSSDWidget: CameraConfigSSDWidget? = null
    private var controlsWidget: CameraControlsWidget? = null
    private var lensControlWidget: LensControlWidget? = null
    private var thermalPaletteWidget: ThermalPaletteWidget? = null
    private var height = 0
    private var width = 0
    private var margin = 0
    private var deviceWidth = 0
    private var deviceHeight = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_widgets)
        height = DensityUtil.dip2px(this, 100f)
        width = DensityUtil.dip2px(this, 150f)
        margin = DensityUtil.dip2px(this, 12f)
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val outPoint = Point()
        display.getRealSize(outPoint)
        deviceHeight = outPoint.y
        deviceWidth = outPoint.x
        mapWidget = findViewById<View>(R.id.map_widget) as MapWidget
        mapWidget?.initGoogleMap { map: DJIMap ->
            map.setOnMapClickListener { latLng: DJILatLng? -> onViewClick(mapWidget) }
        }
        mapWidget?.onCreate(savedInstanceState)
        initCameraView()
        parentView = findViewById(R.id.root_view)
        fpvWidget = findViewById(R.id.fpv_widget)
        fpvWidget?.setOnClickListener { onViewClick(fpvWidget) }
        fpvOverlayWidget = findViewById(R.id.fpv_overlay_widget)
        fpvOverlayWidget?.isGridOverlayEnabled = true
        fpvOverlayWidget?.currentGridOverlayType = PARALLEL_DIAGONAL
        val sendGpsButton = findViewById<Button>(R.id.send_gps_button)
        sendGpsButton.setOnClickListener { sendGps() }
        primaryVideoView = findViewById(R.id.fpv_container)
        secondaryVideoView = findViewById(R.id.secondary_video_view)
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget)
        secondaryFPVWidget?.setOnClickListener { swapVideoSource() }
        fpvWidget?.setCameraIndexListener { _: Int, _: Int ->
            cameraWidgetKeyIndexUpdated(
                fpvWidget?.cameraKeyIndex ?: 0,
                fpvWidget?.lensKeyIndex ?: 0
            )
        }
        updateSecondaryVideoVisibility()
    }

    private fun sendGps() {
        val product = DJISDKManager.getInstance().product
        if (product == null || product !is Aircraft) {
            Log.d(TAG, "aircraft not connected")
            return
        }
        val state = product.flightController?.state
        if (state != null) {
            val loc = state.aircraftLocation
            if (loc != null) {
                Log.d(
                    TAG,
                    "lat: ${loc.latitude} lon: ${loc.longitude} alt: ${loc.altitude}"
                )
            } else {
                Log.w(TAG, "aircraftLocation not found")
            }
            val att = state.attitude
            if (att != null) {
                Log.d(TAG, "pitch: ${att.pitch} roll: ${att.roll} yaw: ${att.yaw}")
            } else {
                Log.w(TAG, "attitude not found")
            }
        }

        val gimbal = product.gimbals.firstOrNull()
        gimbal?.setStateCallback { gs ->
            gimbal.setStateCallback(null)
            val att = gs.attitudeInDegrees
            Log.d(TAG, "gimbal pitch: ${att.pitch} roll: ${att.roll} yaw: ${att.yaw}")
        }
    }

    private fun initCameraView() {
        cameraSettingExposurePanel = findViewById(R.id.camera_setting_exposure_panel)
        cameraSettingAdvancedPanel = findViewById(R.id.camera_setting_advanced_panel)
        cameraConfigISOAndEIWidget = findViewById(R.id.camera_config_iso_and_ei_widget)
        cameraConfigShutterWidget = findViewById(R.id.camera_config_shutter_widget)
        cameraConfigApertureWidget = findViewById(R.id.camera_config_aperture_widget)
        cameraConfigEVWidget = findViewById(R.id.camera_config_ev_widget)
        cameraConfigWBWidget = findViewById(R.id.camera_config_wb_widget)
        cameraConfigStorageWidget = findViewById(R.id.camera_config_storage_widget)
        cameraConfigSSDWidget = findViewById(R.id.camera_config_ssd_widget)
        lensControlWidget = findViewById(R.id.camera_lens_control)
        controlsWidget = findViewById(R.id.CameraCapturePanel)
        thermalPaletteWidget = findViewById(R.id.thermal_pallette_widget)
    }

    private fun onViewClick(view: View?) {
        if (view === fpvWidget && !isMapMini) {
            resizeFPVWidget(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT,
                0,
                0
            )
            reorderCameraCapturePanel()
            val mapViewAnimation =
                ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin)
            mapWidget?.startAnimation(mapViewAnimation)
            isMapMini = true
        } else if (view === mapWidget && isMapMini) {
            hidePanels()
            resizeFPVWidget(width, height, margin, 12)
            reorderCameraCapturePanel()
            val mapViewAnimation =
                ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0)
            mapWidget?.startAnimation(mapViewAnimation)
            isMapMini = false
        }
    }

    private fun resizeFPVWidget(width: Int, height: Int, margin: Int, fpvInsertPosition: Int) {
        val fpvParams = primaryVideoView?.layoutParams as? RelativeLayout.LayoutParams ?: return
        fpvParams.height = height
        fpvParams.width = width
        fpvParams.rightMargin = margin
        fpvParams.bottomMargin = margin
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0)
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE)
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        }
        primaryVideoView?.layoutParams = fpvParams
        parentView?.removeView(primaryVideoView)
        parentView?.addView(primaryVideoView, fpvInsertPosition)
    }

    private fun reorderCameraCapturePanel() {
        val cameraCapturePanel = findViewById<View>(R.id.CameraCapturePanel)
        parentView?.removeView(cameraCapturePanel)
        parentView?.addView(cameraCapturePanel, if (isMapMini) 9 else 13)
    }

    private fun swapVideoSource() {
        if (secondaryFPVWidget?.videoSource == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget?.videoSource = FPVWidget.VideoSource.SECONDARY
            secondaryFPVWidget?.videoSource = FPVWidget.VideoSource.PRIMARY
        } else {
            fpvWidget?.videoSource = FPVWidget.VideoSource.PRIMARY
            secondaryFPVWidget?.videoSource = FPVWidget.VideoSource.SECONDARY
        }
    }

    private fun cameraWidgetKeyIndexUpdated(keyIndex: Int, subKeyIndex: Int) {
        controlsWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraSettingExposurePanel?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraSettingAdvancedPanel?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraConfigISOAndEIWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraConfigShutterWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraConfigApertureWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraConfigEVWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraConfigWBWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraConfigStorageWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        cameraConfigSSDWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        controlsWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        lensControlWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        thermalPaletteWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
        fpvOverlayWidget?.updateKeyOnIndex(keyIndex, subKeyIndex)
    }

    private fun updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget?.videoSource == null || !DJIProductUtil.isSupportMultiCamera()) {
            secondaryVideoView?.visibility = View.GONE
        } else {
            secondaryVideoView?.visibility = View.VISIBLE
        }
    }

    private fun hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(
                CameraKey.create(CameraKey.HISTOGRAM_ENABLED, fpvWidget?.cameraKeyIndex ?: 0),
                false,
                null
            )
            KeyManager.getInstance().setValue(
                CameraKey.create(
                    CameraKey.COLOR_WAVEFORM_ENABLED,
                    fpvWidget?.cameraKeyIndex ?: 0
                ), false, null
            )
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        controlsWidget?.setAdvancedPanelVisibility(false)
        controlsWidget?.setExposurePanelVisibility(false)

        //These panels don't have a button state, so we can just hide them.
        findViewById<View>(R.id.pre_flight_check_list).visibility = View.GONE
        findViewById<View>(R.id.rtk_panel).visibility = View.GONE
        //findViewById(R.id.simulator_panel).setVisibility(View.GONE);
        findViewById<View>(R.id.spotlight_panel).visibility = View.GONE
        findViewById<View>(R.id.speaker_panel).visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()

        // Hide both the navigation bar and the status bar.
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        mapWidget?.onResume()
    }

    override fun onPause() {
        mapWidget?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapWidget?.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapWidget?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapWidget?.onLowMemory()
    }

    private inner class ResizeAnimation(
        private val mView: View?,
        private val mFromWidth: Int,
        private val mFromHeight: Int,
        private val mToWidth: Int,
        private val mToHeight: Int,
        private val mMargin: Int
    ) : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight
            val width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth
            val p = mView?.layoutParams as RelativeLayout.LayoutParams
            p.height = height.toInt()
            p.width = width.toInt()
            p.rightMargin = mMargin
            p.bottomMargin = mMargin
            mView.requestLayout()
        }

        init {
            duration = 300
        }
    }

    companion object {
        private const val TAG = "Sancho"
    }
}