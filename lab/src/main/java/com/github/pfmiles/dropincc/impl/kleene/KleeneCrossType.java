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
package com.github.pfmiles.dropincc.impl.kleene;

/**
 * @author pf-miles
 * 
 */
public class KleeneCrossType extends KleeneType {

    /**
     * valid defIndex starts from 0
     * 
     * @param defIndex
     */
    public KleeneCrossType(int defIndex) {
        super(defIndex);
    }

    @Override
    public String toCodeGenStr() {
        return "kleeneCross" + this.defIndex;
    }

}