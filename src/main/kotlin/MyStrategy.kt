import model.*
import simulator.*
import kotlin.system.measureNanoTime

class MyStrategy : Strategy {
    var drawObjects: MutableList<String> = mutableListOf()
    val timer: Timer = Timer()
    var tick = 0

    override fun act(me: Robot, rules: Rules, game: Game, action: Action) {
        /*val isAttacker = game.robots.filter { it.is_teammate }.sortedBy {
            (RRobot(it, rules).position - BBall(game.ball, rules).position).length
        }[0].id == me.id

        if(isAttacker) {
            val offset = Vec3D(game.ball.x - me.x, 0.0, game.ball.z - me.z).normalized * rules.ROBOT_MAX_GROUND_SPEED
            action.target_velocity_z = offset.z
            action.target_velocity_x = offset.x
            if(Vec3D(game.ball.x - me.x, 0.0, game.ball.z - me.z).length < rules.BALL_RADIUS * 4) {
                action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED
                action.use_nitro = true
            }
        } else {
            val offset = Vec3D(
                    game.ball.x.clamp(
                            -rules.arena.goal_width / 2,
                            rules.arena.goal_width / 2) - me.x,
                    0.0,
                    (-rules.arena.depth / 2 + rules.arena.goal_depth / 2) - me.z)
                        .normalized * rules.ROBOT_MAX_GROUND_SPEED * 0.8
            if(Vec3D(game.ball.x - me.x, 0.0, game.ball.z - me.z).length < rules.BALL_RADIUS * 3) {
                // TODO
                action.target_velocity_z = offset.z
                action.target_velocity_x = offset.x
            } else {
                action.target_velocity_z = offset.z
                action.target_velocity_x = offset.x
            }
        }*/
        if(tick != game.current_tick) {
            val sim = Simulation(game, rules)
            timer.measure(100) {
                for(n in 0 until 100) {
                    sim.tick()
                    drawSphere(sim.ball.position.x, sim.ball.position.y, sim.ball.position.z)
                }
            }
            tick = game.current_tick
        }
        println(timer.time)
        /*if(game.current_tick == 500) {
            println(timer.time)
            println(2 / 0)
        }*/
    }

    fun drawSphere(x: Double, y: Double, z: Double) {
        drawObjects.add(buildString {
            append("{\"Sphere\":{")

            append("\"x\":")
            append(x)
            append(",\"y\":")
            append(y)
            append(",\"z\":")
            append(z)

            append(",\"r\":0,\"g\":0,\"b\":1,\"a\":0.5,\"radius\":1}}")
        })
    }

    fun drawLine(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double) {
        drawObjects.add(buildString {
            append("{\"Line\":{")

            append("\"x1\":")
            append(x1)
            append(",\"y1\":")
            append(y1)
            append(",\"z1\":")
            append(z1)

            append("\"x2\":")
            append(x2)
            append(",\"y2\":")
            append(y2)
            append(",\"z2\":")
            append(z2)

            append(",\"r\":0,\"g\":0,\"b\":1,\"a\":0.5,\"width\":1}}")
        })
    }

    override fun customRendering(): String = buildString {
        append('[')
        for((i, obj) in drawObjects.withIndex()) {
            if(i != 0) append(',')
            append(obj)
        }
        drawObjects.clear()
        append(']')
    }
}


class Timer(var total: Double = 0.0, var count: Int = 0) {
    inline fun measure(func: () -> Unit) {
        total += measureNanoTime(func)
        count++
    }

    inline fun measure(numberOfIterations:Int, func: () -> Unit) {
        total += measureNanoTime(func)
        count += numberOfIterations
    }

    fun reset() {
        total = 0.0
        count = 0
    }

    val time: Double
        get() = total / count
}
