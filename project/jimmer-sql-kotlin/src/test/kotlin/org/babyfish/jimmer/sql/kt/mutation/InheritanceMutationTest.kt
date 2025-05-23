package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.PreparedIdGenerator
import org.babyfish.jimmer.sql.kt.model.inheritance.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test

class InheritanceMutationTest : AbstractMutationTest() {

    fun sqlClient(): KSqlClient =
        sqlClient {
            setIdGenerator(Role::class, PreparedIdGenerator(101L))
            setIdGenerator(Permission::class, PreparedIdGenerator(101L, 102L))
            addDraftInterceptor(NamedEntityDraftInterceptor)
        }

    @Test
    fun testSaveRole() {
        executeAndExpectResult({con ->
            sqlClient().entities.forConnection(con).save(
                new(Role::class).by {
                    name = "role"
                    permissions().addBy {
                        name = "permission-1"
                    }
                    permissions().addBy {
                        name = "permission-2"
                    }
                }
            )
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from ROLE tb_1_ 
                        |where tb_1_.NAME = ? and tb_1_.DELETED <> ?""".trimMargin()
                )
                queryReason(QueryReason.INTERCEPTOR)
            }
            statement {
                sql(
                    """insert into ROLE(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME) 
                        |values(?, ?, ?, ?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME, tb_1_.ROLE_ID 
                        |from PERMISSION tb_1_ 
                        |where tb_1_.NAME in (?, ?) and tb_1_.DELETED <> ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """insert into PERMISSION(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID) 
                        |values(?, ?, ?, ?, ?, ?)""".trimMargin()
                )
                batches(2)
            }
            entity {
                original(
                    """{"name":"role","permissions":[{"name":"permission-1"},{"name":"permission-2"}]}"""
                )
                modified(
                    """{
                        |--->"name":"role",
                        |--->"deleted":false,
                        |--->"createdTime":"2022-10-03 00:00:00",
                        |--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->"permissions":[
                        |--->--->{
                        |--->--->--->"name":"permission-1",
                        |--->--->--->"deleted":false,
                        |--->--->--->"createdTime":"2022-10-03 00:00:00",
                        |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->--->--->"role":{"id":101},"id":101
                        |--->--->},{
                        |--->--->--->"name":"permission-2",
                        |--->--->--->"deleted":false,
                        |--->--->--->"createdTime":"2022-10-03 00:00:00",
                        |--->--->--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->--->--->"role":{"id":101},"id":102
                        |--->--->}
                        |--->
                        |],"id":101}""".trimMargin()
                )
            }
        }
    }

    @Test
    fun testSavePermission() {
        executeAndExpectResult({con ->
            sqlClient().entities.forConnection(con).save(
                new(Permission::class).by {
                    name = "permission"
                    role().apply {
                        name = "role"
                    }
                }
            )
        }) {
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from ROLE tb_1_ 
                        |where tb_1_.NAME = ? and tb_1_.DELETED <> ?""".trimMargin()
                )
                queryReason(QueryReason.INTERCEPTOR)
            }
            statement {
                sql(
                    """insert into ROLE(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME) 
                        |values(?, ?, ?, ?, ?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.NAME 
                        |from PERMISSION tb_1_ 
                        |where tb_1_.NAME = ? and tb_1_.DELETED <> ?""".trimMargin()
                )
                queryReason(QueryReason.INTERCEPTOR)
            }
            statement {
                sql(
                    """insert into PERMISSION(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ROLE_ID) 
                        |values(?, ?, ?, ?, ?, ?)""".trimMargin()
                )
                batches(1)
            }
            entity {
                original(
                    """{"name":"permission","role":{"name":"role"}}"""
                )
                modified(
                    """{
                        |--->"name":"permission",
                        |--->"deleted":false,
                        |--->"createdTime":"2022-10-03 00:00:00",
                        |--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->"role":{
                        |--->--->"name":"role",
                        |--->--->"deleted":false,
                        |--->--->"createdTime":"2022-10-03 00:00:00",
                        |--->--->"modifiedTime":"2022-10-03 00:10:00",
                        |--->--->"id":101
                        |--->},
                        |--->"id":101
                        |}""".trimMargin()
                )
            }
        }
    }

    companion object {

        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        private val CREATED_TIME = LocalDateTime.parse("2022-10-03 00:00:00", FORMATTER)

        private val MODIFIED_TIME = LocalDateTime.parse("2022-10-03 00:10:00", FORMATTER)
    }

    private object NamedEntityDraftInterceptor : DraftInterceptor<NamedEntity, NamedEntityDraft> {
        override fun beforeSave(draft: NamedEntityDraft, original: NamedEntity?) {
            if (!isLoaded(draft, NamedEntity::deleted)) {
                draft.deleted = false
            }
            if (!isLoaded(draft, NamedEntity::modifiedTime)) {
                draft.modifiedTime = MODIFIED_TIME
            }
            if (original === null && !isLoaded(draft, NamedEntity::createdTime)) {
                draft.createdTime = CREATED_TIME
            }
        }
    }
}