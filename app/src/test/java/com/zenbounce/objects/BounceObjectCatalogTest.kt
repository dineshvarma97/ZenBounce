package com.zenbounce.objects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for [BounceObjectCatalog].
 * Verifies that every catalog entry is well-formed and physics values are in valid ranges.
 */
class BounceObjectCatalogTest {

    @Test
    fun `all catalog entries have unique IDs`() {
        val ids = BounceObjectCatalog.ALL.map { it.id }
        assertEquals("Catalog IDs must be unique", ids.distinct().size, ids.size)
    }

    @Test
    fun `all catalog entries have non-empty names`() {
        BounceObjectCatalog.ALL.forEach { obj ->
            assertTrue("name must not be blank for ${obj.id}", obj.name.isNotBlank())
            assertTrue("displayName must not be blank for ${obj.id}", obj.displayName.isNotBlank())
        }
    }

    @Test
    fun `all catalog entries have positive mass`() {
        BounceObjectCatalog.ALL.forEach { obj ->
            assertTrue(
                "mass must be > 0 for ${obj.displayName}, was ${obj.physics.mass}",
                obj.physics.mass > 0f
            )
        }
    }

    @Test
    fun `all catalog entries have restitution in valid range`() {
        BounceObjectCatalog.ALL.forEach { obj ->
            assertTrue(
                "restitution must be in 0..1 for ${obj.displayName}, was ${obj.physics.restitution}",
                obj.physics.restitution in 0f..1f
            )
        }
    }

    @Test
    fun `all catalog entries have airDamping in valid range`() {
        BounceObjectCatalog.ALL.forEach { obj ->
            assertTrue(
                "airDamping must be in 0..1 for ${obj.displayName}, was ${obj.physics.airDamping}",
                obj.physics.airDamping in 0f..1f
            )
        }
    }

    @Test
    fun `all catalog entries have positive radius`() {
        BounceObjectCatalog.ALL.forEach { obj ->
            assertTrue(
                "radius must be > 0 for ${obj.displayName}, was ${obj.visuals.radius}",
                obj.visuals.radius > 0f
            )
        }
    }

    @Test
    fun `byId returns correct object for each catalog entry`() {
        BounceObjectCatalog.ALL.forEach { obj ->
            val found = BounceObjectCatalog.byId(obj.id)
            assertEquals("byId should return the correct object for id=${obj.id}", obj.id, found.id)
        }
    }

    @Test
    fun `byId returns DEFAULT for unknown ID`() {
        val result = BounceObjectCatalog.byId(Int.MAX_VALUE)
        assertEquals("Unknown ID should fall back to DEFAULT", BounceObjectCatalog.DEFAULT.id, result.id)
    }

    @Test
    fun `ping pong is lighter than tennis ball`() {
        assertTrue(
            "Ping pong mass should be less than tennis ball mass",
            BounceObjectCatalog.PING_PONG.physics.mass < BounceObjectCatalog.TENNIS_BALL.physics.mass
        )
    }

    @Test
    fun `tennis ball is lighter than football`() {
        assertTrue(
            "Tennis ball mass should be less than football mass",
            BounceObjectCatalog.TENNIS_BALL.physics.mass < BounceObjectCatalog.FOOTBALL.physics.mass
        )
    }

    @Test
    fun `ping pong has highest restitution (most bouncy)`() {
        val maxRestitution = BounceObjectCatalog.ALL.maxOf { it.physics.restitution }
        assertEquals(
            "Ping pong should be the bounciest object",
            maxRestitution,
            BounceObjectCatalog.PING_PONG.physics.restitution
        )
    }

    @Test
    fun `football has smallest radius among non-default objects`() {
        // Football is the biggest — it should have the largest radius in the catalog
        val maxRadius = BounceObjectCatalog.ALL.maxOf { it.visuals.radius }
        assertEquals(
            "Football should have the largest radius",
            maxRadius,
            BounceObjectCatalog.FOOTBALL.visuals.radius
        )
    }

    @Test
    fun `ObjectPhysics init rejects zero mass`() {
        var threw = false
        try {
            ObjectPhysics(mass = 0f, restitution = 0.5f, airDamping = 0.9f)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("ObjectPhysics should reject zero mass", threw)
    }

    @Test
    fun `ObjectPhysics init rejects restitution above 1`() {
        var threw = false
        try {
            ObjectPhysics(mass = 1f, restitution = 1.1f, airDamping = 0.9f)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("ObjectPhysics should reject restitution > 1", threw)
    }

    @Test
    fun `ObjectPhysics init rejects negative airDamping`() {
        var threw = false
        try {
            ObjectPhysics(mass = 1f, restitution = 0.5f, airDamping = -0.1f)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("ObjectPhysics should reject negative airDamping", threw)
    }
}
