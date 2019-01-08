package simulator

import model.*
import kotlin.math.*

// import kotlin.random.Random

data class Simulation(
        var ball: Model.MBall,
        var robots: Array<Model.MRobot>,
        var nitroPacks: Array<Model.MNitroPack>,
        var rules: Rules,
        var currentTick: Int,
        var ballDistance: Double = rules.arena.depth / 3) {
    private val deltaTime = 1.0 / (rules.TICKS_PER_SECOND * rules.MICROTICKS_PER_TICK)

    constructor(game: Game, rules: Rules) : this(
            Model.MBall(game.ball, rules),
            Array(game.robots.size) { Model.MRobot(game.robots[it], rules) },
            Array(game.nitro_packs.size) { Model.MNitroPack(game.nitro_packs[it]) },
            rules,
            game.current_tick)

    fun tick(simulateRobots: Boolean = true): Boolean {
        var goalScored = false

        if(simulateRobots) {
            for (robot in robots) {
                robot.calculateActivityByDistance(ball, ballDistance)
            }
        }

        for (i in 0 until rules.MICROTICKS_PER_TICK) {
            if(update(simulateRobots)) {
                goalScored = true
            }
        }

        for (pack in nitroPacks) {
            if (pack.alive) {
                continue
            }
            pack.respawnTicks -= 1
            if (pack.respawnTicks == 0) {
                pack.alive = true
            }
        }

        currentTick++
        return goalScored
    }

    private fun update(simulateRobots: Boolean): Boolean {
        // robots.shuffle()
        if(simulateRobots) {
            for (robot in robots) {
                if (robot.touch && robot.active) {
                    var targetVelocity = robot.action.targetVelocity.clamp(rules.ROBOT_MAX_GROUND_SPEED)
                    targetVelocity -= robot.touchNormal * (robot.touchNormal dot targetVelocity)
                    val targetVelocityChange = targetVelocity - robot.velocity
                    if (targetVelocityChange.length > 0) {
                        val acceleration = rules.ROBOT_ACCELERATION * max(0.0, robot.touchNormal.y)
                        robot.velocity += (
                                targetVelocityChange
                                        .normalized
                                        * acceleration
                                        * deltaTime)
                                .clamp(targetVelocityChange.length)
                    }
                }
                if (robot.action.useNitro && robot.active) {
                    val targetVelocityChange = (
                            robot.action.targetVelocity
                                    - robot.velocity)
                            .clamp(robot.nitro * rules.NITRO_POINT_VELOCITY_CHANGE)
                    if (targetVelocityChange.length > 0) {
                        val acceleration = targetVelocityChange.normalized * rules.ROBOT_NITRO_ACCELERATION
                        val velocityChange =
                                (acceleration * deltaTime)
                                .clamp(targetVelocityChange.length)
                        robot.velocity += velocityChange
                        robot.nitro -= velocityChange.length / rules.NITRO_POINT_VELOCITY_CHANGE
                    }
                }
                robot.move(deltaTime)
                robot.radius = (
                        rules.ROBOT_MIN_RADIUS
                                + (rules.ROBOT_MAX_RADIUS - rules.ROBOT_MIN_RADIUS)
                                * robot.action.jumpSpeed
                                / rules.ROBOT_MAX_JUMP_SPEED)
                robot.radiusChangeSpeed = robot.action.jumpSpeed
            }

            for (i in 0 until robots.size) {
                for (j in 0 until i) {
                    robots[i].collide(robots[j])
                }
            }
        }

        ball.move(deltaTime)

        if(simulateRobots) {
            for (robot in robots) {
                robot.collide(ball)
                val collisionNormal = robot.collideWithArena()
                if (collisionNormal == null) {
                    robot.touch = false
                } else {
                    robot.touch = true
                    robot.touchNormal = collisionNormal
                }
            }
        }

        ball.collideWithArena()
        val goalScored = abs(ball.position.z) > rules.arena.depth / 2 + ball.radius

        if(simulateRobots) {
            for (robot in robots) {
                if (robot.nitro == rules.MAX_NITRO_AMOUNT) {
                    continue
                }
                nitro@ for (pack in nitroPacks) {
                    if (!pack.alive) {
                        continue@nitro
                    }
                    if ((robot.position - pack.position).length <= robot.radius + pack.radius) {
                        robot.nitro = pack.nitroAmount
                        pack.alive = false
                        pack.respawnTicks = rules.NITRO_PACK_RESPAWN_TICKS
                    }
                }
            }
        }

        return goalScored
    }

    fun setAction(id: Int, action: Model.AAction) {
        for(robot in robots) {
            if(robot.id == id) {
                robot.action = action
            }
        }
    }

    val reward: Double
        get() {
            return (ball.position.z / rules.arena.depth / 2 + 0.5) * 100 + (Array(rules.team_size) {
                (robots.filter { i -> i.isTeammate }[it].position - ball.position).length
            }.sumByDouble { it } - Array(rules.team_size) {
                (robots.filter { i -> !i.isTeammate }[it].position - ball.position).length
            }.sumByDouble { it }) +
                    (if(ball.position.z > rules.arena.depth / 2 + ball.radius) 1000 else 0) -
                    (if(ball.position.z < rules.arena.depth / 2 - ball.radius) 1000 else 0)
        }

    fun toGame(): Game {
        val game = Game()

        game.robots = Array(robots.size) {robots[it].toRobot()}

        game.ball = Ball()
        game.ball.x = ball.position.x
        game.ball.y = ball.position.y
        game.ball.z = ball.position.z
        game.ball.velocity_x = ball.velocity.x
        game.ball.velocity_y = ball.velocity.y
        game.ball.velocity_z = ball.velocity.z
        game.ball.radius = ball.radius

        game.current_tick = currentTick

        game.players = arrayOf()
        game.nitro_packs = arrayOf()

        return game
    }
}
