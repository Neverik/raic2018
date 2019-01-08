import model.*
import processing.core.*
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
    private val rotationSpeed = 0.07f
    private var zoom = 0f
    private val zoomSpeed = 7f
    private lateinit var arenaShape: PShape
    private val arenaPath = "/home/neverik/IdeaProjects/raic2018/arena.obj"

    override fun settings() {
        size(600, 600, PConstants.P3D)
    }

    override fun setup() {
        simulation.ballDistance = Double.POSITIVE_INFINITY
        arenaShape = loadShape(arenaPath)
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

        translate(0f, 0f, zoom)
        scale(width / rules.arena.depth.toFloat())

        rotateX(rotation.x)
        rotateY(rotation.y)

        directionalLight(126f, 126f, 126f, 0f, 1f, 0f)
        directionalLight(126f, 126f, 126f, -1f, 0f, 0f)
        directionalLight(126f, 126f, 126f, 1f, 1f, 0f)
        ambientLight(55f, 55f, 55f)
        ambient(255)
        arenaShape.setFill(color(55f, 155f, 255f))
        shape(arenaShape)

        hint(DISABLE_DEPTH_TEST)
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

        fill(220f)
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

            if(key == 'w' || key == 'W') zoom += zoomSpeed
            if(key == 's' || key == 'S') zoom -= zoomSpeed
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
