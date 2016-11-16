/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc.impl.syntactical.codegen.rulemethods;

import com.github.pfmiles.dropincc.impl.CAlternative;
import com.github.pfmiles.dropincc.impl.llstar.PredictingGrule;
import com.github.pfmiles.dropincc.impl.syntactical.codegen.CodeGen;
import com.github.pfmiles.dropincc.impl.syntactical.codegen.CodeGenContext;
import com.github.pfmiles.dropincc.impl.syntactical.codegen.rulemethods.code.ElementsCodeGen;
import com.github.pfmiles.dropincc.impl.util.Pair;

import java.text.MessageFormat;

/**
 * @author pf-miles
 * 
 */
public class MultiAltMatchBacktrackGen extends CodeGen {

    // multi alt match code(backtracking rule) -> var and code
    // 0: ruleName
    // 1: backtracksCode
    // 2: cleanCacheCode
    private static final String fmt = getTemplate("multiAltBacktrackMatchCode.dt", MultiAltMatchBacktrackGen.class);
    // altBacktrack -> only string code
    // 0: ruleNum
    // 1: elements code
    // 2: elements var
    // 3: actionName
    private static final String altBacktrackFmt = getTemplate("altBacktrack.dt", MultiAltMatchBacktrackGen.class);
    private static final String altBacktrackOnPathFmt = getTemplate("altBacktrackOnPath.dt", MultiAltMatchBacktrackGen.class);

    private final PredictingGrule pg;
    private final boolean generatingBacktrackCode;

    public MultiAltMatchBacktrackGen(PredictingGrule pg, boolean generatingBacktrackCode) {
        this.pg = pg;
        this.generatingBacktrackCode = generatingBacktrackCode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String render(CodeGenContext context) {
        String ruleName = this.pg.type.toCodeGenStr();
        StringBuilder backtrackCode = new StringBuilder();
        String ruleNum = String.valueOf(pg.type.getDefIndex());
        for (CAlternative alt : pg.alts) {
            // the container rule is self-backtracking, so all its sub
            // components must generate backtracking code
            Pair<String, String> varAndCode = new ElementsCodeGen(alt.seq, true).render(context);
            String actionName = "null";
            if (alt.action != null) {
                actionName = context.actionFieldMapping.get(alt.action);
            }
            if (this.generatingBacktrackCode) {
                backtrackCode.append(MessageFormat.format(altBacktrackOnPathFmt, ruleNum, varAndCode.getRight(), varAndCode.getLeft(), actionName))
                        .append('\n');
            } else {
                backtrackCode.append(MessageFormat.format(altBacktrackFmt, ruleNum, varAndCode.getRight(), varAndCode.getLeft(), actionName)).append(
                        '\n');
            }
        }
        String cleanCacheCode = "";
        if (!this.generatingBacktrackCode)
            cleanCacheCode = "cleanCache();";
        return MessageFormat.format(fmt, ruleName, backtrackCode.toString(), cleanCacheCode);
    }

}