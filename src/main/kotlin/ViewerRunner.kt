import model.*
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PVector
import simulator.Model
import simulator.Simulation
import simulator.Vec3D
import simulator.clamp
import kotlin.random.Random


class CodeBallViewer: PApplet() {
    private val rules = Rules()
    private var game = reset(rules)
    private var simulation = Simulation(game, rules)
    private val myStrategy = MyStrategy()
    private val enemyStrategy = EnemyStrategy()
    private var rotation = PVector(PConstants.QUARTER_PI, 0f)
    private val rotationSpeed = 0.05f
    private val zoom = 1f
    private val zoomSpeed = 0.05f

    init {
        simulation.ballDistance = Double.POSITIVE_INFINITY
    }

    override fun settings() {
        size(600, 600, PConstants.P3D)
    }

    override fun draw() {
        game = simulation.toGame()

        for(me in game.robots) {
            val strategy = if(me.is_teammate) myStrategy else enemyStrategy
            val action = Action()
            strategy.act(me, rules, game, action)
            simulation.setAction(me.id, Model.AAction().copyFrom(action))
        }

        val goalScored = simulation.tick()
        if(goalScored) {
            simulation = Simulation(reset(rules), rules)
        }

        background(230)

        scale(1f, -1f)
        translate(width / 2f, -height / 2f)
        noStroke()

        scale(width / rules.arena.depth.toFloat())

        rotateX(rotation.x)
        rotateY(rotation.y)

        pushMatrix()
        fill(200)
        box(rules.arena.depth.toFloat(), 0.001f, rules.arena.width.toFloat())
        popMatrix()

        for (r in game.robots) {
            if(r.is_teammate) {
                fill(0f, 255f, 0f)
            } else {
                fill(255f, 0f, 0f)
            }

            pushMatrix()
            translate(
                    r.z.toFloat(),
                    r.y.toFloat(),
                    r.x.toFloat())

            val radius = r.radius.toFloat()
            sphere(radius)
            popMatrix()
        }

        fill(55f)
        pushMatrix()
        translate(
                game.ball.z.toFloat(),
                game.ball.y.toFloat(),
                game.ball.x.toFloat())

        val radius = game.ball.radius.toFloat()
        sphere(radius)
        popMatrix()

        if(keyPressed) {
            if(keyCode == UP)    rotation.x += rotationSpeed
            if(keyCode == DOWN)  rotation.x -= rotationSpeed
            if(keyCode == LEFT)  rotation.y += rotationSpeed
            if(keyCode == RIGHT) rotation.y -= rotationSpeed
        }
    }
}

fun reset(rules: Rules): Game {
    val ball = Ball()
    ball.y = Random.nextDouble(rules.arena.height / 2) + rules.BALL_RADIUS
    val robots = Array(rules.team_size * 2) {
        val robot = Robot()
        robot.is_teammate = it < rules.team_size
        robot.z = rules.arena.depth / 4 * if (robot.is_teammate) -1 else 1
        robot.x = rules.arena.width / 4 * ((it % 2) * 2 - 1)
        robot.y = rules.ROBOT_RADIUS
        robot
    }
    val nitroPacks = emptyArray<NitroPack>()
    val game = Game()
    game.ball = ball
    game.robots = robots
    game.current_tick = -1
    game.nitro_packs = nitroPacks
    return game
}


fun main() {
    PApplet.main("CodeBallViewer")
}

class EnemyStrategy : Strategy {
    override fun act(me: Robot, rules: Rules, game: Game, action: Action) {
        val isAttacker = game.robots.filter { !it.is_teammate }.sortedBy {
            (Model.MRobot(it, rules).position - Model.MBall(game.ball, rules).position).length
        }[0].id == me.id
        if (isAttacker) {
            val offset = Vec3D(game.ball.x - me.x, 0.0, game.ball.z - me.z).normalized * rules.ROBOT_MAX_GROUND_SPEED
            action.target_velocity_x = offset.x
            action.target_velocity_z = offset.z
            action.jump_speed = if (offset.length < rules.BALL_RADIUS * 3) rules.ROBOT_MAX_JUMP_SPEED else 0.0
            if (offset.length < rules.BALL_RADIUS * 4 && game.ball.z - me.z > rules.BALL_RADIUS * 2 && me.velocity_z > 0) {
                action.target_velocity_x = offset.x * 15
                action.target_velocity_z = -rules.ROBOT_MAX_GROUND_SPEED
                action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED
                action.use_nitro = true
            } else {
                action.target_velocity_x = if (0 < game.ball.z - me.z && game.ball.z - me.z < rules.BALL_RADIUS * 4) -offset.x * 5 else offset.x * 150
                action.target_velocity_z = if (0 < me.z - game.ball.z && me.z - game.ball.z < rules.BALL_RADIUS) -offset.z * 200 else offset.z * 200
                action.use_nitro = rules.BALL_RADIUS < game.ball.z - me.z && game.ball.z - me.z < rules.BALL_RADIUS * 4
            }
        } else {
            val offset = Vec3D(
                    clamp(
                            game.ball.x,
                            -rules.arena.goal_width / 2,
                            rules.arena.goal_width / 2) - me.x,
                    0.0,
                    (rules.arena.depth / 2 - rules.arena.goal_depth / 2) - me.z)
                    .normalized * rules.ROBOT_MAX_GROUND_SPEED * 0.8
            action.target_velocity_z = offset.z
            action.target_velocity_x = offset.x
        }
    }

    override fun customRendering() = ""
}

/*
import model.*
import com.corundumstudio.socketio.*
import com.corundumstudio.socketio.SocketIOServer
import kotlinx.coroutines.*
import org.jetbrains.kotlin.js.translate.context.generator.Rule
import simulator.Model
import simulator.Simulation
import simulator.Vec3D
import java.net.BindException
import java.nio.channels.ClosedChannelException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

fun main(args: Array<String>) {
    launchRunner()
}


const val historySize = 50


fun launchRunner() {
    val config = Configuration()
    config.hostname = "localhost"
    config.port = 8100

    val server = SocketIOServer(config)

    server.addConnectListener { client ->
        GlobalScope.launch {
            println("Connected to client ${client.sessionId}")

            val rules = Rules()
            var game = reset(rules)

            client.sendEvent("transfer", listOf(false, Options(
                    Field(
                            rules.arena.width,
                            rules.arena.depth
                    ),
                    BallOptions(
                            rules.BALL_RADIUS,
                            Color(
                                    255.0,
                                    255.0,
                                    255.0
                            )
                    ),
                    PlayerOptions(
                            rules.ROBOT_RADIUS
                    ),
                    listOf(
                            Color(
                                    255.0,
                                    0.0,
                                    0.0
                            ),
                            Color(
                                    0.0,
                                    255.0,
                                    0.0
                            )
                    ),
                    Gate(
                            Size(
                                    rules.arena.goal_width,
                                    rules.arena.goal_height,
                                    rules.arena.goal_depth
                            ),
                            Color(
                                    55.0,
                                    55.0,
                                    55.0
                            )
                    )
            )))
            println("Created initial options for ${client.sessionId}")

            client.sendEvent("transfer", listOf(true, listOf(GameState(game))))

            var simulation = Simulation(game, rules)
            simulation.ballDistance = Double.MAX_VALUE
            val history = Array(historySize) { game }

            val myStrategy = MyStrategy()

            println("Starting the simulation for ${client.sessionId}")
            while (true) {
                for (i in 0 until historySize) {
                    game = if(i > 0) history[i - 1] else history.last()

                    for (robot in simulation.robots) {
                        if (robot.isTeammate) {
                            val action = Action()
                            myStrategy.act(robot.toRobot(), rules, game, action)
                            simulation.setAction(robot.id, Model.AAction().copyFrom(action))
                        }
                    }

                    val gameEnded = simulation.tick()

                    if(gameEnded) {
                        game = reset(rules)
                        simulation = Simulation(game, rules)
                    }
                    history[i] = simulation.toGame()
                }
                try {
                    client.sendEvent("transfer", listOf(true, history.map { GameState(it) }))
                } catch (e: ClosedChannelException) {
                    println("Client ${client.sessionId} disconnected")
                    return@launch
                }
            }
        }
    }

    while (true) {
        try {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    server.stop()
                }
            })
            server.start()
            Thread.sleep(Long.MAX_VALUE)
        } catch (e: BindException) {
            println("Can't bind...")
            Thread.sleep(1_000)
        }
    }
}

data class Options(val field: Field, val ball: BallOptions, val player: PlayerOptions, val colors: List<Color>, val gate: Gate)
data class Field(val w: Double, val l: Double)
data class BallOptions(val r: Double, val color: Color)
data class PlayerOptions(val r: Double)
data class Gate(val size: Size, val color: Color)
data class Color(val r: Double, val g: Double, val b: Double)
data class Size(val w: Double, val h: Double, val d: Double)


data class GameState(val ball: BallState, val teams: List<Team>) {
    constructor(game: Game): this(
            BallState(game.ball),
            listOf(
                Team(game.robots.filter { !it.is_teammate }.map { Player(it) }.toList()),
                Team(game.robots.filter { it.is_teammate }.map { Player(it) }.toList())
            ))
}
data class BallState(val x: Double, val y: Double, val z: Double) {
    constructor(ball: Ball): this(ball.x, ball.y, ball.z)
}
data class Team(val players: List<Player>)
data class Player(val x: Double, val y: Double, val z: Double) {
    constructor(robot: Robot): this(robot.x, robot.y, robot.z)
}

fun reset(rules: Rules): Game {
    val ball = Ball()
    ball.y = Random.nextDouble(rules.arena.height / 2) + rules.BALL_RADIUS
    val robots = Array(rules.team_size * 2) {
        val robot = Robot()
        robot.is_teammate = it < rules.team_size
        robot.z = rules.arena.depth / 4 * if (robot.is_teammate) -1 else 1
        robot.x = rules.arena.width / 4 * ((it % 2) * 2 - 1)
        robot.y = rules.ROBOT_RADIUS
        robot
    }
    val nitroPacks = emptyArray<NitroPack>()
    var game = Game()
    game.ball = ball
    game.robots = robots
    game.current_tick = -1
    game.nitro_packs = nitroPacks
    return game
}
*/