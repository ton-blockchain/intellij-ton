package org.ton.intellij.util.unify

import org.junit.Test

class UnificationTableTest {
    private object Value
    class TyVar(override var parent: NodeOrValue = VarValue(null, 0)) : Node

    private val table: UnificationTable<TyVar, Value> = UnificationTable()

    @Test
    fun `test unifyVarValue`() = checkDifferentSnapshotStates { a ->
        table.unifyVarValue(a, Value)
    }

    @Test
    fun `test unifyVarVar first`() = checkDifferentSnapshotStates { a ->
        val b = TyVar()
        table.unifyVarVar(a, b)
        table.unifyVarValue(b, Value)
    }

    @Test
    fun `test unifyVarVar last`() = checkDifferentSnapshotStates { a ->
        val b = TyVar()
        table.unifyVarValue(b, Value)
        table.unifyVarVar(a, b)
    }

    @Test
    fun `test unifyVarVar both`() = checkDifferentSnapshotStates { a ->
        val b = TyVar()
        table.unifyVarVar(a, b)
        table.unifyVarValue(a, Value)
        table.unifyVarValue(b, Value)
    }

    @Test
    fun `test unifyVarVar both reverse`() = checkDifferentSnapshotStates { a ->
        val b = TyVar()
        table.unifyVarValue(a, Value)
        table.unifyVarValue(b, Value)
        table.unifyVarVar(a, b)
    }

    @Test
    fun `test 3 vars`() = checkDifferentSnapshotStates { a ->
        val b = TyVar()
        val c = TyVar()
        table.unifyVarVar(a, b)
        table.unifyVarVar(b, c)
        table.unifyVarValue(c, Value)
    }

    @Test
    fun `test clusters`() = checkDifferentSnapshotStates { a ->
        val b = TyVar()
        val c = TyVar()
        val d = TyVar()
        table.unifyVarVar(a, b)
        table.unifyVarVar(c, d)
        table.unifyVarValue(d, Value)

        table.unifyVarVar(b, c)
    }

    private fun checkDifferentSnapshotStates(action: (TyVar) -> Unit) {
        fun checkWithoutSnapshot(action: (TyVar) -> Unit): TyVar {
            val ty = TyVar()
            action(ty)
            check(table.findValue(ty) == Value)
            return ty
        }

        fun checkSnapshotCommit(action: () -> TyVar) {
            val snapshot = table.startSnapshot()
            val ty = action()
            snapshot.commit()
            check(table.findValue(ty) == Value)
        }

        fun checkSnapshotRollback(action: () -> TyVar) {
            val snapshot = table.startSnapshot()
            val ty = action()
            snapshot.rollback()
            check(table.findValue(ty) == null)
        }

        checkWithoutSnapshot(action)
        checkSnapshotCommit { checkWithoutSnapshot(action) }
        checkSnapshotRollback { checkWithoutSnapshot(action) }

        checkSnapshotCommit { // snapshot-snapshot-commit-commit
            checkWithoutSnapshot { ty ->
                table.unifyVarValue(ty, Value)
                checkSnapshotCommit { checkWithoutSnapshot(action) }
            }
        }

        checkSnapshotCommit { // snapshot-snapshot-rollback-commit
            checkWithoutSnapshot { ty ->
                table.unifyVarValue(ty, Value)
                checkSnapshotRollback { checkWithoutSnapshot(action) }
            }
        }

        checkSnapshotRollback { // snapshot-snapshot-commit-rollback
            checkWithoutSnapshot { ty ->
                table.unifyVarValue(ty, Value)
                checkSnapshotCommit { checkWithoutSnapshot(action) }
            }
        }

        checkSnapshotRollback { // snapshot-snapshot-rollback-rollback
            checkWithoutSnapshot { ty ->
                table.unifyVarValue(ty, Value)
                checkSnapshotRollback { checkWithoutSnapshot(action) }
            }
        }
    }
}
