package dev.logickoder.vidstream

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dev.logickoder.vidstream.domain.UserRole
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.delay

class VideoCallActivity : ComponentActivity() {
    var thisUser: Int? = null

    private val channelName by lazy { intent.getStringExtra("channel_name")!! }

    private val userRole by lazy { intent.getStringExtra("role")?.let { UserRole.valueOf(it) }!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        Log.e("Agora", "Channel Name: $channelName, User Role: $userRole")

        setContent {
            Scaffold { scaffoldPadding ->
                Box(
                    modifier = Modifier.padding(scaffoldPadding),
                    content = {
                        UIRequirePermissions(
                            onPermissionGranted = {
                                CallScreen()
                            },
                            onPermissionDenied = {
                                AlertScreen(it)
                            }
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun CallScreen(
        modifier: Modifier = Modifier,
    ) {
        val context = LocalContext.current

        var muted by remember { mutableStateOf(false) }
        val remoteUsers = remember { mutableStateListOf<Int>() }

        val engine = remember(channelName, userRole) {
            RtcEngine.create(
                context,
                BuildConfig.APP_ID,
                object : IRtcEngineEventHandler() {
                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        Log.e("Agora", "onJoinChannelSuccess: $channel, $uid")
                        runOnUiThread {
                            thisUser = uid
                            remoteUsers.clear()
                        }
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        Log.e("Agora", "onUserJoined: $uid")
                        runOnUiThread {
                            if (uid !in remoteUsers && uid != thisUser) {
                                remoteUsers.add(uid)
                            }
                        }
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        Log.e("Agora", "onUserOffline: $uid")
                        runOnUiThread {
                            remoteUsers.remove(uid)
                        }
                    }
                }
            ).apply {
                enableVideo()
                joinChannel(
                    BuildConfig.APP_TOKEN,
                    channelName,
                    0,
                    ChannelMediaOptions().apply {
                        clientRoleType = if (userRole == UserRole.Broadcaster) 1 else 0
                        channelProfile = 1
                    }
                )
            }
        }

        // A hack, since for some reason, agora is not updating when a user joins
        LaunchedEffect(remoteUsers.size) {
            if (remoteUsers.isEmpty()) {
                return@LaunchedEffect
            }
            delay(1500)
            // mute and unmute to force recomposition?
            muted = !muted
            delay(500)
            muted = !muted
        }

        DisposableEffect(engine) {
            onDispose {
                remoteUsers.clear()
                engine.leaveChannel()
                RtcEngine.destroy()
            }
        }

        Box(
            modifier = modifier.fillMaxSize(),
            content = {
                LocalView(engine)
                RemoteView(remoteUsers, engine)
                UserControls(muted, engine, { muted = it })
            }
        )
    }

    @Composable
    fun LocalView(
        engine: RtcEngine,
        modifier: Modifier = Modifier,
    ) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                FrameLayout(context).apply {
                    val surfaceView = SurfaceView(context).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                engine.startPreview()
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {
                                // Handle surface changes
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                engine.stopPreview()
                            }
                        })
                    }
                    addView(surfaceView)
                    engine.setupLocalVideo(
                        VideoCanvas(
                            surfaceView,
                            VideoCanvas.RENDER_MODE_FIT,
                            0
                        )
                    )
                }
            },
        )
    }

    @Composable
    private fun RemoteView(
        users: List<Int>,
        engine: RtcEngine,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.2f)
                .horizontalScroll(rememberScrollState()),
            content = {
                users.forEach { uid ->
                    AndroidView(
                        modifier = Modifier.size(180.dp, 240.dp),
                        factory = {
                            SurfaceView(this@VideoCallActivity).apply {
                                setZOrderMediaOverlay(true)
                                holder.setFormat(PixelFormat.TRANSLUCENT)
                                engine.setupRemoteVideo(
                                    VideoCanvas(this, VideoCanvas.RENDER_MODE_FIT, uid)
                                )
                            }
                        },
                        update = {
                            engine.setupRemoteVideo(
                                VideoCanvas(it, VideoCanvas.RENDER_MODE_FIT, uid)
                            )
                        }
                    )
                }
            }
        )
    }

    @Composable
    private fun UserControls(
        muted: Boolean,
        engine: RtcEngine,
        onMutedChange: (Boolean) -> Unit,
    ) {
        var videoDisabled by remember { mutableStateOf(false) }
        val activity = LocalContext.current as? ComponentActivity

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 50.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
            content = {
                OutlinedButton(
                    onClick = {
                        onMutedChange(!muted)
                        engine.muteLocalAudioStream(!muted)
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(50.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(if (muted) Color.Blue else Color.White),
                    content = {
                        Icon(
                            imageVector = if (muted) Icons.Rounded.Close else Icons.Rounded.Add,
                            contentDescription = null,
                            tint = if (muted) Color.White else Color.Blue
                        )
                    }
                )
                OutlinedButton(
                    onClick = {
                        engine.leaveChannel()
                        activity?.finish()
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(Color.Red),
                    content = {
                        Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White)
                    }
                )
                OutlinedButton(
                    onClick = {
                        videoDisabled = !videoDisabled
                        engine.muteLocalVideoStream(videoDisabled)
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(50.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(if (videoDisabled) Color.Blue else Color.White),
                    content = {
                        Icon(
                            imageVector = if (videoDisabled) Icons.Rounded.Call else Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (videoDisabled) Color.White else Color.Blue
                        )
                    }
                )
            }
        )
    }

    @Composable
    private fun AlertScreen(onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red),
            contentAlignment = Alignment.Center,
            content = {
                Button(
                    onClick = onClick,
                    content = {
                        Icon(Icons.Rounded.Warning, contentDescription = null)
                        Text(text = "Permission Required")
                    }
                )
            }
        )
    }

    @Composable
    private fun UIRequirePermissions(
        onPermissionGranted: @Composable () -> Unit,
        onPermissionDenied: @Composable (requester: () -> Unit) -> Unit
    ) {
        val context = LocalContext.current
        var state by remember {
            mutableStateOf(
                permissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }
            )
        }

        when (state) {
            true -> onPermissionGranted()
            else -> {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = {
                        state = it.values.all { granted -> granted }
                    }
                )
                onPermissionDenied { launcher.launch(permissions) }
            }
        }
    }

    companion object {
        private val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }
}