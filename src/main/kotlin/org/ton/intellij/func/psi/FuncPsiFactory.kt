package org.ton.intellij.func.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiParserFacade
import org.intellij.lang.annotations.Language
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.util.descendantOfTypeStrict

@Service(Service.Level.PROJECT)
class FuncPsiFactory private constructor(val project: Project) {
    val builtinStdlibFile by lazy {
        createFile(
            "builtin.fc", """
            
            int _+_(int x, int y) asm "ADD";
            int _-_(int x, int y) asm "SUB";
            int -_(int x) asm "NEGATE";
            int _*_(int x, int y) asm "MUL";
            int _/_(int x, int y) asm "DIV";
            int _~/_(int x, int y) asm "DIVR";
            int _^/_(int x, int y) asm "DIVC";
            int _%_(int x, int y) asm "MOD";
            int _~%_(int x, int y) asm "MODR";
            int _^%_(int x, int y) asm "MODC";
            (int, int) _/%_(int x, int y) asm "DIVMOD";
            (int, int) divmod(int x, int y) asm "DIVMOD";
            (int, int) ~divmod(int x, int y) asm "DIVMOD";
            (int, int) moddiv(int x, int y) asm(x y -> 1 0) "DIVMOD";
            (int, int) ~moddiv(int x, int y) asm(x y -> 1 0) "DIVMOD";
            int _<<_(int x, int y) asm "LSHIFT";
            int _>>_(int x, int y) asm "RSHIFT";
            int _~>>_(int x, int y) asm "RSHIFTR";
            int _^>>_(int x, int y) asm "RSHIFTC";
            int _&_(int x, int y) asm "AND";
            int _|_(int x, int y) asm "OR";
            int _^_(int x, int y) asm "XOR";
            int ~_(int x) asm "NOT";
            int ^_+=_(int x, int y) asm "ADD";
            int ^_-=_(int x, int y) asm "SUB";
            int ^_*=_(int x, int y) asm "MUL";
            int muldiv(int x, int y, int z) asm "MULDIV";    
            
            ;;; `q'=round(x*y/z)`
            int muldivr(int x, int y, int z) asm "MULDIVR";
                        
            int muldivc(int x, int y, int z) asm "MULDIVC";
            
            (int, int) muldivmod(int x, int y, int z) asm "MULDIVMOD";
            
            int _==_(int x, int y) asm "EQUAL";
            
            int _!=_(int x, int y) asm "NEQ";
            
            int _<_(int x, int y) asm "LESS";
            
            int _>_(int x, int y) asm "GREATER";
            
            int _<=_(int x, int y) asm "LEQ";
            
            int _>=_(int x, int y) asm "GEQ";
            
            int _<=>_(int x, int y) asm "CMP";
            
            ;;; Checks whether [x] is a _Null_, and returns `-1` or `0` accordingly.
            forall X -> int null?(X x) asm "ISNULL";

            ;;; Throws exception [`excno`] with parameter zero.
            ;;;
            ;;; In other words, it transfers control to the continuation in `c2`, 
            ;;; pushing `0` and [`excno`] into it's stack, and discarding the old stack altogether.
            () throw(int excno) impure asm "THROW";
            
            ;;; Throws exception [`excno`] with parameter zero only if [`cond`] != `0`.
            () throw_if(int excno, int cond) impure asm "THROWARGIF";
            
            ;;; Throws exception [`excno`] with parameter zero only if [`cond`] == `0`.
            () throw_unless(int excno, int cond) impure asm "THROWARGIFNOT";
            
            ;;; Throws exception [`excno`] with parameter [`x`],
            ;;; by copying [`x`] and [`excno`] into the stack of `c2` and transferring control to `c2`.
            forall X -> () throw_arg(X x, int excno) impure asm "THROWARGANY";
            
            ;;; Throws exception [`excno`] with parameter [`x`] only if [`cond`] != `0`.
            forall X -> () throw_arg_if(X x, int excno, int cond) impure asm "THROWARGANYIF";
           
            ;;; Throws exception [`excno`] with parameter [`x`] only if [`cond`] == `0`.
            forall X -> () throw_arg_unless(X x, int excno, int cond) impure asm "THROWARGANYIFNOT";
            
            ;;; Loads a signed [`len`]-bit integer from slice [`s`].
            (slice, int) load_int(slice s, int len) asm(s len -> 1 0) "LDIX";
            
            ;;; Loads a signed [`len`]-bit integer from slice [`s`].
            (slice, int) ~load_int(slice s, int len) asm(s len -> 1 0) "LDIX";
            
            ;;; Loads a unsigned [`len`]-bit integer from slice [`s`].
            (slice, int) load_uint(slice s, int len) asm(s len -> 1 0) "LDUX";
            
            ;;; Loads a unsigned [`len`]-bit integer from slice [`s`].
            (slice, int) ~load_uint(slice s, int len) asm(s len -> 1 0) "LDUX";
            
            ;;; Preloads a signed [`len`]-bit integer from slice [`s`].
            int preload_int(slice s, int len) asm "PLDIX";
                        
            ;;; Preloads an unsigned [`len`]-bit integer from slice [`s`].
            int preload_uint(slice s, int len) asm "PLDUX";
                        
            ;;; Stores a signed [`len`]-bit integer [`x`] into [`b`] for `0 ≤ len ≤ 257`.
            builder store_int(builder b, int x, int len) asm(x b len) "STIX";

            ;;; Stores a unsigned [`len`]-bit integer [`x`] into [`b`] for `0 ≤ len ≤ 256`.
            builder store_uint(builder b, int x, int len) asm(x b len) "STUX";
            
            ;;; Loads the first `0` ≤ [`len`] ≤ `1023` bits from slice [`s`] into a separate slice `s''`.
            (slice, slice) load_bits(slice s, int len) asm(s len -> 1 0) "LDSLICEX";
            
            ;;; Preloads the first `0` ≤ [`len`] ≤ `1023` bits from slice [`s`] into a separate slice `s''`.
            slice preload_bits(slice s, int len) asm "PLDSLICEX";
            
            ;;; Returns the [`index`]-th int-element of tuple [`t`].
            int int_at(tuple t, int index) asm "INDEXVAR";
            
            ;;; Returns the [`index`]-th cell-element of tuple [`t`].
            cell cell_at(tuple t, int index) asm "INDEXVAR";

            ;;; Returns the [`index`]-th slice-element of tuple [`t`].
            slice slice_at(tuple t, int index) asm "INDEXVAR";
            
            ;;; Returns the [`index`]-th tuple-element of tuple [`t`].
            tuple tuple_at(tuple t, int index) asm "INDEXVAR";
            
            ;;; Returns the [`index`]-th element of tuple [`t`].
            forall X -> X at(tuple t, int index) asm "INDEXVAR";
            
            ;;; Moves a variable [x] to the top of the stack.
            forall X -> X touch(X x) asm "NOP";

            ;;; Moves a variable [x] to the top of the stack.
            forall X -> (X, ()) ~touch(X x) asm "NOP";
            
            forall X, Y -> (X, Y) touch2((X, Y) xy) asm "NOP";
            
            forall X, Y -> ((X, Y), ()) ~touch2((X, Y) xy) asm "NOP";

            ;;; Dump variable [x] to the debug log.
            forall X -> (X, ()) ~dump(X x) impure asm "s0 DUMP";
            
            ;;; Dump string [x] to the debug log.
            forall X -> (X, ()) ~strdump(X x) impure asm "STRDUMP";
            
            () run_method0(int id) impure asm "0 CALLXARGS";
            
            forall X -> () run_method1(int id, X x) impure asm(x id) "1 CALLXARGS";
            
            forall X, Y -> () run_method2(int id, X x, Y y) impure asm(x y id) "2 CALLXARGS";
                        
            forall X, Y, Z -> () run_method3(int id, X x, Y y, Z z) impure asm(x y z id) "3 CALLXARGS";
            
        """.trimIndent()
        ).also {
            it.virtualFile.isWritable = false
        }
    }

    fun createFile(@Language("FunC") text: CharSequence) =
        createFile(null, text)

    fun createFile(name: String?, @Language("FunC") text: CharSequence) =
        PsiFileFactory.getInstance(project).createFileFromText(name ?: "dummy.fc", FuncLanguage, text) as FuncFile

    fun createNewline(): PsiElement = createWhitespace("\n")

    fun createWhitespace(ws: String): PsiElement =
        PsiParserFacade.getInstance(project).createWhiteSpaceFromText(ws)

    fun createStatement(text: String): FuncStatement {
        val file = createFile("() foo() { $text }")
        return file.functions.first().blockStatement!!.statementList.first()
    }

    fun createExpression(text: String): FuncExpression {
        return (createStatement("$text;") as FuncExpressionStatement).expression
    }

    fun createIdentifierFromText(text: String): PsiElement {
        val funcFile = createFile("() $text();")
        val function = funcFile.functions.first()
        return function.identifier
    }

    fun createIncludeDefinition(text: String): FuncIncludeDefinition =
        createFromText("#include \"$text\";")
            ?: error("Failed to create include definition from text: `$text`")

    private inline fun <reified T : FuncElement> createFromText(
        @Language("FunC") code: CharSequence
    ): T? = createFile(code).descendantOfTypeStrict()

    companion object {
        operator fun get(project: Project) =
            requireNotNull(project.getService(FuncPsiFactory::class.java))
    }
}
