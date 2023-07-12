package org.ton.intellij.func.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.intellij.lang.annotations.Language
import org.ton.intellij.func.FuncLanguage

@Service(Service.Level.PROJECT)
class FuncElementFactory(val project: Project) {
    val builtinStdlibFile by lazy {
        createFileFromText(
            "builtin_stdlib.fc", """
                
            (int, int) divmod(int x, int y) asm "DIVMOD";
                
            (int, int) moddiv(int x, int y) asm(x y -> 1 0) "DIVMOD";   
                            
            int muldiv(int x, int y, int z) asm "MULDIV";    
            
            ;;; `q'=round(x*y/z)`
            int muldivr(int x, int y, int z) asm "MULDIVR";
                        
            int muldivc(int x, int y, int z) asm "MULDIVC";
            
            (int, int) muldivmod(int x, int y, int z) asm "MULDIVMOD";
            
            ;;; Checks whether [x] is a _Null_, and returns `-1` or `0` accordingly.
            forall X -> int null?(X x) asm "ISNULL";

            () throw(int excno) impure asm "THROW";
            
            () throw_if(int excno, int cond) impure asm "THROWARGIF";
            
            () throw_unless(int excno, int cond) impure asm "THROWARGIFNOT";
            
            forall X -> () throw_arg(X x, int excno) impure asm "THROWARGANY";
            
            forall X -> () throw_arg_if(X x, int excno, int cond) impure asm "THROWARGANYIF";
           
            forall X -> () throw_arg_unless(X x, int excno, int cond) impure asm "THROWARGANYIFNOT";
            
            (slice, int) ~load_int(slice s, int len) asm(s len -> 1 0) "LDIX";
            
            (slice, int) ~load_uint(slice s, int len) asm(s len -> 1 0) "LDUX";
            
            int preload_int(slice s, int len) asm "PLDIX";
                        
            int preload_uint(slice s, int len) asm "PLDUX";
                        
            builder store_int(builder b, int x, int len) asm(x b len) "STIX";

            builder store_uint(builder b, int x, int len) asm(x b len) "STUX";
            
            (slice, slice) load_bits(slice s, int len) asm(s len -> 1 0) "LDSLICEX";
            
            slice preload_bits(slice s, int len) asm "PLDSLICEX";
            
            int int_at(tuple t, int index) asm "INDEXVAR";
            
            cell cell_at(tuple t, int index) asm "INDEXVAR";
            
            slice slice_at(tuple t, int index) asm "INDEXVAR";
            
            tuple tuple_at(tuple t, int index) asm "INDEXVAR";
            
            forall X -> X at(tuple t, int index) asm "INDEXVAR";
            
            ;;; Moves a variable [x] to the top of the stack
            forall X -> X touch(X x) asm "NOP";

            ;;; Moves a variable [x] to the top of the stack
            forall X -> (X, ()) ~touch(X x) asm "NOP";
            
            forall X, Y -> (X, Y) touch2((X, Y) xy) asm "NOP";
            
            forall X, Y -> ((X, Y), ()) ~touch2((X, Y) xy) asm "NOP";

            forall X -> (X, ()) ~dump(X x) impure asm "s0 DUMP";
            
            forall X -> (X, ()) ~strdump(X x, slice str) impure asm "STRDUMP";
            
            () run_method0(int id) impure asm "0 CALLXARGS";
            
            forall X -> () run_method1(int id, X x) impure asm(x id) "1 CALLXARGS";
            
            forall X, Y -> () run_method2(int id, X x, Y y) impure asm(x y id) "2 CALLXARGS";
                        
            forall X, Y, Z -> () run_method3(int id, X x, Y y, Z z) impure asm(x y z id) "3 CALLXARGS";
            
        """.trimIndent()
        ).also {
            it.virtualFile.isWritable = false
        }
    }

    fun createFileFromText(name: String?, @Language("FunC") text: String) =
        PsiFileFactory.getInstance(project).createFileFromText(name ?: "dummy.fc", FuncLanguage, text) as FuncFile

    fun createIdentifierFromText(project: Project, text: String) {
//        val funcFile = createFileFromText(project, "const $text;")
//        val const = PsiTreeUtil.findChildOfType(funcFile, FuncConstVariable::class.java)
    }

    companion object {
        operator fun get(project: Project) =
            requireNotNull(project.getService(FuncElementFactory::class.java))
    }
}
