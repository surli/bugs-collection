/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.api.test;

import static org.junit.Assert.*;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;
import static org.semanticweb.owlapi.search.Searcher.inverse;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.contains;

import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

import com.google.common.collect.Lists;

/**
 * @author Matthew Horridge, The University Of Manchester, Information
 *         Management Group
 * @since 2.2.0
 */
@SuppressWarnings("javadoc")
public class ObjectPropertyTestCase extends TestBase {

    @Test
    public void testNamedSimplification() {
        OWLObjectProperty p = ObjectProperty(iri("p"));
        OWLObjectPropertyExpression exp = p.getSimplified();
        assertEquals(p, exp);
    }

    @Test
    public void testInverseSimplification() {
        OWLObjectProperty p = ObjectProperty(iri("p"));
        OWLObjectPropertyExpression inv = p.getInverseProperty();
        OWLObjectPropertyExpression exp = inv.getSimplified();
        assertEquals(inv, exp);
    }

    @Test
    public void testInverseInverseSimplification() {
        OWLObjectProperty p = ObjectProperty(iri("p"));
        OWLObjectPropertyExpression inv = p.getInverseProperty();
        OWLObjectPropertyExpression inv2 = inv.getInverseProperty();
        OWLObjectPropertyExpression exp = inv2.getSimplified();
        assertEquals(p, exp);
    }

    @Test
    public void testInverseInverseInverseSimplification() {
        OWLObjectProperty p = ObjectProperty(iri("p"));
        OWLObjectPropertyExpression inv = p.getInverseProperty();
        OWLObjectPropertyExpression inv2 = inv.getInverseProperty();
        OWLObjectPropertyExpression inv3 = inv2.getInverseProperty();
        OWLObjectPropertyExpression exp = inv3.getSimplified();
        assertEquals(inv, exp);
    }

    @Test
    public void testInverse() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = ObjectProperty(iri("p"));
        OWLObjectProperty propQ = ObjectProperty(iri("q"));
        OWLAxiom ax = InverseObjectProperties(propP, propQ);
        ont.getOWLOntologyManager().addAxiom(ont, ax);
        assertTrue(contains(inverse(ont.inverseObjectPropertyAxioms(propP), propP), propQ));
        assertFalse(contains(inverse(ont.inverseObjectPropertyAxioms(propP), propP), propP));
    }

    @Test
    public void testInverseSelf() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = ObjectProperty(iri("p"));
        OWLAxiom ax = InverseObjectProperties(propP, propP);
        ont.getOWLOntologyManager().addAxiom(ont, ax);
        assertTrue(contains(inverse(ont.inverseObjectPropertyAxioms(propP), propP), propP));
    }

    @Test
    public void testCompareRoleChains() {
        OWLObjectPropertyExpression p = df.getOWLObjectProperty("_:", "p");
        OWLObjectPropertyExpression q = df.getOWLObjectProperty("_:", "q");
        OWLObjectPropertyExpression r = df.getOWLObjectProperty("_:", "r");
        OWLSubPropertyChainOfAxiom ax1 = df.getOWLSubPropertyChainOfAxiom(Lists.newArrayList(p, q), r);
        OWLSubPropertyChainOfAxiom ax2 = df.getOWLSubPropertyChainOfAxiom(Lists.newArrayList(p, p), r);
        assertNotEquals("role chains should not be equal", ax1, ax2);
        int comparisonResult = ax1.compareTo(ax2);
        assertNotEquals("role chain comparision:\n " + ax1 + " should not compare to\n " + ax2 + " as 0\n", 0,
            comparisonResult);
    }
}
