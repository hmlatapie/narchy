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
package com.github.pfmiles.dropincc.impl.syntactical.codegen;

import com.github.pfmiles.dropincc.impl.llstar.PredictingGrule;
import com.github.pfmiles.dropincc.impl.syntactical.codegen.rulemethods.MethodContent;
import com.github.pfmiles.dropincc.impl.util.SeqGen;

import java.text.MessageFormat;
import java.util.List;

/**
 * Responsible for recursive descent rule matching methods generation.
 * 
 * @author pf-miles
 * 
 */
public class RuleMethodsGen extends CodeGen {
    // method skeleton -> only string code
    // 0: gruleName
    // 1: methodContent
    private static final String ruleMethodSkeleton = getTemplate("ruleMethodSkeleton.dt", RuleMethodsGen.class);

    private final List<PredictingGrule> pgs;

    public RuleMethodsGen(List<PredictingGrule> pgs) {
        this.pgs = pgs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String render(CodeGenContext context) {
        StringBuilder sb = new StringBuilder();
        for (PredictingGrule p : pgs) {
            // the 'varSeq' is method scoped
            context.varSeq = new SeqGen();
            context.curGrule = p.type;
            String ruleName = p.type.toCodeGenStr();
            sb.append(MessageFormat.format(ruleMethodSkeleton, ruleName, new MethodContent(p).render(context))).append('\n');
        }
        return sb.toString();
    }
}