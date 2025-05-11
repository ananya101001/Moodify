package com.example.moodify

import android.content.Context
import android.graphics.* // Import Bitmap, BitmapFactory etc.
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap // Extension function for cleaner loading
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class SentimentConstellationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val nodes = mutableListOf<SentimentNode>()
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var viewWidth = 0f
    private var viewHeight = 0f
    private var minTimestampSeconds = 0L
    private var maxTimestampSeconds = 0L
    private var timeSpanSeconds = 1L

    // --- Bitmaps for Moods ---
    private val moodBitmaps = mutableMapOf<String, Bitmap?>()
    private var defaultBitmap: Bitmap? = null
    private var nodeBitmapWidth = 0f
    private var nodeBitmapHeight = 0f
    private val desiredBitmapSizeDp = 32 // Desired size in DP for the icons

    // Simulation parameters (TUNE THESE)
    private val targetTimestep = 0.016f
    private val sentimentAttractionStrength = 0.05f
    private val repulsionStrength = 2000f // May need adjustment with images
    private val centerAttractionStrength = 0.005f
    private val damping = 0.95f

    private val simulationScope = CoroutineScope(Dispatchers.Default + Job())
    private var simulationJob: Job? = null

    init {
        nodePaint.style = Paint.Style.FILL
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2f // Maybe slightly thicker for black? Adjust as needed.
        linePaint.color = Color.BLACK // <--- SET TO BLACK
        linePaint.alpha = 255 // <--- SET TO FULLY OPAQUE (or slightly less like 200 if desired)

        // --- Load Bitmaps ---
        loadMoodBitmaps()
    }

    private fun loadMoodBitmaps() {
        Log.d("ConstellationView", "Loading mood bitmaps...")
        val targetSizePx = (desiredBitmapSizeDp * resources.displayMetrics.density).toInt()

        val moodMap = mapOf(
            "Happy" to R.drawable.happiness,
            "Good" to R.drawable.smiling,
            "Okay" to R.drawable.feelings,
            "Sad" to R.drawable.sad,
            "Anxious" to R.drawable.anxious
        )

        moodMap.forEach { (label, resId) ->
            try {
                val drawable = ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        drawable.toBitmap(),
                        targetSizePx,
                        targetSizePx,
                        true
                    )
                    moodBitmaps[label] = scaledBitmap
                    if (nodeBitmapWidth == 0f) {
                        nodeBitmapWidth = scaledBitmap.width.toFloat()
                        nodeBitmapHeight = scaledBitmap.height.toFloat()
                    }
                    Log.d("ConstellationView", "Loaded bitmap for $label")
                } else {
                    Log.w("ConstellationView", "Drawable not found for mood: $label (Res ID: $resId)")
                    moodBitmaps[label] = null
                }
            } catch (e: Exception) {
                Log.e("ConstellationView", "Error loading bitmap for mood: $label", e)
                moodBitmaps[label] = null
            }
        }

        try {
            val defaultDrawable = ContextCompat.getDrawable(context, R.drawable.smiling)
            if (defaultDrawable != null) {
                defaultBitmap = Bitmap.createScaledBitmap(
                    defaultDrawable.toBitmap(),
                    targetSizePx,
                    targetSizePx,
                    true
                )
            }
        } catch (e: Exception) {
            Log.e("ConstellationView", "Error loading default bitmap", e)
        }

        if (nodeBitmapWidth == 0f && defaultBitmap != null) {
            nodeBitmapWidth = defaultBitmap!!.width.toFloat()
            nodeBitmapHeight = defaultBitmap!!.height.toFloat()
        }
        Log.d("ConstellationView", "Bitmap loading complete. Node size: $nodeBitmapWidth x $nodeBitmapHeight")
    }

    fun setData(newNodes: List<SentimentNode>) {
        stopSimulation()
        if (newNodes.isEmpty()) {
            nodes.clear()
            invalidate()
            return
        }

        nodes.clear()
        nodes.addAll(newNodes.map { it.copy() })

        if (nodes.size > 1) {
            minTimestampSeconds = nodes.minOf { it.timestamp.seconds }
            maxTimestampSeconds = nodes.maxOf { it.timestamp.seconds }
            timeSpanSeconds = maxTimestampSeconds - minTimestampSeconds
            if (timeSpanSeconds <= 0L) timeSpanSeconds = 1L
        } else if (nodes.size == 1) {
            minTimestampSeconds = nodes.first().timestamp.seconds
            maxTimestampSeconds = minTimestampSeconds
            timeSpanSeconds = 1L
        }

        if (viewWidth > 0f && viewHeight > 0f) {
            initializeNodePositions()
            startSimulation()
        }
        invalidate()
    }

    private fun initializeNodePositions() {
        if (viewWidth == 0f || viewHeight == 0f || nodes.isEmpty()) return

        Log.d("ConstellationView", "Initializing node positions...")
        val paddingHorizontal = nodeBitmapWidth
        val paddingVertical = nodeBitmapHeight
        val drawableWidth = (viewWidth - 2 * paddingHorizontal).coerceAtLeast(1f)
        val drawableHeight = (viewHeight - 2 * paddingVertical).coerceAtLeast(1f)

        nodes.forEach { node ->
            val timeRatio = if (timeSpanSeconds <= 1L) 0.5f else
                ((node.timestamp.seconds - minTimestampSeconds).toFloat() / timeSpanSeconds.toFloat()).coerceIn(0f, 1f)
            node.x = paddingHorizontal + timeRatio * drawableWidth

            val sentimentRatio = ((node.sentimentScore.coerceIn(-1f, 1f)) + 1.0f) / 2.0f
            node.y = paddingVertical + (1.0f - sentimentRatio) * drawableHeight

            node.x += (-3..3).random()
            node.y += (-3..3).random()

            node.velocityX = 0f
            node.velocityY = 0f
        }
        Log.d("ConstellationView", "Node positions initialized.")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        if (nodes.isNotEmpty()) {
            initializeNodePositions()
            if (simulationJob == null || !simulationJob!!.isActive) {
                startSimulation()
            }
        }
        Log.d("ConstellationView", "onSizeChanged: w=$w, h=$h")
    }

    // Physics Simulation Methods
    private fun updatePhysics(deltaTime: Float) {
        if (nodes.size < 2) return

        nodes.forEach { node ->
            node.forceX = 0f
            node.forceY = 0f
        }

        for (i in nodes.indices) {
            val nodeA = nodes[i]
            for (j in i + 1 until nodes.size) {
                val nodeB = nodes[j]
                calculateRepulsion(nodeA, nodeB)
                calculateSentimentAttraction(nodeA, nodeB)
            }
            calculateCenterAttraction(nodeA)
        }

        nodes.forEach { node ->
            node.velocityX = (node.velocityX + node.forceX * deltaTime) * damping
            node.velocityY = (node.velocityY + node.forceY * deltaTime) * damping

            node.x += node.velocityX * deltaTime
            node.y += node.velocityY * deltaTime

            // Boundary collision using bitmap dimensions
            if (node.x < nodeBitmapWidth/2 || node.x > viewWidth - nodeBitmapWidth/2) {
                node.velocityX *= -0.6f
                node.x = node.x.coerceIn(nodeBitmapWidth/2, viewWidth - nodeBitmapWidth/2)
            }
            if (node.y < nodeBitmapHeight/2 || node.y > viewHeight - nodeBitmapHeight/2) {
                node.velocityY *= -0.6f
                node.y = node.y.coerceIn(nodeBitmapHeight/2, viewHeight - nodeBitmapHeight/2)
            }
        }
    }

    private fun calculateRepulsion(nodeA: SentimentNode, nodeB: SentimentNode) {
        val dx = nodeB.x - nodeA.x
        val dy = nodeB.y - nodeA.y
        var distanceSquared = (dx * dx + dy * dy).coerceAtLeast(0.1f)
        val distance = sqrt(distanceSquared)

        val force = repulsionStrength / distanceSquared
        val forceX = force * (dx / distance)
        val forceY = force * (dy / distance)

        nodeA.forceX -= forceX
        nodeA.forceY -= forceY
        nodeB.forceX += forceX
        nodeB.forceY += forceY
    }

    private fun calculateSentimentAttraction(nodeA: SentimentNode, nodeB: SentimentNode) {
        val sentimentDiff = abs(nodeA.sentimentScore - nodeB.sentimentScore)
        val attractionFactor = (1.0f - (sentimentDiff / 2.0f)).pow(2)

        if (attractionFactor < 0.05f) return

        val dx = nodeB.x - nodeA.x
        val dy = nodeB.y - nodeA.y
        val distance = sqrt(dx*dx + dy*dy).coerceAtLeast(1f)

        val force = sentimentAttractionStrength * distance * attractionFactor
        val forceX = force * (dx / distance)
        val forceY = force * (dy / distance)

        nodeA.forceX += forceX
        nodeA.forceY += forceY
        nodeB.forceX -= forceX
        nodeB.forceY -= forceY
    }

    private fun calculateCenterAttraction(node: SentimentNode) {
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f
        val dx = centerX - node.x
        val dy = centerY - node.y

        node.forceX += dx * centerAttractionStrength
        node.forceY += dy * centerAttractionStrength
    }

    // Simulation Control Methods
    private fun startSimulation() {
        stopSimulation()
        if (nodes.isEmpty() || !isAttachedToWindow) return

        Log.d("ConstellationView", "Starting simulation loop")
        simulationJob = simulationScope.launch {
            while (isActive) {
                updatePhysics(targetTimestep)
                withContext(Dispatchers.Main) {
                    invalidate()
                }
                delay((targetTimestep * 1000).toLong())
            }
            Log.d("ConstellationView", "Simulation loop ended")
        }
    }

    private fun stopSimulation() {
        if (simulationJob?.isActive == true) {
            Log.d("ConstellationView", "Stopping simulation job")
            simulationJob?.cancel()
        }
        simulationJob = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (nodes.isNotEmpty()) {
            startSimulation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSimulation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (nodes.isEmpty() || viewWidth == 0f || viewHeight == 0f) return

        val sortedNodes = nodes.sortedBy { it.timestamp.seconds }
        if (sortedNodes.size > 1) {
            for (i in 0 until sortedNodes.size - 1) {
                canvas.drawLine(
                    sortedNodes[i].x,
                    sortedNodes[i].y,
                    sortedNodes[i+1].x,
                    sortedNodes[i+1].y,
                    linePaint
                )
            }
        }

        nodes.forEach { node ->
            val bitmapToDraw = moodBitmaps[node.originalMoodLabel] ?: defaultBitmap
            if (bitmapToDraw != null) {
                val drawX = node.x - nodeBitmapWidth / 2f
                val drawY = node.y - nodeBitmapHeight / 2f
                canvas.drawBitmap(bitmapToDraw, drawX, drawY, nodePaint)
            } else {
                nodePaint.color = Color.DKGRAY
                canvas.drawCircle(node.x, node.y, nodeBitmapWidth / 2f, nodePaint)
            }
        }
    }
}