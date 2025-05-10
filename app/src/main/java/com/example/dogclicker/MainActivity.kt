package com.example.dogclicker

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dogclicker.ui.theme.DogClickerTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DogClickerTheme {
                var started by remember { mutableStateOf(false) }
                if (!started) {
                    StartScreen { started = true }
                } else {
                    DogClickerGame()
                }
            }
        }
    }
}

@Composable
fun StartScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val baloo = FontFamily(Font(R.font.baloo2))
    val menuMusicPlayer = remember { MediaPlayer.create(context, R.raw.menu_music) }

    DisposableEffect(Unit) {
        menuMusicPlayer.isLooping = true
        menuMusicPlayer.start()
        onDispose {
            if (menuMusicPlayer.isPlaying) menuMusicPlayer.stop()
            menuMusicPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onStart() }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Text(
            text = "Tap to Start",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = baloo,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun DogClickerGame() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val baloo = FontFamily(Font(R.font.baloo2))
    var clicks by remember { mutableStateOf(0) }
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 1.1f else 1f, label = "scale")
    val floatingTexts = remember { mutableStateListOf<Triple<String, Offset, Long>>() }
    val dogCenter = remember { mutableStateOf(Offset.Zero) }
    val dogSize = remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Game music
    val gameMusic = remember { MediaPlayer.create(context, R.raw.game_music).apply { isLooping = true; start() } }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (gameMusic.isPlaying) gameMusic.pause()
                Lifecycle.Event.ON_START -> if (!gameMusic.isPlaying) gameMusic.start()
                Lifecycle.Event.ON_DESTROY -> gameMusic.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (gameMusic.isPlaying) gameMusic.stop()
            gameMusic.release()
        }
    }

    // Click sound
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
    }
    val soundId = remember { soundPool.load(context, R.raw.click_sound, 1) }
    DisposableEffect(Unit) { onDispose { soundPool.release() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (dogCenter.value == Offset.Zero) return@detectTapGestures
                    val increment = when (clicks) {
                        in 0..99 -> 1
                        in 100..299 -> 2
                        in 300..599 -> 3
                        in 600..999 -> 4
                        else -> 5
                    }
                    clicks += increment
                    isPressed = true
                    soundPool.play(soundId, 1f, 1f, 0, 0, 1f)

                    val areaWidth = dogSize.value.width * 0.7f
                    val areaHeight = dogSize.value.height * 0.7f
                    val halfWidth = areaWidth / 2f
                    val halfHeight = areaHeight / 2f

                    val offset = Offset(
                        dogCenter.value.x + Random.nextFloat() * areaWidth - halfWidth,
                        dogCenter.value.y + Random.nextFloat() * areaHeight - halfHeight
                    )
                    val id = System.currentTimeMillis()
                    floatingTexts.add(Triple("+$increment", offset, id))
                }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.onGloballyPositioned { coords ->
                    val bounds = coords.boundsInRoot()
                    dogCenter.value = bounds.center
                    dogSize.value = bounds.size
                }
            ) {
                Image(
                    painter = painterResource(R.drawable.dogface),
                    contentDescription = "Dog face",
                    modifier = Modifier
                        .size(360.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Taps: $clicks",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = baloo,
                color = Color.White
            )
        }

        // Анимированные +1, +2
        floatingTexts.forEach { (text, offset, id) ->
            var visible by remember { mutableStateOf(true) }
            val animatedY by animateFloatAsState(if (visible) offset.y else offset.y - 60f, label = "offsetY")
            val alpha by animateFloatAsState(if (visible) 1.1f else 1f, label = "alpha")
            val density = LocalDensity.current

            Text(
                text = text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = baloo,
                color = Color.White,
                modifier = Modifier
                    .offset(
                        x = with(density) { (offset.x - 40f).toDp() },
                        y = with(density) { (animatedY - 40f).toDp() }
                    )
                    .graphicsLayer(alpha = alpha)
            )

            LaunchedEffect(id) {
                delay(300)
                visible = false
                delay(200)
                floatingTexts.removeIf { it.third == id }
            }
        }
        Text(
            text = "Center X: ${dogCenter.value.x.toInt()}, Y: ${dogCenter.value.y.toInt()}",
            fontSize = 16.sp,
            color = Color.White
        )
    }

    LaunchedEffect(clicks) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}
