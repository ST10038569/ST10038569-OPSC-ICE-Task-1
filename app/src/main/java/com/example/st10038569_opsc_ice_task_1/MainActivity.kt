package com.example.st10038569_opsc_ice_task_1

import android.app.ActionBar.LayoutParams
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var backgroundImageView1: ImageView
    private lateinit var backgroundImageView2: ImageView
    private lateinit var groundImageView: ImageView
    private lateinit var pipeTopImageView: ImageView
    private lateinit var pipeBottomImageView: ImageView
    private lateinit var birdImageView: ImageView
    private lateinit var scoreTextView: TextView
    private lateinit var gameOverTextView: TextView
    private lateinit var restartButton: Button


    private var birdYPosition: Float = 0f
    private var birdVelocity: Float = 0f
    private var birdGravity: Float = 4f // Gravity effect
    private var birdJumpHeight: Float = 30f // Jump height
    private var birdRotationSpeed: Float = 20f // Rotation speed
    private var handler = Handler()
    private var updateInterval: Long = 25 // Update interval for gravity simulation
    private var chillTime: Long = 1000 // Chill time before pipes start spawning (1 second)
    private var isGameOver: Boolean = false
    private var pipeEndX = 0f;
    private var hasPassedPipe: Boolean = false
    private var score: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        backgroundImageView1 = findViewById(R.id.background1)
        backgroundImageView2 = findViewById(R.id.background2)
        groundImageView = findViewById(R.id.ground)
        pipeTopImageView = findViewById(R.id.pipeTop)
        pipeBottomImageView = findViewById(R.id.pipeBottom)
        birdImageView = findViewById(R.id.bird)
        scoreTextView = findViewById(R.id.score)
        gameOverTextView = findViewById(R.id.gameOver)
        restartButton = findViewById(R.id.restart)

        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        // Set the text size
        scoreTextView.textSize = (screenHeight * 0.05).toFloat()
        gameOverTextView.textSize = (screenHeight * 0.04).toFloat()
        restartButton.textSize = (screenHeight * 0.01).toFloat()

        // Get the current layout parameters
        val scoreLayoutParams = scoreTextView.layoutParams as FrameLayout.LayoutParams
        val buttonLayoutParams = restartButton.layoutParams as FrameLayout.LayoutParams

        // Set the top margin
        scoreLayoutParams.topMargin = (screenHeight * 0.1).toInt()
        buttonLayoutParams.topMargin = (screenHeight * 0.7).toInt()

        // Apply the updated layout parameters
        scoreTextView.layoutParams = scoreLayoutParams
        restartButton.layoutParams = buttonLayoutParams

        gameOverTextView.visibility = View.GONE
        restartButton.visibility = View.GONE
        restartButton.isEnabled = false

        // Set bird size and position
        val birdSize = (screenHeight * 0.1).toInt()
        val birdLayoutParams = FrameLayout.LayoutParams(birdSize, birdSize)
        birdImageView.layoutParams = birdLayoutParams
        birdYPosition = (screenHeight - birdSize) / 2.toFloat() // Center the bird vertically
        birdImageView.translationY = birdYPosition

        val groundHeight = (screenHeight * 0.15).toInt()
        val groundLayoutParams = groundImageView.layoutParams
        groundLayoutParams.height = groundHeight
        groundImageView.layoutParams = groundLayoutParams

        // Set pipe size relative to screen dimensions
        val pipeWidth = (screenWidth * 0.3).toInt() // Adjust this value as needed
        val pipeHeight = (screenHeight * 0.6).toInt() // Adjust this value as needed

        pipeTopImageView.layoutParams = FrameLayout.LayoutParams(pipeWidth, pipeHeight)
        pipeBottomImageView.layoutParams = FrameLayout.LayoutParams(pipeWidth, pipeHeight)

        // Flip the top pipe image vertically
        pipeTopImageView.rotationX = 180f

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set bird's X position to the center of the screen
        val birdXPosition = (screenWidth - birdSize) / 2
        birdImageView.translationX = birdXPosition.toFloat()

        restartButton.setOnClickListener {
            restartGame()
        }

        startChillTime() // Start with a chill time before pipes start spawning
        startGravitySimulation()

        if (score == 0)
        {
            score--
        }
    }

    private fun restartGame() {
        // Start the new activity to restart the game
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && !isGameOver) {
            jump()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun jump() {

        // Make the bird jump by setting a negative velocity
        birdVelocity = -birdJumpHeight
    }

    private fun startGravitySimulation() {
        startParallaxScrolling()
        handler.post(object : Runnable {
            override fun run() {

                // Update bird's position based on gravity
                birdVelocity += birdGravity
                birdYPosition += birdVelocity

                // Rotate the bird
                val rotation = if (birdVelocity > 0) {
                    birdRotationSpeed // Rotate down when falling
                } else {
                    -birdRotationSpeed // Rotate up when jumping
                }
                birdImageView.rotation = rotation

                // Prevent the bird from falling below the ground
                val maxY = (groundImageView.top - birdImageView.height).toFloat()
                if (birdYPosition > maxY) {
                    birdYPosition = maxY
                    birdVelocity = 0f
                }

                // Update bird's translationY
                birdImageView.translationY = birdYPosition

                // Update bird's translationX
                val birdXPosition = birdImageView.translationX

                pipeEndX = pipeBottomImageView.x + pipeTopImageView.width

                if (birdXPosition > pipeEndX && !hasPassedPipe) {
                    incrementScore()
                    hasPassedPipe = true
                } else if (birdXPosition <= pipeEndX) {
                    hasPassedPipe = false
                }

                // Check for collision with pipes
                checkCollision()

                // Repeat this task
                if (!isGameOver) {
                    handler.postDelayed(this, updateInterval)
                }
            }
        })
    }

    private fun incrementScore() {
        score++
        scoreTextView.text = score.toString()
        Log.d("GameDebug", "Score incremented to $score")
    }

    private fun checkCollision() {
        val birdRect = birdImageView.getHitRect()

        // Create a smaller hitbox for the pipes
        val topPipeRect = pipeTopImageView.getHitRect().apply {
            inset(50, 50) // Adjust these values as needed
        }
        val bottomPipeRect = pipeBottomImageView.getHitRect().apply {
            inset(50, 50) // Adjust these values as needed
        }

        // Check collision with pipes
        if (birdRect.intersect(topPipeRect) || birdRect.intersect(bottomPipeRect)) {
            if (!isGameOver) {
                isGameOver = true
                backgroundImageView1.animate().cancel()
                backgroundImageView2.animate().cancel()
                pipeTopImageView.animate().cancel()
                pipeBottomImageView.animate().cancel()
                endGame()
            }
        }

        // Check collision with ground
        val groundInset = 100 // Adjust this value to set the desired inset
        val groundRect = Rect(
            groundImageView.left + groundInset,
            groundImageView.top + groundInset,
            groundImageView.right - groundInset,
            groundImageView.bottom - groundInset
        )

        if (birdRect.intersect(groundRect)) {
            if (!isGameOver) {
                isGameOver = true
                backgroundImageView1.animate().cancel()
                backgroundImageView2.animate().cancel()
                pipeTopImageView.animate().cancel()
                pipeBottomImageView.animate().cancel()
                endGame()
            }
        }
    }

    private fun ImageView.getHitRect(): Rect {
        val rect = Rect()
        this.getHitRect(rect)
        rect.offset(this.left, this.top)
        return rect
    }

    private fun endGame() {
        // Stop all handlers
        handler.removeCallbacksAndMessages(null)

        // Disable further touch events
        findViewById<View>(R.id.main).setOnTouchListener(null)

        // Set game over flag to prevent further updates
        isGameOver = true

        // Play death animation
        birdImageView.animate()
            .setDuration(500) // Duration of the death animation
            .withEndAction {
                restartButton.visibility = View.VISIBLE
                restartButton.isEnabled = true
            }
            .start()

        // Show game over text
        gameOverTextView.visibility = View.VISIBLE
    }

    private fun startParallaxScrolling() {
        // Check if animations are already running
        if (backgroundImageView1.translationX != 0f) return

        val backgroundWidth = backgroundImageView1.drawable.intrinsicWidth.toFloat()

        backgroundImageView1.translationX = 0f
        backgroundImageView2.translationX = backgroundWidth

        val animator1 = backgroundImageView1.animate()
            .translationX(-backgroundWidth)
            .setDuration(30000)
            .setInterpolator(null)

        val animator2 = backgroundImageView2.animate()
            .translationX(0f)
            .setDuration(30000)
            .setInterpolator(null)

        animator1.withEndAction {
            backgroundImageView1.translationX = backgroundWidth
            backgroundImageView1.animate()
                .translationX(0f)
                .setDuration(0)
                .setInterpolator(null)
                .withEndAction {
                    startParallaxScrolling()
                }
                .start()
        }

        animator2.withEndAction {
            backgroundImageView2.translationX = 2 * backgroundWidth
            backgroundImageView2.animate()
                .translationX(backgroundWidth)
                .setDuration(0)
                .setInterpolator(null)
                .withEndAction {
                    startParallaxScrolling()
                }
                .start()
        }
    }

    private fun spawnPipes() {
        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        val minGap = (screenHeight * 0.2).toInt()
        val maxGap = (screenHeight * 0.3).toInt()

        // Random gap between pipes
        val gap = Random.nextInt(minGap, maxGap)
        Log.d("PipeDebug", "Gap: $gap")

        // Get pipe dimensions
        val pipeWidth = pipeTopImageView.layoutParams.width
        val pipeHeight = pipeTopImageView.layoutParams.height

        // Randomize top pipe's Y position within the allowed range
        val maxTopPipeY = screenHeight - groundImageView.layoutParams.height - gap - pipeHeight.toFloat()
        val minTopPipeY = -pipeHeight.toFloat() // Start partially off-screen
        val topPipeY = Random.nextFloat() * (maxTopPipeY - minTopPipeY) + minTopPipeY

        // Calculate bottom pipe's Y position based on the top pipe's Y position and the gap
        val bottomPipeY = topPipeY + pipeHeight + gap

        // Create top pipe
        pipeTopImageView.translationX = screenWidth.toFloat()
        pipeTopImageView.translationY = topPipeY
        pipeTopImageView.visibility = ImageView.VISIBLE

        // Create bottom pipe
        pipeBottomImageView.translationX = screenWidth.toFloat()
        pipeBottomImageView.translationY = bottomPipeY
        pipeBottomImageView.visibility = ImageView.VISIBLE

        // Set the end x position of the pipe pair
        pipeEndX = pipeBottomImageView.x + pipeTopImageView.width

        // Log positions for debugging
        Log.d("PipeDebug", "TopPipeX: ${pipeTopImageView.translationX}, TopPipeY: ${pipeTopImageView.translationY}")
        Log.d("PipeDebug", "BottomPipeX: ${pipeBottomImageView.translationX}, BottomPipeY: ${pipeBottomImageView.translationY}")

        // Animate pipes
        val pipeAnimator1 = pipeTopImageView.animate()
            .translationX(-pipeWidth.toFloat())
            .setDuration(4000)
            .setInterpolator(null)

        val pipeAnimator2 = pipeBottomImageView.animate()
            .translationX(-pipeWidth.toFloat())
            .setDuration(4000)
            .setInterpolator(null)

        // Ensure top pipe stays in place relative to the bottom pipe
        pipeAnimator1.withEndAction {
            pipeTopImageView.visibility = ImageView.GONE
        }

        // Ensure bottom pipe stays in place relative to the top pipe
        pipeAnimator2.withEndAction {
            pipeBottomImageView.visibility = ImageView.GONE
        }
    }

    private fun startChillTime() {
        handler.postDelayed({
            // Start spawning pipes after chill time
            spawnPipes()
            // Schedule next pair of pipes
            startPipeSpawn()
        }, chillTime)
    }

    private fun startPipeSpawn() {
        handler.post(object : Runnable {
            override fun run() {

                // Spawn new pipes
                spawnPipes()

                // Schedule next pair of pipes
                handler.postDelayed(this, 4000) // Adjust this value to control pipe frequency
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}