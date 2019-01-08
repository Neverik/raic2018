import model.*
import simulator.*
import kotlin.math.*
import kotlin.system.measureNanoTime

class MyStrategy : Strategy {
    private var drawObjects: MutableList<String> = mutableListOf()
    private val tryDirections = 7
    private val tryDirectionsVertical = 3
    private val calculateSimulationPeriod = 1
    private val calculateSimulationTicks = 50
    private val simulationFPS = 30
    private val simulationMicroticks = 2
    private var lastAction: Action = Action()
    private val viewArea = PI * 0.75
    private val lowresSimLength = 20
    private val lowresSimMicro = 2
    private val lowresSimFPS = 6
    private val simTrigger = 14.0
    private val jumpDelay = 20

    override fun act(me: Robot, rules: Rules, game: Game, action: Action) {
        /*val sim = Simulation(game, rules)

        val simBall = Simulation(game, rules)
        simBall.rules.MICROTICKS_PER_TICK = lowresSimMicro
        simBall.rules.TICKS_PER_SECOND = lowresSimFPS
        for (i in 0 until lowresSimLength) {
            val oldBall = simBall.ball.position.copy()
            simBall.tick()
            drawLine(oldBall, simBall.ball.position, 3f)
        }
        val lowResBallPos = simBall.ball.position

        if((sim.ball.position.copy(y = 0.0) - Vec3D(me.x, 0.0, me.z)).length < simTrigger) {
            if(game.current_tick % calculateSimulationPeriod != 0) {
                action.copyFrom(Model.AAction().copyFrom(lastAction))
                return
            }

            val ballAngle = atan2(game.ball.z - me.z, game.ball.x - me.x)
            val horDirections = Array(tryDirections) {
                val angle = it * viewArea / tryDirections.toDouble() + ballAngle - viewArea / 2
                Vec3D(cos(angle), 0.0, sin(angle)) * rules.ROBOT_MAX_GROUND_SPEED
            }
            val directions = Array(tryDirections * tryDirectionsVertical) {
                val base = horDirections[it % tryDirections]
                base.copy(y = floor(it.toDouble() / tryDirections) / max(tryDirectionsVertical - 1, 1))
            }

            var maxReward = Double.NEGATIVE_INFINITY
            var bestAction = Model.AAction().copyFrom(lastAction)

            for (dir in directions) {
                val simulation = Simulation(game, rules)
                simulation.rules.TICKS_PER_SECOND = simulationFPS
                simulation.rules.MICROTICKS_PER_TICK = simulationMicroticks

                val selectedAction = Model.AAction(dir)

                var lastPosition = simulation.robots.filter { it.id == me.id }[0].position
                var lastBall = simulation.ball.position
                val startTick = simulation.currentTick

                for(i in 0 until calculateSimulationTicks) {
                    for(r in game.robots) {
                        if (r.id != me.id) {
                            val newAction = Action()
                            baseline(r, rules, game, action, lowResBallPos)

                            simulation.setAction(r.id, Model.AAction(
                                    Vec3D(
                                            newAction.target_velocity_x,
                                            newAction.target_velocity_y,
                                            newAction.target_velocity_z),
                                    newAction.jump_speed,
                                    newAction.use_nitro))
                        }
                    }

                    if(simulation.currentTick == startTick + jumpDelay) {
                        selectedAction.jumpSpeed = dir.y * rules.ROBOT_MAX_JUMP_SPEED
                    }

                    simulation.setAction(me.id, selectedAction)
                    simulation.tick()

                    val newPosition = simulation.robots.filter { it.id == me.id }[0].position
                    drawLine(lastPosition, newPosition, 0.5f)
                    lastPosition = newPosition

                    val newBall = simulation.ball.position
                    drawLine(lastBall, newBall, 5f)
                    lastBall = newBall
                }

                val reward = simulation.reward
                if (reward > maxReward) {
                    maxReward = reward
                    bestAction = selectedAction
                }
            }

            action.copyFrom(bestAction)
        } else {
            baseline(me, rules, game, action, lowResBallPos)
        }

        lastAction = action*/
        baseline(me, rules, game, action, Model.MBall(game.ball, rules).position)
    }

    private fun baseline(me: Robot, rules: Rules, game: Game, action: Action, ballPrediction: Vec3D) {
        val isAttacker = game.robots.filter { it.is_teammate }.sortedBy {
            (Model.MRobot(it, rules).position - Model.MBall(game.ball, rules).position).length
        }[0].id == me.id
        if(game.ball.z < -rules.arena.depth / 2 && ballPrediction != Model.MBall(game.ball, rules).position && isAttacker) {
            baseline(me, rules, game, action, Model.MBall(game.ball, rules).position)
        }
        if(isAttacker) {
            val offset = Vec3D(ballPrediction.x - me.x, 0.0, ballPrediction.z - me.z).normalized * rules.ROBOT_MAX_GROUND_SPEED
            action.target_velocity_x = offset.x
            action.target_velocity_z = offset.z
            action.jump_speed = if (offset.length < rules.BALL_RADIUS * 3) rules.ROBOT_MAX_JUMP_SPEED else 0.0
            if(offset.length < rules.BALL_RADIUS * 4 && game.ball.z - me.z > rules.BALL_RADIUS * 2 && me.velocity_z > 0) {
                action.target_velocity_x = offset.x * 15
                action.target_velocity_z = -rules.ROBOT_MAX_GROUND_SPEED
                action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED
                action.use_nitro = true
            } else {
                action.target_velocity_x = if(0 < me.z - game.ball.z && me.z - game.ball.z < rules.BALL_RADIUS * 4) -offset.x * 5 else offset.x * 150
                action.target_velocity_z = if(0 < game.ball.z - me.z && game.ball.z - me.z < rules.BALL_RADIUS) -offset.z * 200 else offset.z * 200
                action.use_nitro = rules.BALL_RADIUS < game.ball.z - me.z && game.ball.z - me.z < rules.BALL_RADIUS * 4
            }
        } else {
            val offset = Vec3D(
                    clamp(
                            game.ball.x,
                            -rules.arena.goal_width / 2,
                            rules.arena.goal_width / 2) - me.x,
                    0.0,
                    (-rules.arena.depth / 2 + rules.arena.goal_depth / 2) - me.z)
                    .normalized * rules.ROBOT_MAX_GROUND_SPEED * 0.8
            action.target_velocity_z = offset.z
            action.target_velocity_x = offset.x
        }
    }

    fun drawSphere(p: Vec3D) {
        drawObjects.add(buildString {
            append("{\"Sphere\":{")

            append("\"x\":")
            append(p.x)
            append(",\"y\":")
            append(p.y)
            append(",\"z\":")
            append(p.z)

            append(",\"r\":0,\"g\":0,\"b\":1,\"a\":0.5,\"radius\":1}}")
        })
    }

    fun drawLine(p1: Vec3D, p2: Vec3D, width: Float = 1f) {
        drawObjects.add(buildString {
            append("{\"Line\":{")

            append("\"x1\":${p1.x},\"y1\":${p1.y},\"z1\":${p1.z}")
            append(",\"x2\":${p2.x},\"y2\":${p2.y},\"z2\":${p2.z}")

            append(",\"r\":0.0,\"g\":0.0,\"b\":1.0,\"a\":0.5,\"width\":$width}}")
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

fun Action.copyFrom(action: Model.AAction) {
    target_velocity_x = action.targetVelocity.x
    target_velocity_y = action.targetVelocity.y
    target_velocity_z = action.targetVelocity.z
    jump_speed = action.jumpSpeed
    use_nitro = action.useNitro
}
