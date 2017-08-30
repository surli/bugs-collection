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
package org.semanticweb.owlapi.api.test.baseclasses;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Annotation;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.AnnotationAssertion;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.AnnotationProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.AnnotationPropertyDomain;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.AnnotationPropertyRange;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.AsymmetricObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataComplementOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataExactCardinality;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataIntersectionOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataMaxCardinality;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataMinCardinality;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataPropertyAssertion;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataPropertyDomain;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataPropertyRange;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DataUnionOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Datatype;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DatatypeDefinition;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DatatypeRestriction;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Declaration;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DifferentIndividuals;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DisjointClasses;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DisjointDataProperties;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.DisjointObjectProperties;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.EquivalentDataProperties;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.EquivalentObjectProperties;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.FacetRestriction;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.FunctionalDataProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.FunctionalObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.HasKey;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Integer;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.InverseFunctionalObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.InverseObjectProperties;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IrreflexiveObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Literal;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.NamedIndividual;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.NegativeDataPropertyAssertion;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.NegativeObjectPropertyAssertion;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectPropertyAssertion;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectPropertyDomain;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectPropertyRange;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.RDFSComment;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.RDFSLabel;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.ReflexiveObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SameIndividual;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubAnnotationPropertyOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubClassOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubDataPropertyOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubObjectPropertyOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubPropertyChainOf;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SymmetricObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.TopDatatype;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.TransitiveObjectProperty;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.createClass;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.createIndividual;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asUnorderedSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLIndividualArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;
import org.semanticweb.owlapi.model.SWRLVariable;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;

import com.google.common.collect.Sets;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 * @since 3.0.0
 */
@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class AxiomsRoundTrippingTestCase extends AxiomsRoundTrippingBase {

    private static final IRI iriA = iri("A");
    private static final OWLClass clsA = Class(iriA);
    private static final OWLClass clsB = Class(iri("B"));
    private static final OWLDataProperty dp = DataProperty(iri("p"));
    private static final OWLDataProperty dq = DataProperty(iri("q"));
    private static final OWLObjectProperty op = ObjectProperty(iri("op"));
    private static final OWLObjectProperty oq = ObjectProperty(iri("oq"));
    private static final OWLDataProperty dpA = DataProperty(iri("dpropA"));
    private static final OWLDataProperty dpB = DataProperty(iri("dpropB"));
    private static final OWLDataProperty dpC = DataProperty(iri("dpropC"));
    private static final OWLObjectProperty pA = ObjectProperty(iri("propA"));
    private static final OWLObjectProperty pB = ObjectProperty(iri("propB"));
    private static final OWLObjectProperty pC = ObjectProperty(iri("propC"));
    private static final OWLObjectProperty pD = ObjectProperty(iri("propD"));
    private static final OWLAnnotationProperty apropA = AnnotationProperty(iri("apropA"));
    private static final OWLAnnotationProperty apropB = AnnotationProperty(iri("apropB"));
    private static final OWLNamedIndividual ind = NamedIndividual(iri("i"));
    private static final OWLNamedIndividual indj = NamedIndividual(iri("j"));
    private static final OWLEntity peter =
        NamedIndividual(IRI("http://www.another.com/ont#", "peter"));
    private static final OWLAnnotation ann1 = Annotation(RDFSLabel(), Literal("Annotation 1"));
    private static final OWLAnnotation ann2 = Annotation(RDFSLabel(), Literal("Annotation 2"));
    private static final OWLAnnotation eAnn1 =
        Annotation(RDFSLabel(), Literal("EntityAnnotation 1"));
    private static final OWLAnnotation eAnn2 =
        Annotation(RDFSLabel(), Literal("EntityAnnotation 2"));
    private static final OWLDatatype datatype =
        Datatype(IRI("http://www.ont.com/myont/", "mydatatype"));
    private static final OWLAnnotation annoOuterOuter1 =
        Annotation(AnnotationProperty(iri("myOuterOuterLabel1")), Literal("Outer Outer label 1"));
    private static final OWLAnnotation annoOuterOuter2 =
        Annotation(AnnotationProperty(iri("myOuterOuterLabel2")), Literal("Outer Outer label 2"));
    private static final OWLDatatype dt = Datatype(IRI("file:/c/test.owlapi#", "SSN"));
    private static final OWLFacetRestriction fr =
        FacetRestriction(OWLFacet.PATTERN, Literal("[0-9]{3}-[0-9]{2}-[0-9]{4}"));
    private static final OWLDataRange dr =
        DatatypeRestriction(Datatype(IRI("http://www.w3.org/2001/XMLSchema#", "string")), fr);
    private static final OWLDataIntersectionOf disj1 = DataIntersectionOf(DataComplementOf(dr), dt);
    private static final OWLDataIntersectionOf disj2 = DataIntersectionOf(DataComplementOf(dt), dr);
    private static final OWLAnnotation annoOuter =
        Annotation(AnnotationProperty(iri("myOuterLabel")), Literal("Outer label"), annoOuterOuter1,
            annoOuterOuter2);
    private static final OWLAnnotation annoInner =
        Annotation(AnnotationProperty(iri("myLabel")), Literal("Label"), annoOuter);

    public AxiomsRoundTrippingTestCase(AxiomBuilder f) {
        super(f);
    }

    @Parameters
    public static List<AxiomBuilder> getData() {
        return Arrays.asList(
            //@formatter:off
            (AxiomBuilder) () -> swrl("http://www.owlapi#", df),
            () -> swrl("urn:swrl#", df),
            () -> singleton(SubPropertyChainOf(Arrays.asList(pA, pB, pC), pD)),
            () -> singleton(AsymmetricObjectProperty(op)),
            () -> singleton(DifferentIndividuals(createIndividual(), createIndividual(), createIndividual(), createIndividual(), createIndividual(), createIndividual(), createIndividual(), createIndividual(), createIndividual(), createIndividual())),
            () -> Sets.newHashSet( SubClassOf(clsA, ObjectSomeValuesFrom(op, ObjectSomeValuesFrom(op, clsB))), Declaration(clsA), Declaration(clsB)),
            () -> Sets.newHashSet(Declaration(RDFSLabel()), Declaration(peter), AnnotationAssertion(RDFSLabel(), peter.getIRI(), Literal("X", "en"), ann1, ann2)),
            () -> Sets.newHashSet(Declaration(RDFSLabel()), Declaration(peter, eAnn1, eAnn2), AnnotationAssertion(RDFSLabel(), peter.getIRI(), Literal("X", "en"), ann1, ann2)),
            () -> singleton(InverseObjectProperties(oq, op)),
            () -> singleton(InverseObjectProperties(op, oq)),
            () -> Sets.newHashSet(Declaration(clsA), AnnotationAssertion(apropA, clsA.getIRI(),     IRI("http://www.semanticweb.org/owlapi#", "object"))),
            () -> singleton(SubClassOf(clsA, clsB, singleton(annoInner))),
            () -> singleton(AnnotationPropertyDomain(RDFSComment(), iriA)),
            () -> singleton(AnnotationPropertyRange(RDFSComment(), iriA)),
            () -> singleton(SubAnnotationPropertyOf(apropA, RDFSLabel())),
            () -> singleton(SubClassOf(clsA, DataMaxCardinality(3, dp, Integer()))),
            () -> singleton(SubClassOf(clsA, DataMinCardinality(3, dp, Integer()))),
            () -> singleton(SubClassOf(clsA, DataExactCardinality(3, dp, Integer()))),
            () -> singleton(DataPropertyRange(dp, DataUnionOf(disj1, disj2))),
            () -> Sets.newHashSet( HasKey(singleton(Annotation(apropA, Literal("Test", ""))), clsA, pA, pB, pC), Declaration(apropA), Declaration(pA), Declaration(pB), Declaration(pC)),
            () -> singleton( DisjointClasses(asUnorderedSet(Stream.generate(() -> createClass()).limit(1000)))),
            () -> singleton(SubClassOf(clsB, ObjectSomeValuesFrom(op.getInverseProperty(), clsA))),
            () -> singleton(SubDataPropertyOf(dp, dq)),
            () -> singleton(DataPropertyAssertion(dp, ind, Literal(33.3))),
            () -> Sets.newHashSet(NegativeDataPropertyAssertion(dp, ind, Literal(33.3)), NegativeDataPropertyAssertion(dp, ind, Literal("weasel", "")), NegativeDataPropertyAssertion(dp, ind, Literal("weasel"))),
            () -> singleton(FunctionalDataProperty(dp)),
            () -> singleton(DataPropertyDomain(dp, Class(iri("A")))),
            () -> singleton(DataPropertyRange(dp, TopDatatype())),
            () -> Sets.newHashSet(DisjointDataProperties(dpA, dpB, dpC), Declaration(dpA), Declaration(dpB), Declaration(dpC)),
            () -> singleton(DisjointDataProperties(dpA, dpB)),
            () -> singleton(EquivalentDataProperties(dp, dq)),
            () -> singleton(AsymmetricObjectProperty(op)),
            () -> Sets.newHashSet(DatatypeDefinition(datatype, DataComplementOf(Integer())), Declaration(datatype)),
            () -> Sets.newHashSet(DifferentIndividuals(ind, indj), DifferentIndividuals(ind, NamedIndividual(iri("k")))),
            () -> singleton(DifferentIndividuals(ind, indj, NamedIndividual(iri("k")), NamedIndividual(iri("l")))),
            () -> Sets.newHashSet(DisjointObjectProperties(pA, pB, pC), Declaration(pA), Declaration(pB), Declaration(pC)),
            () -> singleton(DisjointObjectProperties(pA, pB)),
            () -> Sets.newHashSet(EquivalentObjectProperties(pA, pB), Declaration(pA), Declaration(pB)),
            () -> singleton(FunctionalObjectProperty(op)),
            () -> singleton(InverseFunctionalObjectProperty(op)),
            () -> singleton(IrreflexiveObjectProperty(op)),
            () -> singleton(DifferentIndividuals( asUnorderedSet(Stream.generate(() -> createIndividual()).limit(1000)))),
            () -> Sets.newHashSet(AnnotationAssertion(apropA, clsA.getIRI(), Literal("abc", "en")), Declaration(clsA)),
            () -> Sets.newHashSet(AnnotationAssertion(apropA, iriA, Literal("abc", "en")), AnnotationAssertion(apropA, iriA, Literal("abcd", "")), AnnotationAssertion(apropA, iriA, Literal("abcde")), AnnotationAssertion(apropA, iriA, Literal("abcdef", OWL2Datatype.XSD_STRING)), Declaration(clsA)),
            () -> singleton(NegativeObjectPropertyAssertion(op, ind, indj)),
            () -> singleton(ObjectPropertyAssertion(op, ind, indj)),
            () -> singleton(SubPropertyChainOf(Arrays.asList(pA, pB, pC), pD, Sets.newHashSet(Annotation(apropA, Literal("Test", "en")),     Annotation(apropB, Literal("Test", ""))))),
            () -> singleton(ObjectPropertyDomain(op, clsA)),
            () -> singleton(ObjectPropertyRange(op, clsA)),
            () -> Sets.newHashSet( Declaration(Class(IRI("http://www.test.com/ontology#", "Class%37A"))), Declaration(ObjectProperty(IRI("http://www.test.com/ontology#", "prop%37A")))),
            () -> singleton(ReflexiveObjectProperty(op)),
            () -> singleton(SameIndividual(ind, indj)),
            () -> singleton(DataPropertyAssertion(dp, ind, Literal("Test \"literal\"\nStuff"))),
            () -> Sets.newHashSet(DataPropertyAssertion(dp, ind, Literal("Test \"literal\"")), DataPropertyAssertion(dp, ind, Literal("Test 'literal'")), DataPropertyAssertion(dp, ind, Literal("Test \"\"\"literal\"\"\""))),
            () -> singleton(SubObjectPropertyOf(op, oq)),
            () -> singleton(SymmetricObjectProperty(op)),
            () -> singleton(TransitiveObjectProperty(op)),
            () -> Sets.newHashSet(DataPropertyAssertion(dp, ind, Literal(3)), DataPropertyAssertion(dp, ind, Literal(33.3)), DataPropertyAssertion(dp, ind, Literal(true)), DataPropertyAssertion(dp, ind, Literal(33.3f)), DataPropertyAssertion(dp, ind, Literal("33.3")))
            );
        //@formatter:on
    }

    protected static Set<OWLAxiom> swrl(String namespace, OWLDataFactory df2) {
        Set<OWLAxiom> axioms = new HashSet<>();
        SWRLVariable varX = df2.getSWRLVariable(namespace, "x");
        SWRLVariable varY = df2.getSWRLVariable(namespace, "y");
        SWRLVariable varZ = df2.getSWRLVariable(namespace, "z");
        Set<SWRLAtom> body = new HashSet<>();
        body.add(df2.getSWRLClassAtom(Class(iri("A")), varX));
        SWRLIndividualArgument indIArg = df2.getSWRLIndividualArgument(ind);
        SWRLIndividualArgument indJArg = df2.getSWRLIndividualArgument(indj);
        body.add(df2.getSWRLClassAtom(Class(iri("D")), indIArg));
        body.add(df2.getSWRLClassAtom(Class(iri("B")), varX));
        SWRLVariable varQ = df2.getSWRLVariable(namespace, "q");
        SWRLVariable varR = df2.getSWRLVariable(namespace, "r");
        body.add(df2.getSWRLDataPropertyAtom(dp, varX, varQ));
        OWLLiteral lit = Literal(33);
        SWRLLiteralArgument litArg = df2.getSWRLLiteralArgument(lit);
        body.add(df2.getSWRLDataPropertyAtom(dp, varY, litArg));
        Set<SWRLAtom> head = new HashSet<>();
        head.add(df2.getSWRLClassAtom(Class(iri("C")), varX));
        head.add(df2.getSWRLObjectPropertyAtom(op, varY, varZ));
        head.add(df2.getSWRLSameIndividualAtom(varX, varY));
        head.add(df2.getSWRLSameIndividualAtom(indIArg, indJArg));
        head.add(df2.getSWRLDifferentIndividualsAtom(varX, varZ));
        head.add(df2.getSWRLDifferentIndividualsAtom(varX, varZ));
        head.add(df2.getSWRLDifferentIndividualsAtom(indIArg, indJArg));
        OWLObjectSomeValuesFrom svf = ObjectSomeValuesFrom(op, Class(iri("A")));
        head.add(df2.getSWRLClassAtom(svf, varX));
        List<SWRLDArgument> args = new ArrayList<>();
        args.add(varQ);
        args.add(varR);
        args.add(litArg);
        head.add(df2.getSWRLBuiltInAtom(IRI(namespace, "myBuiltIn"), args));
        axioms.add(df2.getSWRLRule(body, head));
        return axioms;
    }
}
