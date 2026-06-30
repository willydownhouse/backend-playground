package com.learn.user

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User : PanacheEntityBase {

    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(nullable = false, unique = true)
    lateinit var username: String

    @Column(nullable = false, unique = true)
    lateinit var email: String

    fun toResponse(): UserResponse = UserResponse(
        id = id,
        username = username,
        email = email,
    )

    companion object : PanacheCompanionBase<User, UUID>
}
