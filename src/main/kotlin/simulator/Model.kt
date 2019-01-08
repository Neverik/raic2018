package simulator

import model.*

class Model {
    data class AAction(var targetVelocity: Vec3D = Vec3D(), var jumpSpeed: Double = 0.0, var useNitro: Boolean = false) {
        fun copyFrom(action: Action): AAction {
            targetVelocity = Vec3D(action.target_velocity_x, action.target_velocity_y, action.target_velocity_z)
            jumpSpeed = action.jump_speed
            useNitro = action.use_nitro
            return this
        }
    }

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
        val id = robot.id
        val isTeammate = robot.is_teammate
        val playerID = robot.player_id

        fun toRobot(): Robot {
            val robot = Robot()

            robot.x = position.x
            robot.y = position.y
            robot.z = position.z

            robot.velocity_x = velocity.x
            robot.velocity_y = velocity.y
            robot.velocity_z = velocity.z

            robot.touch = touch
            robot.touch_normal_x = touchNormal.x
            robot.touch_normal_y = touchNormal.y
            robot.touch_normal_z = touchNormal.z

            robot.id = id
            robot.is_teammate = isTeammate
            robot.radius = radius
            robot.player_id = playerID

            return robot
        }
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