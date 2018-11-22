
import io.vertx.core.AbstractVerticle
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.KeyEvent

/*
Robot.betterMouseMove is a function that's meant to fix Robot.mouseMove when dealing with Windows's
display scaling in order to make text easier to see. The 'scaleFactor' is the actual scaling factor
as a float > 1.0f.

This is the error vector field that represents the error caused by Robot.mouseMove in which to move
in the x and y direction towards its destination. It's not that useful in hindsight, but kind of cool
that one can model the error as a mathematical function.

"Error" Vector Field: F(x, y) = f(x, y) * i + g(x, y) * j
    f(x, y) = (-x + t_x) / (width - t_x)
    g(x, y) = (-y + t_y) / (height - t_y)
    x = x coordinate of origin point
    y = y coordinate of origin point
    width = width of screen
    height = height of screen
    t_x = x coordinate of target point
    t_y = x coordinate of target point

    - Note: f(x, y) and g(x, y) will return the negative error result, which represents the
        "restoring" error quantity required to reach the target.

    -f(x, y) = actual error amount for x direction
    -g(x, y) = actual error amount for y direction

    s = scaling factor of screen (i.e. 125% => s = 1.25)
    G(x, y) = predicted output coords (as a vector)
    G(x, y) = ((-x + t_x) * s) * i + (2 * t_y) * ((-y + t_y) * s) * j
        Note: (2 * t_y) is needed to translate from cartesian coords to screen coords
*/

const val DEFAULT_SCALE_FACTOR: Float = 1.25f

fun mouseLoc(): Point = MouseInfo.getPointerInfo().location

fun Robot.betterMouseMove(x: Int, y: Int, scaleFactor: Float = DEFAULT_SCALE_FACTOR) {
    val modFactor = 1 - (1 / scaleFactor)
    val origin = mouseLoc()

    val deltaX = x - origin.x
    val deltaY = y - origin.y

    val finalX = (x - deltaX * modFactor).toInt()
    val finalY = (y - deltaY * modFactor).toInt()

    this.mouseMove(finalX, finalY)
}

class Server : AbstractVerticle() {
    private val rightJoystickThresh = 50 // C Stick
    private val screenSize = Toolkit.getDefaultToolkit().screenSize
    private val centerOfScreen = Point(screenSize.width / 2, screenSize.height / 2 - 30)
    private val width2HeightRatio = screenSize.width.toFloat() / screenSize.height.toFloat()
    private val cmdSeparator = ":"
    private val valSeparator = "="
    private val vertRegex = Regex("V=(-?\\d\\.?(\\d+)?)")
    private val horizRegex = Regex("H=(-?\\d\\.?(\\d+)?)")
    private val robot = Robot()
    private var lastRecordedLeftLoc = Point(centerOfScreen)
    private var lastRecordedRightLoc = Point(50, 50)

    // parses a "0" or "1" (as a string) to a boolean
    private fun parseBool(input: String): Boolean = input == "1"

    private fun attemptKeyToggle(keyCode: Int, isOn: Boolean, inputType: String, unknownParam: String) {
        if (keyCode != -1)
            if (isOn) robot.keyPress(keyCode) else robot.keyRelease(keyCode)
        else
            println("Warning: Unknown button found in $inputType command: '$unknownParam'")
    }

    private fun parseControllerInput(it: String) {
        val (inputType, params) = it.split(cmdSeparator)
        when (inputType) {
            "BTN" -> {
                val (button, state) = params.split(valSeparator)
                val isOn = parseBool(state)

                val keyCode = when (button) {
                    "A" -> KeyEvent.VK_X  // A -> X
                    "B" -> KeyEvent.VK_C  // B -> C -> X
                    "X" -> KeyEvent.VK_Z  // X -> Z -> B
                    "Y" -> KeyEvent.VK_S  // Y -> S
                    else -> -1
                }

                attemptKeyToggle(keyCode, isOn, inputType, button)
            }
            "DPAD" -> {
                val (button, state) = params.split(valSeparator)
                val isOn = parseBool(state)

                val keyCode = when (button) {
                    "U" -> KeyEvent.VK_T  // UP    -> T
                    "D" -> KeyEvent.VK_G  // DOWN  -> G
                    "L" -> KeyEvent.VK_F  // LEFT  -> F
                    "R" -> KeyEvent.VK_H  // RIGHT -> H
                    else    -> -1
                }

                attemptKeyToggle(keyCode, isOn, inputType, button)
            }
            "TRIG" -> {
                val (button, state) = params.split(valSeparator)
                val isOn = parseBool(state)

                val keyCode = when (button) {
                    "L" -> KeyEvent.VK_Q  // LEFT  -> Q -> START
                    "R" -> KeyEvent.VK_D  // RIGHT -> D -> Z
                    else    -> -1
                }

                attemptKeyToggle(keyCode, isOn, inputType, button)
            }
            "JOY-L" -> {
                val vert = vertRegex.find(params)?.groupValues?.get(1)?.toFloat()
                val horiz = horizRegex.find(params)?.groupValues?.get(1)?.toFloat()

                // sign added (-vert) cuz of screen coords
                if (vert != null)
                    lastRecordedLeftLoc.y = (centerOfScreen.y + 320 * -vert).toInt()
                if (horiz != null)
                    lastRecordedLeftLoc.x = (centerOfScreen.x + 320 * horiz * width2HeightRatio).toInt()

                robot.betterMouseMove(
                    x = lastRecordedLeftLoc.x,
                    y = lastRecordedLeftLoc.y
                )
            }
            "JOY-R" -> {
                val vert = vertRegex.find(params)?.groupValues?.get(1)?.toFloat()
                val horiz = horizRegex.find(params)?.groupValues?.get(1)?.toFloat()

                // sign added (-vert) cuz of screen coords
                if (vert != null)
                    lastRecordedRightLoc.y = (vert * 100).toInt()
                if (horiz != null)
                    lastRecordedRightLoc.x = (horiz * 100).toInt()

                val up = KeyEvent.VK_I
                val down = KeyEvent.VK_K
                val left = KeyEvent.VK_J
                val right = KeyEvent.VK_L

                when {
                    lastRecordedRightLoc.y >= rightJoystickThresh -> {
                        robot.keyRelease(down)
                        robot.keyPress(up)
                    }
                    lastRecordedRightLoc.y <= -rightJoystickThresh -> {
                        robot.keyRelease(up)
                        robot.keyPress(down)
                    }
                    else -> {
                        robot.keyRelease(up)
                        robot.keyRelease(down)
                    }
                }

                when {
                    lastRecordedRightLoc.x >= rightJoystickThresh -> {
                        robot.keyRelease(left)
                        robot.keyPress(right)
                    }
                    lastRecordedRightLoc.x <= -rightJoystickThresh -> {
                        robot.keyRelease(right)
                        robot.keyPress(left)
                    }
                    else -> {
                        robot.keyRelease(right)
                        robot.keyRelease(left)
                    }
                }
            }
            "BRAKE" -> {
                val (side, valueStr) = params.split(valSeparator)
                val isOn = valueStr.toFloat() != 0f

                val keyCode = when (side) {
                    "L"     -> KeyEvent.VK_P    // LEFT  -> P
                    "R"     -> KeyEvent.VK_O    // RIGHT -> O
                    else    -> -1
                }

                attemptKeyToggle(keyCode, isOn, inputType, side)
            }
            else -> {
                println("Received unknown input: '$it'")
            }
        }
    }

    override fun start() {
        vertx
            .createHttpServer()
            .websocketHandler { ws ->
                robot.betterMouseMove(screenSize.width / 2, screenSize.height / 2)

                ws.textMessageHandler {
                    println(it)
                    if (it == "I am here!")
                        ws.writeTextMessage("I got you!")
                    else
                        for (cmd in it.split(";"))
                            parseControllerInput(cmd)
                }

                ws.closeHandler {
                    println("A client has disconnected!")
                }

                ws.exceptionHandler {
                    it.printStackTrace()
                }
            }.listen(9001)
    }
}