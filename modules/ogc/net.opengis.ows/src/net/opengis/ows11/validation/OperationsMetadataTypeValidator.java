/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.ows11.validation;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * A sample validator interface for {@link net.opengis.ows11.OperationsMetadataType}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface OperationsMetadataTypeValidator {
    boolean validate();

    boolean validateOperation(EList value);
    boolean validateParameter(EList value);
    boolean validateConstraint(EList value);
    boolean validateExtendedCapabilities(EObject value);
}
