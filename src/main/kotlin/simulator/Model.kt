package simulator

import model.Ball
import model.NitroPack
import model.Robot
import model.Rules

class Model {
    class AAction(var targetVelocity: Vec3D = Vec3D(), var jumpSpeed: Double = 0.0, var useNitro: Boolean = false)

    class MRobot(robot: Robot, rules: Rules) : Entity(
            position = Vec3D(robot.x, robot.y, robot.z),
            velocity = Vec3D(robot.velocity_x, robot.velocity_y, robot.velocity_z),
            radius = rules.ROBOT_RADIUS,
            mass = rules.ROBOT_MASS,
            radiusChangeSpeed = 0.0,
            rules = rules,
            touch = true,
            arenaE = rules.ROBOT_ARENA_E) {
        var touchNormal: Vec3D = Vec3D()
        var nitro: Double = rules.START_NITRO_AMOUNT
        var action: AAction = AAction()
    }

    class MBall(ball: Ball, rules: Rules) : Entity(
            position = Vec3D(ball.x, ball.y, ball.z),
            velocity = Vec3D(ball.velocity_x, ball.velocity_y, ball.velocity_z),
            radius = rules.BALL_RADIUS,
            mass = rules.BALL_MASS,
            radiusChangeSpeed = 0.0,
            rules = rules,
            touch = false,
            arenaE = rules.BALL_ARENA_E
    )

    class MNitroPack(nitroPack: NitroPack) {
        val position: Vec3D = Vec3D(nitroPack.x, nitroPack.y, nitroPack.z)
        val radius = nitroPack.radius
        val nitroAmount = nitroPack.nitro_amount
        var respawnTicks = nitroPack.respawn_ticks ?: 0
        var alive = true
    }

}