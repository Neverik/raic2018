package model

class Rules {
    /*
    */

    var max_tick_count: Int = 0
    var arena: Arena = Arena()
    var team_size: Int = 1
    var seed: Long = 0
    var ROBOT_MIN_RADIUS: Double = 1.0
    var ROBOT_MAX_RADIUS: Double = 1.05
    var ROBOT_MAX_JUMP_SPEED: Double = 15.0
    var ROBOT_ACCELERATION: Double = 100.0
    var ROBOT_NITRO_ACCELERATION: Double = 30.0
    var ROBOT_MAX_GROUND_SPEED: Double = 30.0
    var ROBOT_ARENA_E: Double = 0.0
    var ROBOT_RADIUS: Double = 1.0
    var ROBOT_MASS: Double = 2.0
    var TICKS_PER_SECOND: Int = 60
    var MICROTICKS_PER_TICK: Int = 100
    var RESET_TICKS: Int = 2 * TICKS_PER_SECOND
    var BALL_ARENA_E: Double = 0.7
    var BALL_RADIUS: Double = 2.0
    var BALL_MASS: Double = 1.0
    var MIN_HIT_E: Double = 0.4
    var MAX_HIT_E: Double = 0.5
    var MAX_ENTITY_SPEED: Double = 100.0
    var MAX_NITRO_AMOUNT: Double = 100.0
    var START_NITRO_AMOUNT: Double = 50.0
    var NITRO_POINT_VELOCITY_CHANGE: Double = 0.6
    var NITRO_PACK_X: Double = 20.0
    var NITRO_PACK_Y: Double = 1.0
    var NITRO_PACK_Z: Double = 30.0
    var NITRO_PACK_RADIUS: Double = 0.5
    var NITRO_PACK_AMOUNT: Double = 100.0
    var NITRO_PACK_RESPAWN_TICKS: Int = 10 * TICKS_PER_SECOND
    var GRAVITY: Double = 30.0
}
