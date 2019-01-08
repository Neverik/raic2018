package simulator

import model.Rules

open class Entity(
        var position: Vec3D,
        var velocity: Vec3D,
        var radius: Double,
        var mass: Double,
        var radiusChangeSpeed: Double,
        var rules: Rules,
        var touch: Boolean,
        var arenaE: Double,
        var active: Boolean = true) {

    fun collide(b: Entity) {
        if(active) {
            val deltaPosition = b.position - position
            val distance = deltaPosition.length
            val penetration = radius + b.radius - distance
            if (penetration >= 0) {
                val ka = (1 / mass) / ((1 / mass) + (1 / b.mass))
                val kb = (1 / b.mass) / ((1 / mass) + (1 / b.mass))
                val normal = deltaPosition.normalized
                position -= normal * penetration * ka
                b.position += normal * penetration * kb
                val deltaVelocity = (b.velocity - velocity) dot normal
                -b.radiusChangeSpeed - radiusChangeSpeed
                if (deltaVelocity <= 0) {
                    val impulse = normal * deltaVelocity * rules.MIN_HIT_E //(Random.nextDouble(rules.MIN_HIT_E + 1, rules.MAX_HIT_E + 1))
                    velocity += impulse * ka
                    b.velocity -= impulse * kb
                }
            }
        }
    }

    fun move(deltaTime: Double) {
        if(active) {
            velocity = velocity.clamp(rules.MAX_ENTITY_SPEED)
            position += velocity * deltaTime
            position.y -= rules.GRAVITY * deltaTime * deltaTime / 2
            velocity.y -= rules.GRAVITY * deltaTime
        }
    }

    fun collideWithArena(): Vec3D? {
        if(active) {
            val collision = ArenaCollision.danToArena(position, rules.arena)
            val distance = collision.distance
            val normal = collision.normal
            val penetration = radius - distance
            if (penetration >= 0) {
                position += normal * penetration
                val vel = (velocity dot normal) - radiusChangeSpeed
                if (vel <= 0) {
                    velocity -= normal * (1 + arenaE) * vel
                    return normal
                }
            }
        }
        return null
    }

    fun calculateActivityByDistance(ball: Entity, distance: Double) {
        active = (ball.position - position).length < distance
    }
}