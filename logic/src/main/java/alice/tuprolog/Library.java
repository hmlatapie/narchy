/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import jcog.list.FasterList;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static alice.tuprolog.PrologPrimitive.FUNCTOR;


/**
 *
 * This abstract class is the base class for developing
 * tuProlog built-in libraries, which can be dynamically
 * loaded by prolog objects.
 * <p>
 * Each library can expose to engine:
 * <ul>
 * <li> a theory (as a string assigned to theory field)
 * <li> builtin predicates: each method whose signature is
 *       boolean name_arity(Term arg0, Term arg1,...)
 *   is considered a built-in predicate provided by the library
 * <li> builtin evaluable functors: each method whose signature is
 *       Term name_arity(Term arg0, Term arg1,...)
 *   is considered a built-in functors provided by the library
 * </ul>
 * <p>
 */
public abstract class Library implements Serializable {


    /**
	 * (current) prolog core which loaded this library
	 */
    protected Prolog engine = null;
    
    /**
	 * operator mapping
	 */
    private final String[][] synonyms;
    
    public Library(){
        synonyms = buildSynonyms();
    }
    
    /**
     * Gets the name of the library. 
     * 
     * By default the name is the class name.
     * 
     * @return the library name
     */
    public String getName() {
        return getClass().getName();
    }
    
    /**
     * Gets the theory provided with the library
     *
     * Empty theory is provided by default.
     */
    public String getTheory() {
        return "";
    }
    
//    public static String getTheory(int a) {
//    	return "";
//    }
    
    /**
     * Gets the synonym mapping, as array of
     * elements like  { synonym, original name}
     */
    @Nullable
    public String[][] buildSynonyms() {
        return null;
    }

    /**
	 * @param en
	 */
    public void setEngine(Prolog en) {	
        engine = en;
    }
    
    /**
     * tries to unify two terms
     *
     * The runtime (demonstration) context currently used by the engine
     * is deployed and altered.
     */
    protected boolean unify(Term a0,Term a1) {
        return engine.unify(a0,a1);
    }


    /**
     * Evaluates an expression. Returns null value if the argument
     * is not an evaluable expression
     *
     * The runtime (demo) context currently used by the engine
     * is deployed and altered.
     * @throws Throwable 
     */
    protected Term evalExpression(Term term) throws Throwable {
        if (term == null)
            return null;
        Term val = term.term();
        if (val instanceof Struct) {
            final Struct t = (Struct) val;
            boolean primitive = t.isPrimitive();
            if (!primitive && term != t) {
                engine.prims.identify(t, FUNCTOR); //identifyFunctor(t);
            } else if (primitive) {
                PrologPrimitive bt = t.getPrimitive();
                if ((bt.type == FUNCTOR)) // check for library functors
                    return bt.evalAsFunctor(t);
            }
        } else if (val instanceof NumberTerm) {
            return val;
        }
        return null;
    }
    
    
    /**
     * method invoked by prolog engine when library is
     * going to be removed
     */
    public void dismiss() {}
    
    /**
     * method invoked when the engine is going
     * to demonstrate a goal
     */
    public void onSolveBegin(Term goal) {}
    
    /**
     * method invoked when the engine has
     * finished a demostration
     */
    
    public void onSolveHalt(){}
    
    public void onSolveEnd() {}
    
    /**
     * gets the list of predicates defined in the library
     */
    public Map<Integer,List<PrologPrimitive>> primitives() {
        try {
            java.lang.reflect.Method[] mlist = this.getClass().getMethods();
            Map<Integer,List<PrologPrimitive>> mapPrimitives = new HashMap<>();
            mapPrimitives.put(PrologPrimitive.DIRECTIVE, new FasterList<>());
            mapPrimitives.put(FUNCTOR, new FasterList<>());
            mapPrimitives.put(PrologPrimitive.PREDICATE, new FasterList<>());
            //{new ArrayList<PrimitiveInfo>(), new ArrayList<PrimitiveInfo>(), new ArrayList<PrimitiveInfo>()};
            
            for (int i = 0; i < mlist.length; i++) {
                String name = mlist[i].getName();
                
                Class<?>[] clist = mlist[i].getParameterTypes();
                Class<?> rclass = mlist[i].getReturnType();
                String returnTypeName = rclass.getName();
                
                int type;
                switch (returnTypeName) {
                    case "boolean":
                        type = PrologPrimitive.PREDICATE;
                        break;
                    case "alice.tuprolog.Term":
                        type = FUNCTOR;
                        break;
                    case "void":
                        type = PrologPrimitive.DIRECTIVE;
                        break;
                    default:
                        continue;
                }
                
                int index=name.lastIndexOf('_');
                if (index!=-1) {
                    try {
                        int arity = Integer.parseInt(name.substring(index + 1, name.length()));
                        // check arg number
                        if (clist.length == arity) {
                            boolean valid = true;
                            for (int j=0; j<arity; j++) {
                                if (!(Term.class.isAssignableFrom(clist[j]))) {
                                    valid = false;
                                    break;
                                }
                            }
                            if (valid) {
                                String rawName = name.substring(0,index);
                                String key = rawName + '/' + arity;
                                PrologPrimitive prim = new PrologPrimitive(type, key, this, mlist[i], arity);
                                mapPrimitives.get(type).add(prim);
                                //
                                // adding also or synonims
                                //
                                if (synonyms != null) {
                                    String[] stringFormat = {"directive", "predicate", "functor"};
                                    for (int j = 0; j< synonyms.length; j++){
                                        String[] map = synonyms[j];
                                        if (map[1].equals(rawName) && map[2].equals(stringFormat[type])){
                                            key = map[0] + '/' + arity;
                                            prim = new PrologPrimitive(type, key, this, mlist[i], arity);
                                            mapPrimitives.get(type).add(prim);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                
            }
            return mapPrimitives;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    
    /**
     * Gets the method linked to a builtin (null value if
     * the builtin has not any linked service)
     */
    /*    public Method getLinkedMethod(Struct s){
     //System.out.println("get linked for "+s);         
      
      int arity = s.getArity();
      String name = s.getName()+"_"+arity; 
      
      // NOT found, Try with synonims
       Method m = findMethod(name,arity);    
       if (m!=null){
       return m;
       }
       
       // try with synonims
        if (opMappingCached!=null){
        String rawName=s.getName();
        for (int j=0; j<opMappingCached.length; j++){
        String[] map=opMappingCached[j];
        if (map[0].equals(rawName)){
        return findMethod(map[1]+"_"+s.getArity(),s.getArity());
        }
        }
        }
        return null;
        }
        
        private Method findMethod(String name, int arity){
        Method[] mlist = this.getClass().getMethods();
        for (int i=0; i<mlist.length; i++){
        if (mlist[i].getName().equals(name)){
        Class[] parms=mlist[i].getParameterTypes();
        if (parms.length==arity){
        boolean valid=true;
        for (int j=0; j<parms.length; j++){
        if (!Term.class.isAssignableFrom(parms[j])){
        valid=false;
        }
        }
        if (valid){
        return mlist[i];
        }
        }
        }
        }
        return null;
        }
        */
    
    
}