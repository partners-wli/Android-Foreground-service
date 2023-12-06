package com.example.foregroundservicesdemo.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.foregroundservicesdemo.receiver.RestartBackgroundService
import com.google.android.gms.location.*
import java.util.*

/**
 * Foreground service to execute task in background
 */
@Suppress("DEPRECATION")
class LocationService : Service() {
    var counter = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    //Method to create and start foreground service
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            createNotificationChanel()
        else
            startForeground(
                1, Notification()
            )
        requestLocationUpdates()
    }

    //Method to create notification channel to show user and indicate foreground service is running in background
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun createNotificationChanel() {
        val notificationChannelId = "com.foregroundservicesdemo"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            notificationChannelId, channelName, NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        val notification: Notification =
            notificationBuilder.setOngoing(true).setContentTitle("App is running count:: $counter")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE).build()
        startForeground(2, notification)
    }

    //Method to start timer as well created sticky service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startTimer()
        return START_STICKY
    }

    //Method to destroy broadcast and timer instances
    override fun onDestroy() {
        super.onDestroy()
        stopTimerTask()

        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, RestartBackgroundService::class.java)
        this.sendBroadcast(broadcastIntent)
    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                val count = counter++
                if (latitude != 0.0 && longitude != 0.0) {
                    Log.d(
                        "Location::",
                        latitude.toString() + ":::" + longitude.toString() + "Count" + count.toString()
                    )
                }
            }
        }
        timer!!.schedule(
            timerTask, 0, 1000
        ) //1 * 60 * 1000 1 minute
    }

    //Method to stop timer
    private fun stopTimerTask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    //Method to setup location objects and grab location data
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.interval = 10000
        request.fastestInterval = 5000
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        val locationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val notificationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        )

        if (locationPermission == PackageManager.PERMISSION_GRANTED && notificationPermission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is received
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location: Location? = locationResult.lastLocation
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        Log.d("Location Service", "location update $location")
                        //You can add network call or firebase call to store location data
                    }
                }
            }, null)
        }
    }
}